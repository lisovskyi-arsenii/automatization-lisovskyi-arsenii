package service;

import dto.BookingReceipt;
import dto.Seat;
import enums.SeatType;
import enums.Status;
import exception.PaymentFailedException;
import exception.SeatUnavailableException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.SeatRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Service Class Tests")
class BookingServiceTest {
    final String email = "test@test.com";

    @Mock
    SeatRepository seatRepository;

    @Mock
    PaymentGateway paymentGateway;

    @Mock
    EmailNotifier emailNotifier;

    @InjectMocks
    BookingService bookingService;

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should throw exception when email is empty")
        void shouldThrowExceptionWhenEmailIsEmpty(final String invalidEmail) {
            final List<Seat> seats = List.of(new Seat("A", 1, SeatType.REGULAR));

            assertThatThrownBy(() -> bookingService.book(invalidEmail, seats))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should throw exception when seat list is null or empty")
        void shouldThrowExceptionWhenSeatListIsEmpty(final List<Seat> invalidSeats) {
            assertThatThrownBy(() -> bookingService.book(email, invalidSeats))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Booking Process & Exceptions")
    class BookingProcess {
        final List<Seat> seats = List.of(
                Seat.regularSeat("A", 1),
                Seat.regularSeat("A", 2)
        );

        @Test
        @DisplayName("Should throw SeatUnavailableException when seats are taken")
        void shouldThrowExceptionWhenSeatsUnavailable() {
            when(seatRepository.areSeatsAvailable(seats))
                    .thenReturn(false);

            assertThatThrownBy(() -> bookingService.book(email, seats))
                    .isInstanceOf(SeatUnavailableException.class);

            verify(paymentGateway, never())
                    .charge(eq(email), any());

            verify(emailNotifier, never())
                    .sendTicketConfirmation(eq(email), any());
        }

        @Test
        @DisplayName("Should throw PaymentFailedException and notify user when payment fails")
        void shouldThrowExceptionAndAlertWhenPaymentFails() {
            when(seatRepository.areSeatsAvailable(seats))
                    .thenReturn(true);

            when(paymentGateway.charge(eq(email), any()))
                    .thenReturn(false);

            assertThatThrownBy(() -> bookingService.book(email, seats))
                    .isInstanceOf(PaymentFailedException.class);

            verify(emailNotifier, times(1))
                    .sendPaymentFailedAlert(email);
        }
    }

    @Nested
    @DisplayName("Successful Booking")
    class SuccessfulBooking {
        @Test
        @DisplayName("Successful booking")
        void shouldBookSeatsSuccessfully() {
            final List<Seat> seats = List.of(
                    Seat.regularSeat("A", 1),
                    Seat.regularSeat("B", 1)
            );

            mockSuccessfulDependencies(seats);

            BookingReceipt receipt = bookingService.book(email, seats);

            verify(emailNotifier, times(1))
                    .sendTicketConfirmation(email, receipt);
            verify(seatRepository, times(1))
                    .bookSeats(email, seats);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(receipt.bookingId()).isNotBlank();
                soft.assertThat(receipt.status()).isEqualTo(Status.CONFIRMED.toString());

                soft.assertThat(receipt.seats())
                        .isNotEmpty()
                        .hasSize(2)
                        .containsExactly(seats.get(0), seats.get(1));
            });
        }
    }

    @Nested
    @DisplayName("Discount Calculation (Mutation Testing Coverage)")
    class DiscountCalculation {
        @Test
        @DisplayName("Should charge base price without discount for 2 seats")
        void shouldNotApplyDiscountForTwoSeats() {
            final List<Seat> seats = List.of(
                    Seat.regularSeat("A", 1),
                    Seat.regularSeat("B", 1)
            );

            final BigDecimal expectedPrice = new BigDecimal("1000.00");

            mockSuccessfulDependencies(seats);

            BookingReceipt receipt = bookingService.book(email, seats);

            assertThat(receipt.totalAmount())
                    .isEqualByComparingTo(expectedPrice);

            verifyPaymentCharged(expectedPrice);
        }

        @Test
        @DisplayName("Should apply 10% discount for exactly 3 seats")
        void shouldApplyDiscountForExactlyThreeSeats() {
            final List<Seat> seats = List.of(
                    Seat.regularSeat("A", 1),
                    Seat.vipSeat("A", 2),
                    Seat.vipSeat("A", 3)
            );

            BigDecimal expectedPrice = new BigDecimal("2250.00");

            mockSuccessfulDependencies(seats);

            BookingReceipt receipt = bookingService.book(email, seats);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(receipt.totalAmount()).isEqualByComparingTo(expectedPrice);
                soft.assertThat(receipt.status()).isEqualTo(Status.CONFIRMED.toString());
                soft.assertThat(receipt.bookingId()).isNotBlank();
                soft.assertThat(receipt.seats()).hasSize(3).containsExactly(seats.get(0), seats.get(1), seats.get(2));
            });

            verifyPaymentCharged(expectedPrice);
        }

        @Test
        @DisplayName("Should apply 10% discount for more than 3 seats")
        void shouldApplyDiscountForFourSeats() {
            final List<Seat> seats = List.of(
                    Seat.regularSeat("A", 1),
                    Seat.vipSeat("A", 2),
                    Seat.vipSeat("A", 3),
                    Seat.vipSeat("A", 4)
            );

            BigDecimal expectedPrice = new BigDecimal("3150.00");

            mockSuccessfulDependencies(seats);

            BookingReceipt receipt = bookingService.book(email, seats);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(receipt.totalAmount())
                        .isEqualByComparingTo(expectedPrice);

                soft.assertThat(receipt.status())
                        .isEqualTo(Status.CONFIRMED.toString());

                soft.assertThat(receipt.seats())
                        .hasSize(4)
                        .containsAll(seats);
            });

            verifyPaymentCharged(expectedPrice);
        }
    }

    @Nested
    @DisplayName("PIT Mutation Testing: Vulnerable vs Fixed Tests")
    class MutationTestingCoverage {
        @Test
        @DisplayName("VULNERABLE TEST: Covers only 2 and 4 seats. Survives mutation from '>=' to '>'")
        void vulnerableTestThatLetsMutantSurvive() {
            final List<Seat> seats = List.of(
                    Seat.regularSeat("A", 1),
                    Seat.regularSeat("A", 2)
            );
            final BigDecimal expectedPrice = new BigDecimal("1000.00");

            mockSuccessfulDependencies(seats);

            BookingReceipt receipt = bookingService.book(email, seats);
            assertThat(receipt.totalAmount())
                    .isEqualByComparingTo(expectedPrice);

            final List<Seat> fourSeats = List.of(
                    Seat.regularSeat("A", 1), Seat.vipSeat("A", 2),
                    Seat.vipSeat("A", 3), Seat.vipSeat("A", 4)
            );
            final BigDecimal expectedForFour = new BigDecimal("3150.00");

            mockSuccessfulDependencies(fourSeats);

            BookingReceipt receiptForFour = bookingService.book(email, fourSeats);
            assertThat(receiptForFour.totalAmount())
                    .isEqualByComparingTo(expectedForFour);
        }

        @Test
        @DisplayName("FIXED TEST: Tests exactly boundary value (3 seats). Kills the mutant")
        void fixedTestThatKillsMutant() {
            final List<Seat> threeSeats = List.of(
                    Seat.regularSeat("A", 1),
                    Seat.vipSeat("A", 2),
                    Seat.vipSeat("A", 3)
            );

            BigDecimal expectedPrice = new BigDecimal("2250.00");

            mockSuccessfulDependencies(threeSeats);

            BookingReceipt receipt = bookingService.book(email, threeSeats);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(receipt.totalAmount()).isEqualByComparingTo(expectedPrice);
                soft.assertThat(receipt.status()).isEqualTo(Status.CONFIRMED.toString());
                soft.assertThat(receipt.seats()).hasSize(3);
            });

            verifyPaymentCharged(expectedPrice);
        }
    }

    private void mockSuccessfulDependencies(final List<Seat> seats) {
        when(seatRepository.areSeatsAvailable(seats))
                .thenReturn(true);

        when(paymentGateway.charge(eq(email), any()))
                .thenReturn(true);
    }

    private void verifyPaymentCharged(BigDecimal expectedPrice) {
        verify(paymentGateway, times(1))
                .charge(eq(email), argThat(amount -> amount.compareTo(expectedPrice) == 0));
    }
}

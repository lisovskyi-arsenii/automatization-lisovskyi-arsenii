package service;

import dto.BookingReceipt;
import dto.Seat;
import enums.Status;
import exception.PaymentFailedException;
import exception.SeatUnavailableException;
import lombok.Getter;
import repository.SeatRepository;
import util.Assert;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
public class BookingService {
    private final SeatRepository seatRepository;
    private final PaymentGateway paymentGateway;
    private final EmailNotifier emailNotifier;

    public BookingService(final SeatRepository seatRepository,
                          final PaymentGateway paymentGateway,
                          final EmailNotifier emailNotifier
    ) {
        this.seatRepository = seatRepository;
        this.paymentGateway = paymentGateway;
        this.emailNotifier = emailNotifier;
    }

    public BookingReceipt book(final String userEmail, final List<Seat> seats) throws IllegalArgumentException {
        Assert.hasText(userEmail);
        Assert.notEmpty(seats);

        if (!seatRepository.areSeatsAvailable(seats)) {
            throw new SeatUnavailableException("Seats are not available");
        }

        BigDecimal finalPrice = getPriceForAllWantedSeats(seats);

        if (!paymentGateway.charge(userEmail, finalPrice)) {
            emailNotifier.sendPaymentFailedAlert(userEmail);
            throw new PaymentFailedException("Payment failed");
        }

        seatRepository.bookSeats(userEmail, seats);

        String bookingId = UUID.randomUUID().toString();

        BookingReceipt receipt = new BookingReceipt(
                bookingId,
                seats,
                finalPrice,
                Status.CONFIRMED.toString()
        );

        emailNotifier.sendTicketConfirmation(userEmail, receipt);
        return receipt;
    }

    public BigDecimal getPriceForAllWantedSeats(final List<Seat> seats) throws IllegalArgumentException {
        Assert.notEmpty(seats);

        BigDecimal basePrice = seats.stream()
                .map(Seat::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return seats.size() >= 3
                ? basePrice.multiply(new BigDecimal("0.90"))
                : basePrice;
    }
}

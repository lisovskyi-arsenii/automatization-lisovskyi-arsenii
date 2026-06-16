import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.annotation.Testable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Testable
class BankAccountTest {
    public static final String CORE_TESTS_NAME = "core";
    public static final String PARAMETERIZED_TESTS_NAME = "parameterized";
    public static final String DYNAMIC_TESTS_NAME = "dynamic";
    public static final String ASSUMPTION_TESTS_NAME = "assumption";

    private static final double INITIAL_BALANCE = 100.0;
    private BankAccount bankAccount;

    @BeforeEach
    void setup() {
        this.bankAccount = new BankAccount(INITIAL_BALANCE);
    }

    @Test
    @Tag(CORE_TESTS_NAME)
    void testConstructor_shouldSetInitialBalance() {
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @Tag(CORE_TESTS_NAME)
    void testWithdraw_shouldAllowWithdrawingExactBalance() {
        bankAccount.withdraw(INITIAL_BALANCE);
        assertEquals(0, bankAccount.getBalance());
    }

    @Test
    @Tag(CORE_TESTS_NAME)
    void testDeposit_shouldAddAmountToBalance() {
        final double amountToAdd = 100.0;
        bankAccount.deposit(amountToAdd);
        assertEquals(amountToAdd + INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @Tag(CORE_TESTS_NAME)
    void testDeposit_shouldThrowIllegalArgumentException() {
        final double amountToAdd = -100.0;
        assertThrows(IllegalArgumentException.class, () -> bankAccount.deposit(amountToAdd));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 50.5, 9999.99})
    @Tag(PARAMETERIZED_TESTS_NAME)
    void testDeposit_shouldSuccessfullyAddVariousAmounts(final double validAmount) {
        bankAccount.deposit(validAmount);
        assertEquals(INITIAL_BALANCE + validAmount, bankAccount.getBalance());
    }

    @Test
    @Tag(CORE_TESTS_NAME)
    void testWithdraw_shouldMinusBalance() {
        final double amountToGet = 50.0;
        bankAccount.withdraw(amountToGet);
        assertEquals(INITIAL_BALANCE - amountToGet, bankAccount.getBalance());
    }

    @Test
    @Tag(CORE_TESTS_NAME)
    void testConvertCurrency_shouldReturnBalanceInAnotherBalance() {
        final double exchangeRate = 45.6;
        assertEquals(INITIAL_BALANCE * exchangeRate, bankAccount.convertCurrency(exchangeRate));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -10.0})
    @Tag(PARAMETERIZED_TESTS_NAME)
    void testConvertCurrency_shouldThrowIllegalArgumentException(final double invalidExchangeRate) {
        assertThrows(IllegalArgumentException.class, () -> bankAccount.convertCurrency(invalidExchangeRate));
    }

    @ParameterizedTest
    @CsvSource({
            "GROCERIES, 0.05",
            "PHARMACY, 0.1",
            "TAXI, 0.15"
    })
    @Tag(PARAMETERIZED_TESTS_NAME)
    void testGetCashbackRate_shouldReturnCashbackOfCertainCategory(
            final Category category,
            final double expectedRate
    ) {
        final double actualRate = bankAccount.getCashbackRate(category);
        assertEquals(expectedRate, actualRate);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -10.0, 1000.0})
    @Tag(PARAMETERIZED_TESTS_NAME)
    void testWithdraw_shouldThrowExceptionForInvalidAmounts(final double invalidAmount) {
        assertThrows(IllegalArgumentException.class, () -> bankAccount.withdraw(invalidAmount));
    }

    @TestFactory
    @Tag(DYNAMIC_TESTS_NAME)
    Stream<DynamicTest> dynamicTestsForCashbackCategories() {
        return BankAccount.categoriesCashbackMap.entrySet().stream()
                .map(entry -> {
                    final Category category = entry.getKey();
                    final double expectedRate = entry.getValue();

                    final String testName = "Should return " + expectedRate + " for " + category;

                    return dynamicTest(testName, () -> {
                        final double actualRate = bankAccount.getCashbackRate(category);
                        assertEquals(expectedRate, actualRate);
                    });
                });
    }

    @TestFactory
    @Tag(DYNAMIC_TESTS_NAME)
    Stream<DynamicTest> dynamicTestsForInvalidExchangeRates() {
        return Stream.of(0.0, -1.5, -100.0).map(invalidRate ->
            dynamicTest("Should throw IllegalArgumentException for exchange rate " + invalidRate, () ->
                assertThrows(IllegalArgumentException.class, () -> bankAccount.convertCurrency(invalidRate))
            )
        );
    }

    @Test
    @Tag(ASSUMPTION_TESTS_NAME)
    void testConvertCurrency_withAssumptions() {
        final DayOfWeek today = LocalDate.now().getDayOfWeek();
        assumeTrue(today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY, "Конвертація не відбувається на вихідних");

        final double exchangeRate = 40.2;
        final double balance = bankAccount.getBalance();

        assertEquals(balance * exchangeRate, bankAccount.convertCurrency(exchangeRate));
    }

    @Test
    @Tag(ASSUMPTION_TESTS_NAME)
    void testDeposit_withAssumptions_shouldRunOnlyOnSpecificSystemProperty() {
        final int javaVersion = Runtime.version().feature();
        assumeTrue(javaVersion >= 21, "Версія java повинна бути вища або дорівнювати 21");

        final double withdrawAmount = 90.5;
        bankAccount.withdraw(withdrawAmount);
        assertEquals(INITIAL_BALANCE - withdrawAmount, bankAccount.getBalance());
    }

    @Test
    @Tag(CORE_TESTS_NAME)
    void testGetCashbackRate_shouldThrowExceptionForUnknownCategory() {
        assertThrows(IllegalArgumentException.class, () -> bankAccount.getCashbackRate(Category.OTHER));
    }
}

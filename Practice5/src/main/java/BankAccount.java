import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

@Getter
public class BankAccount {
    public static final Map<Category, Double> categoriesCashbackMap;
    private double balance;

    static {
        categoriesCashbackMap = new EnumMap<>(Category.class);
        categoriesCashbackMap.put(Category.GROCERIES, 0.05);
        categoriesCashbackMap.put(Category.PHARMACY, 0.1);
        categoriesCashbackMap.put(Category.TAXI, 0.15);
    }

    public BankAccount(final double balance) {
        this.balance = balance;
    }

    public void deposit(final double amount) throws IllegalArgumentException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.balance += amount;
    }

    public void withdraw(final double amount) throws IllegalArgumentException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (balance - amount < 0) throw new IllegalArgumentException("Balance must be positive");
        this.balance -= amount;
    }

    public double convertCurrency(final double exchangeRate) {
        if (exchangeRate <= 0) throw new IllegalArgumentException("Exchange rate must be positive");
        return balance * exchangeRate;
    }

    public double getCashbackRate(final Category category) throws IllegalArgumentException {
        Double cashbackRate = categoriesCashbackMap.get(category);
        if (cashbackRate == null) {
            throw new IllegalArgumentException("No cashback rate for category " + category);
        }

        return cashbackRate;
    }
}

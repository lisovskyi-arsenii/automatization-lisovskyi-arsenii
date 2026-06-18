package service;

import java.math.BigDecimal;

public interface PaymentGateway {
    boolean charge(final String userEmail, final BigDecimal amount);
}

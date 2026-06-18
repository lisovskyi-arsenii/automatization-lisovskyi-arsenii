package service;

import dto.BookingReceipt;

public interface EmailNotifier {
    void sendTicketConfirmation(final String email, BookingReceipt receipt);
    void sendPaymentFailedAlert(final String email);
}

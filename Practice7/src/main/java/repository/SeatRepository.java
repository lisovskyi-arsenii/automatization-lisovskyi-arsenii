package repository;

import dto.Seat;

import java.util.List;

public interface SeatRepository {
    boolean areSeatsAvailable(final List<Seat> seats);
    void bookSeats(final String userEmail, final List<Seat> seats);
}

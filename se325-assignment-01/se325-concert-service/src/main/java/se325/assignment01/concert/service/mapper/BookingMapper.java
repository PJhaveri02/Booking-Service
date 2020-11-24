package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Seat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps Booking class to BookingDTO class and vice-versa
 */
public class BookingMapper {

    // Maps domain class to DTO class
    public static BookingDTO domainToDTO(Booking booking) {
        Set<Seat> seatSet = booking.getSeats();
        List<SeatDTO> seatDTOS = new ArrayList<>();
        for (Seat s : seatSet) {
            SeatDTO temp = SeatMapper.domainToDTO(s);
            seatDTOS.add(temp);
        }
        return new BookingDTO(booking.getId(), booking.getDate(), seatDTOS);
    }
}

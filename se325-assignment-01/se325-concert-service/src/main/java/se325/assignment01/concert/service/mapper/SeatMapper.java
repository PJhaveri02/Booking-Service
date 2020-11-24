package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Seat;

import java.time.LocalDateTime;

/**
 * Maps Seat class to SeatDTO class and vice-versa
 */
public class SeatMapper {
    // Maps domain class to DTO class
    public static SeatDTO domainToDTO(Seat seat) {
        return new SeatDTO(seat.getLabel(), seat.getPrice());
    }
}

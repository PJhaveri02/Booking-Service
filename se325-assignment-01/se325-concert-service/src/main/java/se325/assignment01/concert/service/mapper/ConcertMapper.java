package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Performer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Maps Concert class to ConcertDTO class and vice-versa
 */
public class ConcertMapper {

    // Maps domain class to DTO class
    public static ConcertDTO domainToDTO(Concert concert) {
        ConcertDTO tempDTO = new ConcertDTO(concert.getID(), concert.getTitle(), concert.getImage_name(), concert.getBlurb());

        // Add Concert Performers to ConcertDTO
        Set<Performer> performers = concert.getPerformers();
        List<PerformerDTO> performerDTOList = new ArrayList<>();
        for (Performer p : performers) {
            PerformerDTO temp = PerformerMapper.domainToDTO(p);
            performerDTOList.add(temp);
        }
        tempDTO.setPerformers(performerDTOList);

        // Add Concert Dates to ConcertDTO
        Set<LocalDateTime> dates = concert.getDates();
        List<LocalDateTime> dateTimeList = new ArrayList<>(dates);
        tempDTO.setDates(dateTimeList);

        return tempDTO;
    }
}

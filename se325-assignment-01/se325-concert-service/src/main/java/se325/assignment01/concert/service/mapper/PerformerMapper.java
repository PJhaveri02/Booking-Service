package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;

/**
 * Maps Performer class to PerformerDTO class and vice-versa
 */
public class PerformerMapper {

    // Maps domain class to DTO class
    public static PerformerDTO domainToDTO(Performer performer) {
        return new PerformerDTO(performer.getId(), performer.getName(), performer.getImage_name(), performer.getGenre(), performer.getBlurb());
    }
}

package tn.esprit.Entity.Guardian;

import java.time.LocalDateTime;

public record VirtualRoom(
        Integer id,
        String name,
        String description,
        Boolean isActive,
        Integer maxParticipants,
        LocalDateTime createdAt,
        Integer creatorId,
        Integer subjectId
) {
}

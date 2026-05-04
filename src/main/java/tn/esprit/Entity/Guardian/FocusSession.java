package tn.esprit.Entity.Guardian;

import java.time.LocalDateTime;

public record FocusSession(
        Integer id,
        Integer duration,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String sessionType,
        Integer userId,
        Integer taskId
) {
}

package tn.esprit.Entity.Guardian;

import java.time.LocalDateTime;

public record Resource(
        Integer id,
        String title,
        String description,
        String filePath,
        String type,
        Integer downloadCount,
        Integer rating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer subjectId,
        Integer uploaderId
) {
}

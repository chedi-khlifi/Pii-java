package tn.esprit.Entity.Guardian;

import java.time.LocalDateTime;

public record RoomMessage(
        Integer id,
        String content,
        Boolean isEdited,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        Integer senderId,
        Integer virtualRoomId
) {
}

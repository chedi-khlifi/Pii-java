package tn.esprit.services.guardian;

import tn.esprit.Entity.Guardian.FocusSession;
import tn.esprit.Entity.Guardian.Resource;
import tn.esprit.Entity.Guardian.VirtualRoom;

import java.time.LocalDateTime;
import java.util.Locale;

public final class GuardianInputValidator {

    private GuardianInputValidator() {
    }

    public static FocusSession toNewFocusSession(String durationRaw,
                                                 String sessionTypeRaw,
                                                 String userIdRaw,
                                                 String taskIdRaw) {
        int duration = parseRequiredInt(durationRaw, "Duration");
        validateRange(duration, 1, 480, "Duration");
        String sessionType = requireText(sessionTypeRaw, "Session type");
        validateLength(sessionType, 2, 50, "Session type");
        int userId = parseRequiredInt(userIdRaw, "User ID");
        validatePositive(userId, "User ID");
        Integer taskId = parseOptionalInt(taskIdRaw, "Task ID");
        if (taskId != null) {
            validatePositive(taskId, "Task ID");
        }

        return new FocusSession(null, duration, LocalDateTime.now(), null, sessionType, userId, taskId);
    }

    public static FocusSession toExistingFocusSession(String idRaw,
                                                      String durationRaw,
                                                      String sessionTypeRaw,
                                                      String userIdRaw,
                                                      String taskIdRaw,
                                                      FocusSession current) {
        int id = parseEntityId(idRaw, "Focus session ID");
        FocusSession draft = toNewFocusSession(durationRaw, sessionTypeRaw, userIdRaw, taskIdRaw);
        LocalDateTime startedAt = current == null || current.startedAt() == null ? LocalDateTime.now() : current.startedAt();
        return new FocusSession(id, draft.duration(), startedAt, current == null ? null : current.endedAt(), draft.sessionType(), draft.userId(), draft.taskId());
    }

    public static VirtualRoom toNewVirtualRoom(String nameRaw,
                                               String descriptionRaw,
                                               String maxParticipantsRaw,
                                               String creatorIdRaw,
                                               String subjectIdRaw) {
        String name = requireText(nameRaw, "Name");
        validateLength(name, 3, 80, "Name");
        String description = requireText(descriptionRaw, "Description");
        validateLength(description, 5, 500, "Description");
        int maxParticipants = parseRequiredInt(maxParticipantsRaw, "Max participants");
        validateRange(maxParticipants, 2, 500, "Max participants");
        int creatorId = parseRequiredInt(creatorIdRaw, "Creator ID");
        validatePositive(creatorId, "Creator ID");
        Integer subjectId = parseOptionalInt(subjectIdRaw, "Subject ID");
        if (subjectId != null) {
            validatePositive(subjectId, "Subject ID");
        }

        return new VirtualRoom(null, name, description, true, maxParticipants, LocalDateTime.now(), creatorId, subjectId);
    }

    public static VirtualRoom toExistingVirtualRoom(String idRaw,
                                                    String nameRaw,
                                                    String descriptionRaw,
                                                    String maxParticipantsRaw,
                                                    String creatorIdRaw,
                                                    String subjectIdRaw,
                                                    VirtualRoom current) {
        int id = parseEntityId(idRaw, "Virtual room ID");
        VirtualRoom draft = toNewVirtualRoom(nameRaw, descriptionRaw, maxParticipantsRaw, creatorIdRaw, subjectIdRaw);
        LocalDateTime createdAt = current == null || current.createdAt() == null ? LocalDateTime.now() : current.createdAt();
        boolean active = current == null || current.isActive() == null || current.isActive();
        return new VirtualRoom(id, draft.name(), draft.description(), active, draft.maxParticipants(), createdAt, draft.creatorId(), draft.subjectId());
    }

    public static Resource toNewResource(String titleRaw,
                                         String descriptionRaw,
                                         String filePathRaw,
                                         String typeRaw,
                                         String subjectIdRaw,
                                         String uploaderIdRaw) {
        String title = requireText(titleRaw, "Title");
        validateLength(title, 3, 120, "Title");
        String description = requireText(descriptionRaw, "Description");
        validateLength(description, 5, 500, "Description");
        String filePath = requireText(filePathRaw, "File path");
        validateLength(filePath, 3, 255, "File path");
        validateFilePath(filePath);
        String type = requireText(typeRaw, "Type").toUpperCase(Locale.ROOT);
        validateAllowedType(type);
        int subjectId = parseRequiredInt(subjectIdRaw, "Subject ID");
        validatePositive(subjectId, "Subject ID");
        int uploaderId = parseRequiredInt(uploaderIdRaw, "Uploader ID");
        validatePositive(uploaderId, "Uploader ID");

        LocalDateTime now = LocalDateTime.now();
        return new Resource(null, title, description, filePath, type, 0, 0, now, now, subjectId, uploaderId);
    }

    public static Resource toExistingResource(String idRaw,
                                              String titleRaw,
                                              String descriptionRaw,
                                              String filePathRaw,
                                              String typeRaw,
                                              String subjectIdRaw,
                                              String uploaderIdRaw,
                                              Resource current) {
        int id = parseEntityId(idRaw, "Resource ID");
        Resource draft = toNewResource(titleRaw, descriptionRaw, filePathRaw, typeRaw, subjectIdRaw, uploaderIdRaw);
        LocalDateTime createdAt = current == null || current.createdAt() == null ? LocalDateTime.now() : current.createdAt();
        int downloadCount = current == null || current.downloadCount() == null ? 0 : current.downloadCount();
        int rating = current == null || current.rating() == null ? 0 : current.rating();
        return new Resource(id, draft.title(), draft.description(), draft.filePath(), draft.type(), downloadCount, rating, createdAt, LocalDateTime.now(), draft.subjectId(), draft.uploaderId());
    }

    public static int parseEntityId(String idRaw, String fieldName) {
        int id = parseRequiredInt(idRaw, fieldName);
        validatePositive(id, fieldName);
        return id;
    }

    private static int parseRequiredInt(String rawValue, String fieldName) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private static Integer parseOptionalInt(String rawValue, String fieldName) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private static String requireText(String rawValue, String fieldName) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return trimmed;
    }

    private static void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0.");
        }
    }

    private static void validateRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max + ".");
        }
    }

    private static void validateLength(String value, int min, int max, String fieldName) {
        if (value.length() < min || value.length() > max) {
            throw new IllegalArgumentException(fieldName + " length must be between " + min + " and " + max + " characters.");
        }
    }

    private static void validateAllowedType(String type) {
        if (!("PDF".equals(type) || "VIDEO".equals(type) || "LINK".equals(type) || "DOC".equals(type) || "QUIZ".equals(type) || "SLIDES".equals(type) || "NOTE".equals(type))) {
            throw new IllegalArgumentException("Type must be one of: PDF, VIDEO, LINK, DOC, QUIZ, SLIDES, NOTE.");
        }
    }

    private static void validateFilePath(String filePath) {
        String lower = filePath.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".pdf")
                || lower.endsWith(".doc")
                || lower.endsWith(".docx")
                || lower.endsWith(".ppt")
                || lower.endsWith(".pptx")
                || lower.endsWith(".txt")
                || lower.startsWith("http://")
                || lower.startsWith("https://"))) {
            throw new IllegalArgumentException("File path must be a document path (.pdf/.doc/.docx/.ppt/.pptx/.txt) or a valid http(s) link.");
        }
    }
}

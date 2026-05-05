package tn.esprit.services.guardian;

public record ExternalSuggestion(
        String title,
        String author,
        Integer year,
        String url
) {
}

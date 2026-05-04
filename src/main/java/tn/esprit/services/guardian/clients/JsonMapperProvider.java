package tn.esprit.services.guardian.clients;

/**
 * Provides simple JSON mapping helpers without external dependencies.
 * Location: DTO mapping in all API client responses and requests.
 */
public class JsonMapperProvider {

    private JsonMapperProvider() {
    }

    public static String toJson(Object obj) {
        return obj == null ? "{}" : obj.toString();
    }

    public static <T> T toObject(String json, Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            return null;
        }
    }
}

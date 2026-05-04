package tn.esprit.services.guardian.clients;

/**
 * Factory for creating typed client placeholders without external dependencies.
 * Location: Used by Guardian integration clients as a simple marker/factory.
 */
public class RetrofitProvider {

    private RetrofitProvider() {
    }

    public static Object createClient(String baseUrl) {
        return new Object() {
            @Override
            public String toString() {
                return "Client[" + baseUrl + "]";
            }
        };
    }
}

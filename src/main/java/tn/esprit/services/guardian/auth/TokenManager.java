package tn.esprit.services.guardian.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Lightweight token manager without external JWT dependencies.
 * Location: Generates and verifies signed tokens for session and provider auth.
 * Token format: base64(userId|scope|expiresAt).base64(signature)
 */
public class TokenManager {

    private static final String SECRET = System.getenv("GUARDIAN_JWT_SECRET");
    private static final long DEFAULT_TTL_SECONDS = 3600;

    private TokenManager() {
    }

    public static String generateToken(String userId, String scope) {
        String payload = userId + "|" + scope + "|" + (Instant.now().getEpochSecond() + DEFAULT_TTL_SECONDS);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public static String verifyAndGetUserId(String token) throws Exception {
        if (token == null || !token.contains(".")) {
            throw new IllegalArgumentException("Invalid token format.");
        }

        String[] parts = token.split("\\.", 2);
        String encodedPayload = parts[0];
        String expectedSignature = sign(encodedPayload);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Invalid token signature.");
        }

        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        String[] fields = payload.split("\\|", 3);
        if (fields.length != 3) {
            throw new IllegalArgumentException("Invalid token payload.");
        }

        long expiresAt = Long.parseLong(fields[2]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new SecurityException("Token expired.");
        }

        return fields[0];
    }

    private static String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            String secret = SECRET != null ? SECRET : "guardian-dev-secret";
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token.", ex);
        }
    }
}

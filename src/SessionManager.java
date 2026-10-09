import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages server-side application sessions tied to phone-verified identities.
 * Uses cryptographically secure unpredictable random tokens.
 */
public class SessionManager {

    public static class Session {
        public final String token;
        public final int userId;
        public final String phone;
        public final String identityId;
        public final long expiresAt;

        public Session(String token, int userId, String phone, String identityId, long expiresAt) {
            this.token = token;
            this.userId = userId;
            this.phone = phone;
            this.identityId = identityId;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();
    // Default session validity: 4 hours
    private static final long SESSION_DURATION_MS = 4L * 60 * 60 * 1000;

    /**
     * Creates and stores a new application session after verified phone OTP.
     */
    public static Session createSession(int userId, String phone, String identityId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = HexFormat.of().formatHex(randomBytes);

        long expiresAt = System.currentTimeMillis() + SESSION_DURATION_MS;
        Session session = new Session(token, userId, phone, identityId, expiresAt);
        sessions.put(token, session);
        return session;
    }

    /**
     * Retrieves an active session by token.
     * Returns null if token is missing, invalid, or expired.
     */
    public static Session getSession(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        Session session = sessions.get(token.trim());
        if (session == null) return null;

        if (session.isExpired()) {
            sessions.remove(token.trim());
            return null;
        }
        return session;
    }

    /**
     * Invalidates a session on logout.
     */
    public static void invalidate(String token) {
        if (token != null) {
            sessions.remove(token.trim());
        }
    }
}


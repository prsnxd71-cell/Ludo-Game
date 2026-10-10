import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/**
 * Embedded HTTP server powered by Java's built-in HttpServer.
 * Serves static web frontend and provides REST endpoints for OTP, sessions, and movements.
 */
public class GameServer {

    public static void main(String[] args) {
        Config.load();
        int port = Config.getServerPort();

        System.out.println("=============================================");
        System.out.println("       Ludo Game Backend Server Starting     ");
        System.out.println("=============================================");

        // 1. Initialize required database tables safely
        try {
            Database.initializeTables();
            System.out.println("[DB] Connected to MySQL and verified schema.");
        } catch (SQLException e) {
            System.err.println("[DB FATAL] Database connection failed: " + e.getMessage());
            System.err.println("           Target host: " + Config.getDbHost() + ":" + Config.getDbPort() + ", database: " + Config.getDbName());
            System.err.println("           Please ensure MySQL in XAMPP is running on port " + Config.getDbPort());
            System.exit(1);
        }

        // 2. Recover stale active games on server startup (policy: > 15 mins inactive)
        try {
            int recovered = Database.recoverStaleActiveGames(15);
            if (recovered > 0) {
                System.out.println("[SERVER] Recovered " + recovered + " stale active games -> marked 'abandoned'.");
            }
        } catch (SQLException e) {
            System.err.println("[SERVER WARN] Stale game recovery error: " + e.getMessage());
        }

        // 3. Start HTTP server
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // API Endpoints
            server.createContext("/api/auth/otp/send", new SendOtpHandler());
            server.createContext("/api/auth/otp/verify", new VerifyOtpHandler());
            server.createContext("/api/auth/session", new SessionHandler());
            server.createContext("/api/auth/logout", new LogoutHandler());
            server.createContext("/api/games/init", new GameInitHandler());
            server.createContext("/api/games/state", new GameStateHandler());
            server.createContext("/api/games/leave", new GameLeaveHandler());
            server.createContext("/api/games/heartbeat", new GameHeartbeatHandler());
            server.createContext("/api/games/active", new ActiveGameHandler());
            server.createContext("/api/games/analysis", new GameAnalysisHandler());
            server.createContext("/api/moves", new MovesHandler());

            // Static File Handler (Frontend)
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(null); // default executor
            server.start();

            System.out.println("[SERVER] Ludo Game Server running at: http://localhost:" + port);
            System.out.println("[SERVER] Open http://localhost:" + port + " in your browser to play.");
            System.out.println("=============================================");

        } catch (IOException e) {
            System.err.println("[SERVER FATAL] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // HTTP HANDLERS
    // ==========================================

    /**
     * POST /api/auth/otp/send
     */
    static class SendOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            String body = readBody(ex);
            Map<String, Object> data = SimpleJson.parseObject(body);
            String phone = SimpleJson.getString(data, "phone", "");

            MiniMothClient.Result res = MiniMothClient.sendOtp(phone);
            if (res.success) {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("otp_id", res.otpId);
                resp.put("message", "OTP sent successfully.");
                sendJson(ex, 200, resp);
            } else {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", false);
                resp.put("code", res.errorCode);
                resp.put("error", res.errorMessage);
                sendJson(ex, res.statusCode, resp);
            }
        }
    }

    /**
     * POST /api/auth/otp/verify
     */
    static class VerifyOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            String body = readBody(ex);
            Map<String, Object> data = SimpleJson.parseObject(body);
            String rawPhone = SimpleJson.getString(data, "phone", "");
            String code = SimpleJson.getString(data, "code", "");

            MiniMothClient.Result res = MiniMothClient.verifyOtp(rawPhone, code);
            if (res.success) {
                String normalized = MiniMothClient.normalizePhone(rawPhone);
                try {
                    Database.User user = Database.findOrCreateUser(normalized, res.identityId);
                    SessionManager.Session session = SessionManager.createSession(user.id, user.phone, user.identityId);

                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", true);
                    resp.put("token", session.token);
                    resp.put("user", user.toMap());
                    sendJson(ex, 200, resp);

                } catch (SQLException e) {
                    sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
            } else {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", false);
                resp.put("code", res.errorCode);
                resp.put("error", res.errorMessage);
                sendJson(ex, res.statusCode, resp);
            }
        }
    }

    /**
     * GET /api/auth/session
     */
    static class SessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            SessionManager.Session session = authenticate(ex);
            if (session == null) {
                sendJson(ex, 401, Map.of("authenticated", false, "error", "Invalid or expired session."));
                return;
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("authenticated", true);
            resp.put("user", Map.of("id", session.userId, "phone", session.phone));
            sendJson(ex, 200, resp);
        }
    }

    /**
     * POST /api/auth/logout
     */
    static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            String token = extractBearerToken(ex);
            if (token != null) {
                SessionManager.invalidate(token);
            }
            sendJson(ex, 200, Map.of("success", true));
        }
    }

    /**
     * POST /api/games/init
     */
    static class GameInitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            SessionManager.Session session = authenticate(ex);
            if (session == null) {
                sendJson(ex, 401, Map.of("error", "Authentication required. Please verify phone OTP."));
                return;
            }

            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String body = readBody(ex);
                Map<String, Object> data = SimpleJson.parseObject(body);
                int playerCount = SimpleJson.getInt(data, "player_count", 2);
                String gameName = SimpleJson.getString(data, "game_name", "Ludo Match for " + session.phone);
                String initialState = SimpleJson.getString(data, "game_state", null);

                try {
                    int gameId = Database.createGame(gameName, playerCount, initialState);
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", true);
                    resp.put("game_id", gameId);
                    resp.put("player_count", playerCount);
                    sendJson(ex, 200, resp);
                } catch (SQLException e) {
                    sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
            } else {
                // GET returns latest active game ID
                try {
                    int latestId = Database.getLatestGameId();
                    sendJson(ex, 200, Map.of("success", true, "latest_game_id", latestId));
                } catch (SQLException e) {
                    sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
            }
        }
    }

    /**
     * POST /api/games/state and GET /api/games/state
     */
    static class GameStateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                SessionManager.Session session = authenticate(ex);
                if (session == null) {
                    sendJson(ex, 401, Map.of("error", "Authentication required."));
                    return;
                }

                String body = readBody(ex);
                Map<String, Object> data = SimpleJson.parseObject(body);
                int gameId = SimpleJson.getInt(data, "game_id", -1);
                String stateJson = SimpleJson.getString(data, "game_state", "");
                String status = SimpleJson.getString(data, "game_status", "active");

                if (gameId <= 0) {
                    sendJson(ex, 400, Map.of("error", "Invalid game_id."));
                    return;
                }

                try {
                    boolean updated = Database.updateGameState(gameId, stateJson, status);
                    sendJson(ex, 200, Map.of("success", updated));
                } catch (SQLException e) {
                    sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
                }

            } else if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                String query = ex.getRequestURI().getQuery();
                int gameId = parseQueryParamInt(query, "game_id", -1);
                if (gameId <= 0) {
                    sendJson(ex, 400, Map.of("error", "game_id parameter required."));
                    return;
                }

                try {
                    Map<String, Object> game = Database.getGame(gameId);
                    if (game == null) {
                        sendJson(ex, 404, Map.of("error", "Game not found."));
                        return;
                    }
                    List<Map<String, Object>> movements = Database.getRecentMovements(gameId, 100);
                    game.put("movements", movements);
                    sendJson(ex, 200, game);
                } catch (SQLException e) {
                    sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
            } else {
                sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
            }
        }
    }

    /**
     * GET /api/games/analysis?game_id=...
     * Returns full analysis report based on saved MySQL movements and game state.
     */
    static class GameAnalysisHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            SessionManager.Session session = authenticate(ex);
            if (session == null) {
                sendJson(ex, 401, Map.of("error", "Authentication required to view game analysis."));
                return;
            }

            String query = ex.getRequestURI().getQuery();
            int gameId = parseQueryParamInt(query, "game_id", -1);
            if (gameId <= 0) {
                sendJson(ex, 400, Map.of("error", "game_id parameter required."));
                return;
            }

            try {
                Map<String, Object> analysis = Database.getGameAnalysis(gameId);
                if (analysis == null) {
                    sendJson(ex, 404, Map.of("error", "Game not found."));
                    return;
                }
                sendJson(ex, 200, analysis);
            } catch (SQLException e) {
                sendJson(ex, 500, Map.of("error", "Database error generating analysis: " + e.getMessage()));
            }
        }
    }

    /**
     * POST /api/games/leave
     * Called when a player leaves or disconnects (including browser close).
     * Marks game as 'abandoned' without overwriting finished games.
     */
    static class GameLeaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            int gameId = -1;
            String body = readBody(ex);
            if (body != null && !body.isEmpty()) {
                Map<String, Object> data = SimpleJson.parseObject(body);
                gameId = SimpleJson.getInt(data, "game_id", -1);
            }
            if (gameId <= 0) {
                String query = ex.getRequestURI().getQuery();
                gameId = parseQueryParamInt(query, "game_id", -1);
            }

            if (gameId <= 0) {
                sendJson(ex, 400, Map.of("error", "Valid game_id is required."));
                return;
            }

            try {
                Map<String, Object> game = Database.getGame(gameId);
                if (game == null) {
                    sendJson(ex, 404, Map.of("error", "Game not found."));
                    return;
                }

                String currentStatus = (String) game.get("game_status");
                if ("finished".equalsIgnoreCase(currentStatus)) {
                    // Protected: Never overwrite finished games!
                    sendJson(ex, 200, Map.of("success", true, "game_id", gameId, "status", "finished",
                            "message", "Game is already finished; status preserved."));
                    return;
                }

                boolean abandoned = Database.leaveGame(gameId);
                sendJson(ex, 200, Map.of("success", abandoned, "game_id", gameId, "status", "abandoned"));

            } catch (SQLException e) {
                sendJson(ex, 500, Map.of("error", "Database error leaving game: " + e.getMessage()));
            }
        }
    }

    /**
     * POST /api/games/heartbeat or GET /api/games/heartbeat
     * Updates last_activity timestamp for active games to keep them fresh.
     */
    static class GameHeartbeatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            int gameId = -1;
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String body = readBody(ex);
                if (body != null && !body.isEmpty()) {
                    Map<String, Object> data = SimpleJson.parseObject(body);
                    gameId = SimpleJson.getInt(data, "game_id", -1);
                }
            }
            if (gameId <= 0) {
                String query = ex.getRequestURI().getQuery();
                gameId = parseQueryParamInt(query, "game_id", -1);
            }

            if (gameId <= 0) {
                sendJson(ex, 400, Map.of("error", "Valid game_id is required."));
                return;
            }

            try {
                boolean touched = Database.touchGameActivity(gameId);
                sendJson(ex, 200, Map.of("success", touched, "game_id", gameId));
            } catch (SQLException e) {
                sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
            }
        }
    }

    /**
     * GET /api/games/active
     * Checks if there is an ongoing recoverable active game.
     */
    static class ActiveGameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            try {
                Map<String, Object> activeGame = Database.getLatestActiveGame();
                if (activeGame != null && "active".equalsIgnoreCase((String) activeGame.get("game_status"))) {
                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("has_active_game", true);
                    resp.put("game", activeGame);
                    sendJson(ex, 200, resp);
                } else {
                    sendJson(ex, 200, Map.of("has_active_game", false));
                }
            } catch (SQLException e) {
                sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
            }
        }
    }

    /**
     * POST /api/moves (Record movement) and GET /api/moves (List movements)
     */
    static class MovesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                // 1. Authenticate user
                SessionManager.Session session = authenticate(ex);
                if (session == null) {
                    sendJson(ex, 401, Map.of("error", "Authentication required. Please login with phone OTP."));
                    return;
                }

                String body = readBody(ex);
                Map<String, Object> data = SimpleJson.parseObject(body);

                int gameId = SimpleJson.getInt(data, "game_id", -1);
                String playerColor = SimpleJson.getString(data, "player_color", "");
                String pieceId = SimpleJson.getString(data, "piece_id", "");
                int fromPos = SimpleJson.getInt(data, "from_position", -1);
                int toPos = SimpleJson.getInt(data, "to_position", -1);
                int diceRoll = SimpleJson.getInt(data, "dice_roll", -1);
                String eventType = SimpleJson.getString(data, "event_type", "MOVE");
                int moveNumber = SimpleJson.getInt(data, "move_number", -1);
                String clientMoveId = SimpleJson.getString(data, "client_move_id", "");

                // 2. Validate move rules and continuity
                MoveValidator.Result validRes = MoveValidator.validate(data);
                if (!validRes.valid) {
                    sendJson(ex, 400, Map.of("error", "Invalid move rejected: " + validRes.reason));
                    return;
                }

                // 3. Record move in database inside transaction (with duplicate prevention)
                try {
                    Database.MovementRecordResult res = Database.recordMovement(
                            gameId,
                            session.userId,
                            playerColor,
                            pieceId,
                            fromPos,
                            toPos,
                            diceRoll,
                            eventType,
                            moveNumber,
                            clientMoveId
                    );

                    Map<String, Object> resp = new LinkedHashMap<>();
                    resp.put("success", true);
                    resp.put("movement_id", res.movementId);
                    resp.put("status", res.status);
                    resp.put("is_duplicate", res.isDuplicate);
                    sendJson(ex, 200, resp);

                } catch (SQLException e) {
                    sendJson(ex, 500, Map.of("error", "Failed to save movement: " + e.getMessage()));
                }

            } else if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                String query = ex.getRequestURI().getQuery();
                int gameId = parseQueryParamInt(query, "game_id", -1);
                if (gameId <= 0) {
                    sendJson(ex, 400, Map.of("error", "game_id parameter required."));
                    return;
                }

                try {
                    List<Map<String, Object>> movements = Database.getRecentMovements(gameId, 200);
                    sendJson(ex, 200, Map.of("success", true, "game_id", gameId, "movements", movements));
                } catch (SQLException e) {
                    sendJson(ex, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
            } else {
                sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
            }
        }
    }

    /**
     * Serves static frontend files (HTML, CSS, JS)
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            addCorsHeaders(ex);
            if (handleOptions(ex)) return;

            String path = ex.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Remove leading slash
            String relativePath = path.startsWith("/") ? path.substring(1) : path;
            File file = new File(relativePath);
            // Fallback for requests asking for images/xxx-piece.png when pieces are at root
            if (!file.exists() && relativePath.startsWith("images/")) {
                file = new File(relativePath.substring("images/".length()));
            }

            if (!file.exists() || file.isDirectory()) {
                String msg = "404 Not Found: " + path;
                ex.sendResponseHeaders(404, msg.length());
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(msg.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String mime = getMimeType(file.getName());
            ex.getResponseHeaders().set("Content-Type", mime);
            ex.sendResponseHeaders(200, file.length());

            try (FileInputStream fis = new FileInputStream(file); OutputStream os = ex.getResponseBody()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            }
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private static SessionManager.Session authenticate(HttpExchange ex) {
        String token = extractBearerToken(ex);
        if (token == null) return null;
        return SessionManager.getSession(token);
    }

    private static String extractBearerToken(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        // Support ?token= query param (used by navigator.sendBeacon during browser close)
        String query = ex.getRequestURI().getQuery();
        if (query != null && query.contains("token=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6).trim();
                }
            }
        }
        return null;
    }

    private static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
    }

    private static boolean handleOptions(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange ex, int statusCode, Object data) throws IOException {
        String jsonStr = SimpleJson.toJson(data);
        byte[] bytes = jsonStr.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static int parseQueryParamInt(String query, String param, int defaultVal) {
        if (query == null) return defaultVal;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(param)) {
                try {
                    return Integer.parseInt(URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                } catch (Exception e) {
                    return defaultVal;
                }
            }
        }
        return defaultVal;
    }

    private static String getMimeType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }
}


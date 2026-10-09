import java.sql.*;
import java.util.*;

/**
 * Reusable JDBC Database Connection and Data Access Layer.
 * Interacts with 'ludo_db' safely using prepared statements and transactions.
 * Preserves existing tables and records; prevents cascading deletes on movements.
 */
public class Database {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB ERROR] MySQL Driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        String host = Config.getDbHost();
        String port = Config.getDbPort();
        String dbName = Config.getDbName();
        String user = Config.getDbUser();
        String pass = Config.getDbPassword();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Creates only the necessary tables if they do not already exist.
     * Never drops or modifies existing tables or records.
     */
    public static void initializeTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 1. Table for authenticated application users
            String createUsersSql = "CREATE TABLE IF NOT EXISTS `users` ("
                    + "`id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + "`phone` VARCHAR(20) NOT NULL UNIQUE,"
                    + "`identity_id` VARCHAR(100) DEFAULT NULL,"
                    + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "`last_login` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createUsersSql);
            System.out.println("[DB] Verified table 'users' exists.");

            // 2. Table for movement history (preserves history with ON DELETE RESTRICT)
            String createMovementsSql = "CREATE TABLE IF NOT EXISTS `movements` ("
                    + "`id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + "`game_id` INT NOT NULL,"
                    + "`user_id` INT DEFAULT NULL,"
                    + "`player_color` VARCHAR(10) NOT NULL,"
                    + "`piece_id` VARCHAR(20) NOT NULL,"
                    + "`from_position` INT NOT NULL,"
                    + "`to_position` INT NOT NULL,"
                    + "`dice_roll` INT NOT NULL,"
                    + "`event_type` VARCHAR(30) NOT NULL DEFAULT 'MOVE',"
                    + "`move_number` INT NOT NULL,"
                    + "`client_move_id` VARCHAR(64) NOT NULL,"
                    + "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "INDEX `idx_game_id` (`game_id`),"
                    + "INDEX `idx_user_id` (`user_id`),"
                    + "UNIQUE KEY `idx_game_client_move` (`game_id`, `client_move_id`),"
                    + "CONSTRAINT `fk_movements_game` FOREIGN KEY (`game_id`) REFERENCES `games` (`id`) ON DELETE RESTRICT"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            stmt.execute(createMovementsSql);
            System.out.println("[DB] Verified table 'movements' exists.");

            // 3. Ensure 'last_activity' timestamp column exists on 'games' table
            ensureLastActivityColumn(conn);
        }
    }

    /**
     * Non-destructively inspects 'games' table and adds 'last_activity' column if absent.
     * Backfills existing rows with created_at to preserve historical timing.
     */
    public static void ensureLastActivityColumn(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        boolean exists = false;
        try (ResultSet rs = meta.getColumns(null, null, "games", "last_activity")) {
            if (rs.next()) {
                exists = true;
            }
        }

        if (!exists) {
            System.out.println("[DB] Non-destructively adding 'last_activity' column to 'games' table...");
            String alterSql = "ALTER TABLE `games` ADD COLUMN `last_activity` TIMESTAMP "
                    + "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;";
            try (Statement s = conn.createStatement()) {
                s.execute(alterSql);
            }
            String backfillSql = "UPDATE `games` SET `last_activity` = `created_at` WHERE `last_activity` IS NULL;";
            try (Statement s = conn.createStatement()) {
                s.execute(backfillSql);
            }
            System.out.println("[DB] Added and backfilled 'last_activity' column in 'games'.");
        } else {
            System.out.println("[DB] Verified column 'last_activity' exists in 'games'.");
        }
    }

    /**
     * Stale Game Policy:
     * Games with status 'active' whose last_activity is older than thresholdMinutes
     * (default: 15 minutes) are marked 'abandoned' because the player disconnected
     * or closed their browser without sending a leave beacon.
     *
     * Games active within thresholdMinutes are NOT marked abandoned, protecting
     * ongoing games from being disrupted by a server restart.
     */
    public static int recoverStaleActiveGames(int thresholdMinutes) throws SQLException {
        if (thresholdMinutes <= 0) thresholdMinutes = 15;

        String selectSql = "SELECT id, game_name, last_activity, "
                + "TIMESTAMPDIFF(MINUTE, last_activity, NOW()) AS inactive_minutes "
                + "FROM games WHERE game_status = 'active'";

        String updateSql = "UPDATE games SET game_status = 'abandoned' "
                + "WHERE id = ? AND game_status = 'active'";

        int count = 0;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("game_name");
                long inactiveMins = rs.getLong("inactive_minutes");

                if (inactiveMins >= thresholdMinutes) {
                    updateStmt.setInt(1, id);
                    int rows = updateStmt.executeUpdate();
                    if (rows > 0) {
                        count++;
                        System.out.println("[STALE RECOVERY] Game #" + id + " ('" + name + "') was inactive for "
                                + inactiveMins + " mins (threshold: " + thresholdMinutes + "m) -> marked 'abandoned'.");
                    }
                } else {
                    System.out.println("[STALE RECOVERY] Game #" + id + " ('" + name + "') was active "
                            + inactiveMins + " mins ago -> preserved as 'active'.");
                }
            }
        }
        return count;
    }

    public static class User {
        public final int id;
        public final String phone;
        public final String identityId;

        public User(int id, String phone, String identityId) {
            this.id = id;
            this.phone = phone;
            this.identityId = identityId;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("phone", phone);
            m.put("identity_id", identityId);
            return m;
        }
    }

    /**
     * Finds a user by phone or inserts a new user record.
     */
    public static User findOrCreateUser(String phone, String identityId) throws SQLException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        phone = phone.trim();

        try (Connection conn = getConnection()) {
            // Check if user already exists
            String selectSql = "SELECT id, phone, identity_id FROM users WHERE phone = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, phone);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("id");
                        String existingId = rs.getString("identity_id");
                        if (identityId != null && (existingId == null || existingId.isEmpty())) {
                            // Update identity_id if newly provided
                            String updateSql = "UPDATE users SET identity_id = ? WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, identityId);
                                updateStmt.setInt(2, id);
                                updateStmt.executeUpdate();
                            }
                        }
                        return new User(id, phone, identityId != null ? identityId : existingId);
                    }
                }
            }

            // Insert new user
            String insertSql = "INSERT INTO users (phone, identity_id) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, phone);
                insertStmt.setString(2, identityId);
                insertStmt.executeUpdate();
                try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        return new User(id, phone, identityId);
                    }
                }
            }
        }
        throw new SQLException("Failed to create or retrieve user for phone: " + phone);
    }

    public static User getUserById(int userId) throws SQLException {
        String sql = "SELECT id, phone, identity_id FROM users WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("phone"), rs.getString("identity_id"));
                }
            }
        }
        return null;
    }

    public static class MovementRecordResult {
        public final int movementId;
        public final boolean isDuplicate;
        public final String status;

        public MovementRecordResult(int movementId, boolean isDuplicate, String status) {
            this.movementId = movementId;
            this.isDuplicate = isDuplicate;
            this.status = status;
        }
    }

    /**
     * Safely records an accepted piece movement into the movements table using transactions.
     * Prevents duplicate inserts for retried requests using client_move_id.
     */
    public static MovementRecordResult recordMovement(
            int gameId,
            Integer userId,
            String playerColor,
            String pieceId,
            int fromPosition,
            int toPosition,
            int diceRoll,
            String eventType,
            int moveNumber,
            String clientMoveId
    ) throws SQLException {

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Begin transaction

            try {
                // 1. Idempotency check: see if client_move_id was already recorded for this game
                String checkSql = "SELECT id FROM movements WHERE game_id = ? AND client_move_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, gameId);
                    checkStmt.setString(2, clientMoveId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            int existingId = rs.getInt("id");
                            conn.rollback();
                            return new MovementRecordResult(existingId, true, "already_recorded");
                        }
                    }
                }

                // 2. Verify that game_id exists
                String verifyGameSql = "SELECT id FROM games WHERE id = ?";
                try (PreparedStatement verifyStmt = conn.prepareStatement(verifyGameSql)) {
                    verifyStmt.setInt(1, gameId);
                    try (ResultSet rs = verifyStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            throw new SQLException("Game with ID " + gameId + " does not exist.");
                        }
                    }
                }

                // 3. Insert new movement record
                String insertSql = "INSERT INTO movements "
                        + "(game_id, user_id, player_color, piece_id, from_position, to_position, dice_roll, event_type, move_number, client_move_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                int newId = -1;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setInt(1, gameId);
                    if (userId != null && userId > 0) {
                        insertStmt.setInt(2, userId);
                    } else {
                        insertStmt.setNull(2, Types.INTEGER);
                    }
                    insertStmt.setString(3, playerColor);
                    insertStmt.setString(4, pieceId);
                    insertStmt.setInt(5, fromPosition);
                    insertStmt.setInt(6, toPosition);
                    insertStmt.setInt(7, diceRoll);
                    insertStmt.setString(8, eventType != null ? eventType : "MOVE");
                    insertStmt.setInt(9, moveNumber);
                    insertStmt.setString(10, clientMoveId);

                    insertStmt.executeUpdate();

                    try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            newId = keys.getInt(1);
                        }
                    }
                }

                // Touch last_activity on games table for this active game
                String touchSql = "UPDATE games SET last_activity = CURRENT_TIMESTAMP WHERE id = ? AND game_status = 'active'";
                try (PreparedStatement touchStmt = conn.prepareStatement(touchSql)) {
                    touchStmt.setInt(1, gameId);
                    touchStmt.executeUpdate();
                }

                conn.commit(); // Transaction committed safely
                return new MovementRecordResult(newId, false, "recorded");

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Gets the last move number recorded for a game (returns 0 if no moves yet).
     */
    public static int getLastMoveNumber(int gameId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(move_number), 0) FROM movements WHERE game_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Gets the last known position of a piece in a game (returns -1 if no prior movement).
     */
    public static int getPieceLastPosition(int gameId, String pieceId) throws SQLException {
        String sql = "SELECT to_position FROM movements WHERE game_id = ? AND piece_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            stmt.setString(2, pieceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("to_position");
                }
            }
        }
        return -1;
    }

    /**
     * Retrieves recent movements for a game.
     */
    public static List<Map<String, Object>> getRecentMovements(int gameId, int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, game_id, user_id, player_color, piece_id, from_position, to_position, dice_roll, event_type, move_number, client_move_id, created_at "
                + "FROM movements WHERE game_id = ? ORDER BY id ASC LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            stmt.setInt(2, limit > 0 ? limit : 200);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("game_id", rs.getInt("game_id"));
                    int uid = rs.getInt("user_id");
                    m.put("user_id", rs.wasNull() ? null : uid);
                    m.put("player_color", rs.getString("player_color"));
                    m.put("piece_id", rs.getString("piece_id"));
                    m.put("from_position", rs.getInt("from_position"));
                    m.put("to_position", rs.getInt("to_position"));
                    m.put("dice_roll", rs.getInt("dice_roll"));
                    m.put("event_type", rs.getString("event_type"));
                    m.put("move_number", rs.getInt("move_number"));
                    m.put("client_move_id", rs.getString("client_move_id"));
                    m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                    list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * Gets a game record by ID from the existing 'games' table.
     */
    public static Map<String, Object> getGame(int gameId) throws SQLException {
        String sql = "SELECT id, game_name, player_count, game_status, created_at, last_activity, game_state FROM games WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("game_name", rs.getString("game_name"));
                    m.put("player_count", rs.getInt("player_count"));
                    m.put("game_status", rs.getString("game_status"));
                    m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                    m.put("last_activity", String.valueOf(rs.getTimestamp("last_activity")));
                    m.put("game_state", rs.getString("game_state"));
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Creates a new game in the existing 'games' table.
     */
    public static int createGame(String gameName, int playerCount, String initialGameState) throws SQLException {
        String sql = "INSERT INTO games (game_name, player_count, game_status, game_state, last_activity) VALUES (?, ?, 'active', ?, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, (gameName != null && !gameName.isEmpty()) ? gameName : "Ludo Match");
            stmt.setInt(2, playerCount > 0 ? playerCount : 2);
            stmt.setString(3, initialGameState);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Updates the game_state and game_status in the existing 'games' table.
     * Enforces: Never overwrites a 'finished' game status with any other status!
     */
    public static boolean updateGameState(int gameId, String gameStateJson, String status) throws SQLException {
        String targetStatus = (status != null && !status.isEmpty()) ? status : "active";
        String sql = "UPDATE games SET game_state = ?, "
                + "game_status = CASE WHEN game_status = 'finished' THEN 'finished' ELSE ? END, "
                + "last_activity = CURRENT_TIMESTAMP "
                + "WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, gameStateJson);
            stmt.setString(2, targetStatus);
            stmt.setInt(3, gameId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Marks an ongoing game as 'abandoned' when a player leaves or disconnects.
     * Protected: Never overwrites a game that has already reached 'finished' status.
     */
    public static boolean leaveGame(int gameId) throws SQLException {
        String sql = "UPDATE games SET game_status = 'abandoned', last_activity = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND game_status = 'active'";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates last_activity timestamp for an active game (heartbeat).
     */
    public static boolean touchGameActivity(int gameId) throws SQLException {
        String sql = "UPDATE games SET last_activity = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND game_status = 'active'";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves the latest ongoing active game, if any.
     */
    public static Map<String, Object> getLatestActiveGame() throws SQLException {
        String sql = "SELECT id, game_name, player_count, game_status, created_at, last_activity, game_state "
                + "FROM games WHERE game_status = 'active' ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("game_name", rs.getString("game_name"));
                m.put("player_count", rs.getInt("player_count"));
                m.put("game_status", rs.getString("game_status"));
                m.put("created_at", String.valueOf(rs.getTimestamp("created_at")));
                m.put("last_activity", String.valueOf(rs.getTimestamp("last_activity")));
                m.put("game_state", rs.getString("game_state"));
                return m;
            }
        }
        return null;
    }

    /**
     * Retrieves the latest game ID from 'games' table.
     */
    public static int getLatestGameId() throws SQLException {
        String sql = "SELECT id FROM games ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Generates a comprehensive game analysis report from MySQL records.
     */
    public static Map<String, Object> getGameAnalysis(int gameId) throws SQLException {
        Map<String, Object> game = getGame(gameId);
        if (game == null) return null;

        List<Map<String, Object>> movements = getRecentMovements(gameId, 1000);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("success", true);
        report.put("game_id", gameId);
        report.put("game_name", game.get("game_name"));
        report.put("player_count", game.get("player_count"));
        report.put("game_status", game.get("game_status"));
        report.put("created_at", game.get("created_at"));

        int totalMoves = movements.size();
        report.put("total_moves", totalMoves);

        Map<String, Integer> movesPerPlayer = new LinkedHashMap<>();
        Map<String, Integer> perksPerPlayer = new LinkedHashMap<>();
        Map<String, Integer> capturesPerPlayer = new LinkedHashMap<>();
        Map<String, Integer> perkBreakdown = new LinkedHashMap<>();

        int totalCaptures = 0;
        int totalPerks = 0;

        Timestamp firstTimestamp = null;
        Timestamp lastTimestamp = null;

        for (Map<String, Object> m : movements) {
            String color = (String) m.get("player_color");
            String event = (String) m.get("event_type");
            String timeStr = (String) m.get("created_at");

            if (color != null) {
                movesPerPlayer.put(color, movesPerPlayer.getOrDefault(color, 0) + 1);
            }

            if ("CAPTURE".equalsIgnoreCase(event)) {
                totalCaptures++;
                if (color != null) {
                    capturesPerPlayer.put(color, capturesPerPlayer.getOrDefault(color, 0) + 1);
                }
            } else if (event != null && event.startsWith("PERK_")) {
                totalPerks++;
                perkBreakdown.put(event, perkBreakdown.getOrDefault(event, 0) + 1);
                if (color != null) {
                    perksPerPlayer.put(color, perksPerPlayer.getOrDefault(color, 0) + 1);
                }
            }

            try {
                if (timeStr != null) {
                    Timestamp ts = Timestamp.valueOf(timeStr);
                    if (firstTimestamp == null || ts.before(firstTimestamp)) firstTimestamp = ts;
                    if (lastTimestamp == null || ts.after(lastTimestamp)) lastTimestamp = ts;
                }
            } catch (Exception ignored) {}
        }

        long durationSeconds = 0;
        if (firstTimestamp != null && lastTimestamp != null) {
            durationSeconds = Math.max(0, (lastTimestamp.getTime() - firstTimestamp.getTime()) / 1000);
        }

        report.put("duration_seconds", durationSeconds);
        report.put("moves_per_player", movesPerPlayer);
        report.put("captures_count", totalCaptures);
        report.put("captures_per_player", capturesPerPlayer);
        report.put("perks_count", totalPerks);
        report.put("perk_breakdown", perkBreakdown);
        report.put("perks_per_player", perksPerPlayer);

        // Parse rankings and player names from game_state if available
        String gameStateStr = (String) game.get("game_state");
        if (gameStateStr != null && !gameStateStr.isEmpty()) {
            Map<String, Object> stateMap = SimpleJson.parseObject(gameStateStr);
            if (stateMap.containsKey("rankings")) {
                report.put("rankings", stateMap.get("rankings"));
            }
            if (stateMap.containsKey("loser")) {
                report.put("loser", stateMap.get("loser"));
            }
            if (stateMap.containsKey("players")) {
                report.put("players", stateMap.get("players"));
            }
        }

        report.put("movements", movements);
        return report;
    }
}


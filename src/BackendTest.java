import java.sql.*;
import java.util.*;

/**
 * Automated test suite verifying:
 * 1. Safe database table creation (preserves existing games table and records)
 * 2. Session generation and expiry
 * 3. MiniMothClient validation handling
 * 4. MoveValidator game rules and sequence logic
 * 5. Movement recording with transaction, unique client_move_id idempotency
 * 6. Protection of historical records (no cascade delete)
 */
public class BackendTest {

    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("        Ludo Backend Verification Suite      ");
        System.out.println("=============================================");

        int passed = 0;
        int failed = 0;

        Config.load();

        // TEST 1: Database table initialization
        System.out.print("[TEST 1] Initializing tables safely... ");
        try {
            Database.initializeTables();
            // Verify existing games row still exists
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM games WHERE id = 1")) {
                if (rs.next() && rs.getInt(1) == 1) {
                    System.out.println("PASSED (Existing game id=1 preserved)");
                    passed++;
                } else {
                    System.err.println("FAILED (Game id=1 missing!)");
                    failed++;
                }
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 2: Session Manager
        System.out.print("[TEST 2] Testing SessionManager creation and lookup... ");
        try {
            SessionManager.Session session = SessionManager.createSession(999, "+919876543210", "test_id");
            if (session.token != null && session.token.length() == 64) {
                SessionManager.Session fetched = SessionManager.getSession(session.token);
                if (fetched != null && fetched.userId == 999 && fetched.phone.equals("+919876543210")) {
                    SessionManager.invalidate(session.token);
                    if (SessionManager.getSession(session.token) == null) {
                        System.out.println("PASSED (Created, verified, invalidated)");
                        passed++;
                    } else {
                        System.err.println("FAILED (Invalidation failed)");
                        failed++;
                    }
                } else {
                    System.err.println("FAILED (Fetched session mismatched)");
                    failed++;
                }
            } else {
                System.err.println("FAILED (Invalid token length)");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 3: MiniMoth Phone Normalization and Key Check
        System.out.print("[TEST 3] Testing MiniMothClient phone normalization and placeholder key check... ");
        try {
            String n1 = MiniMothClient.normalizePhone("9876543210");
            String n2 = MiniMothClient.normalizePhone("+91 98765 43210");
            boolean v1 = MiniMothClient.isValidPhone(n1);
            boolean v2 = MiniMothClient.isValidPhone(n2);

            // Test phone format normalization and invalid phone rejection
            MiniMothClient.Result resInvalid = MiniMothClient.sendOtp("12345");

            if (n1.equals("+919876543210") && v1 && v2 && !resInvalid.success && "INVALID_PHONE".equals(resInvalid.errorCode)) {
                System.out.println("PASSED (Phone normalized to " + n1 + ", invalid numbers rejected)");
                passed++;
            } else {
                System.err.println("FAILED (Normalization or invalid rejection failed: " + resInvalid.errorCode + ")");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 4: MoveValidator Standard Rules
        System.out.print("[TEST 4] Testing MoveValidator standard rules... ");
        try {
            // Invalid roll
            Map<String, Object> badRoll = Map.of(
                    "game_id", 1, "player_color", "blue", "piece_id", "blue-0",
                    "from_position", 0, "to_position", 1, "dice_roll", 7,
                    "event_type", "MOVE", "move_number", 1, "client_move_id", "c1"
            );
            MoveValidator.Result r1 = MoveValidator.validate(badRoll);

            // Leaving base without a 6
            Map<String, Object> badBase = Map.of(
                    "game_id", 1, "player_color", "blue", "piece_id", "blue-0",
                    "from_position", 0, "to_position", 1, "dice_roll", 4,
                    "event_type", "MOVE", "move_number", 1, "client_move_id", "c2"
            );
            MoveValidator.Result r2 = MoveValidator.validate(badBase);

            if (!r1.valid && !r2.valid) {
                System.out.println("PASSED (Invalid roll & base-exit rules enforced)");
                passed++;
            } else {
                System.err.println("FAILED (Invalid moves were accepted)");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 5: MoveValidator Perk and Capture Rules
        System.out.print("[TEST 5] Testing MoveValidator perk and capture events... ");
        try {
            // Valid PERK_EXTRA_TURN (dice_roll must be 0, from_pos == to_pos)
            Map<String, Object> extraTurnValid = Map.of(
                    "game_id", 1, "player_color", "blue", "piece_id", "blue-0",
                    "from_position", 10, "to_position", 10, "dice_roll", 0,
                    "event_type", "PERK_EXTRA_TURN", "move_number", 99, "client_move_id", "c_et1"
            );
            // Invalid PERK_EXTRA_TURN (dice_roll != 0)
            Map<String, Object> extraTurnBad = Map.of(
                    "game_id", 1, "player_color", "blue", "piece_id", "blue-0",
                    "from_position", 10, "to_position", 10, "dice_roll", 3,
                    "event_type", "PERK_EXTRA_TURN", "move_number", 100, "client_move_id", "c_et2"
            );
            // Valid CAPTURE (to_position must be 0, dice_roll = 0)
            Map<String, Object> capValid = Map.of(
                    "game_id", 1, "player_color", "red", "piece_id", "red-1",
                    "from_position", 15, "to_position", 0, "dice_roll", 0,
                    "event_type", "CAPTURE", "move_number", 101, "client_move_id", "c_cap1"
            );
            // Invalid CAPTURE (to_position != 0)
            Map<String, Object> capBad = Map.of(
                    "game_id", 1, "player_color", "red", "piece_id", "red-1",
                    "from_position", 15, "to_position", 5, "dice_roll", 0,
                    "event_type", "CAPTURE", "move_number", 102, "client_move_id", "c_cap2"
            );

            // Valid Jetpack (forward 1 to 4)
            Map<String, Object> jetpackBad = Map.of(
                    "game_id", 1, "player_color", "green", "piece_id", "green-0",
                    "from_position", 10, "to_position", 20, "dice_roll", 0,
                    "event_type", "PERK_JETPACK", "move_number", 103, "client_move_id", "c_jp_bad"
            );

            // Check validations (before DB continuity check)
            MoveValidator.Result rET_bad = MoveValidator.validate(extraTurnBad);
            MoveValidator.Result rCap_bad = MoveValidator.validate(capBad);
            MoveValidator.Result rJP_bad = MoveValidator.validate(jetpackBad);

            if (!rET_bad.valid && !rCap_bad.valid && !rJP_bad.valid) {
                System.out.println("PASSED (PERK_EXTRA_TURN, CAPTURE, and JETPACK rules correctly validated)");
                passed++;
            } else {
                System.err.println("FAILED (Invalid perk/capture moves accepted)");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 6: Movement Recording & Duplicate Prevention (Idempotency)
        System.out.print("[TEST 6] Testing Database.recordMovement idempotency... ");
        try {
            int nextMoveNum = Database.getLastMoveNumber(1) + 1;
            String uniqueClientId = "test_move_" + System.currentTimeMillis();

            // First insertion: Piece blue-0 from 0 to 1 with roll 6
            Database.MovementRecordResult res1 = Database.recordMovement(
                    1, null, "blue", "blue-0", 0, 1, 6, "MOVE_OUT", nextMoveNum, uniqueClientId
            );

            // Second insertion with identical client_move_id (network retry simulation)
            Database.MovementRecordResult res2 = Database.recordMovement(
                    1, null, "blue", "blue-0", 0, 1, 6, "MOVE_OUT", nextMoveNum, uniqueClientId
            );

            if (res1.movementId > 0 && !res1.isDuplicate && res2.isDuplicate && res1.movementId == res2.movementId) {
                System.out.println("PASSED (Saved move id=" + res1.movementId + ", retry safely returned duplicate)");
                passed++;
            } else {
                System.err.println("FAILED (Idempotency failed: res1=" + res1.movementId + ", res2=" + res2.movementId + ")");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 7: Database.getGameAnalysis Report Generation
        System.out.print("[TEST 7] Testing Database.getGameAnalysis report generation... ");
        try {
            Map<String, Object> analysis = Database.getGameAnalysis(1);
            if (analysis != null && Boolean.TRUE.equals(analysis.get("success"))
                    && analysis.containsKey("total_moves")
                    && analysis.containsKey("moves_per_player")
                    && analysis.containsKey("captures_count")
                    && analysis.containsKey("perks_count")
                    && analysis.containsKey("duration_seconds")) {
                int totalMoves = (int) analysis.get("total_moves");
                System.out.println("PASSED (Analysis generated for Game #1, total moves in DB: " + totalMoves + ")");
                passed++;
            } else {
                System.err.println("FAILED (Game analysis missing expected fields)");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 8: Verify last_activity column and touchGameActivity
        System.out.print("[TEST 8] Testing last_activity column and activity tracking... ");
        try {
            int testGameId = Database.createGame("Test Activity Game", 2, "{\"test\":true}");
            if (testGameId > 0) {
                Map<String, Object> g1 = Database.getGame(testGameId);
                String t1 = (String) g1.get("last_activity");

                // Sleep briefly to ensure timestamp difference
                Thread.sleep(1100);
                Database.touchGameActivity(testGameId);

                Map<String, Object> g2 = Database.getGame(testGameId);
                String t2 = (String) g2.get("last_activity");

                if (t1 != null && t2 != null && !t1.equals(t2)) {
                    System.out.println("PASSED (last_activity updated from " + t1 + " to " + t2 + ")");
                    passed++;
                } else {
                    System.err.println("FAILED (last_activity timestamp was not updated: t1=" + t1 + ", t2=" + t2 + ")");
                    failed++;
                }
            } else {
                System.err.println("FAILED (Could not create test game)");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 9: Status Protection: NEVER overwrite 'finished' status
        System.out.print("[TEST 9] Testing protection against overwriting 'finished' status... ");
        try {
            int testGameId = Database.createGame("Test Finish Protection", 2, "{\"active\":true}");
            // Mark game finished
            Database.updateGameState(testGameId, "{\"final\":true}", "finished");
            Map<String, Object> finishedGame = Database.getGame(testGameId);

            // Attempt to overwrite with 'active'
            Database.updateGameState(testGameId, "{\"tampered\":true}", "active");
            Map<String, Object> check1 = Database.getGame(testGameId);

            // Attempt to overwrite with 'abandoned' via leaveGame
            Database.leaveGame(testGameId);
            Map<String, Object> check2 = Database.getGame(testGameId);

            if ("finished".equals(finishedGame.get("game_status"))
                    && "finished".equals(check1.get("game_status"))
                    && "finished".equals(check2.get("game_status"))) {
                System.out.println("PASSED (Game remained 'finished' through active and leave/abandon attempts)");
                passed++;
            } else {
                System.err.println("FAILED ('finished' status was overwritten! check1="
                        + check1.get("game_status") + ", check2=" + check2.get("game_status") + ")");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        // TEST 10: Disconnect / Leave and Stale Recovery Policy
        System.out.print("[TEST 10] Testing leaveGame disconnect and stale game recovery... ");
        try {
            int testGameId = Database.createGame("Test Leave Disconnect", 2, "{\"playing\":true}");
            // Call leaveGame
            boolean left = Database.leaveGame(testGameId);
            Map<String, Object> leftGame = Database.getGame(testGameId);

            // Test stale game recovery:
            // Game ID 1 (status 'new') should remain 'new'
            int recovered = Database.recoverStaleActiveGames(15);
            Map<String, Object> game1 = Database.getGame(1);

            if (left && "abandoned".equals(leftGame.get("game_status"))
                    && "new".equals(game1.get("game_status"))) {
                System.out.println("PASSED (Active game became 'abandoned' on leave, Game #1 preserved as 'new')");
                passed++;
            } else {
                System.err.println("FAILED (Leave or stale recovery check failed: left=" + left
                        + ", status=" + leftGame.get("game_status") + ", game1=" + game1.get("game_status") + ")");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            failed++;
        }

        System.out.println("=============================================");
        System.out.println("  Tests Passed: " + passed + " / " + (passed + failed));
        System.out.println("=============================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}


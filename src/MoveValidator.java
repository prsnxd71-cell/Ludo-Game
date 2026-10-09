import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Validates movement requests against Ludo game rules and positional continuity.
 * Protects the database from forged or mathematically impossible moves.
 */
public class MoveValidator {

    private static final List<String> VALID_COLORS = Arrays.asList("blue", "red", "green", "yellow");

    public static class Result {
        public final boolean valid;
        public final String reason;

        public Result(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static Result ok() { return new Result(true, null); }
        public static Result fail(String reason) { return new Result(false, reason); }
    }

    public static Result validate(Map<String, Object> moveData) {
        int gameId = SimpleJson.getInt(moveData, "game_id", -1);
        if (gameId <= 0) return Result.fail("Invalid game_id.");

        String playerColor = SimpleJson.getString(moveData, "player_color", "").toLowerCase().trim();
        if (!VALID_COLORS.contains(playerColor)) {
            return Result.fail("Invalid player_color: " + playerColor);
        }

        String pieceId = SimpleJson.getString(moveData, "piece_id", "").trim();
        if (!pieceId.matches("^" + playerColor + "-[0-3]$")) {
            return Result.fail("Invalid piece_id: " + pieceId + " for player: " + playerColor);
        }

        int fromPos = SimpleJson.getInt(moveData, "from_position", -1);
        int toPos = SimpleJson.getInt(moveData, "to_position", -1);
        int diceRoll = SimpleJson.getInt(moveData, "dice_roll", -1);
        String eventType = SimpleJson.getString(moveData, "event_type", "MOVE");
        int moveNumber = SimpleJson.getInt(moveData, "move_number", -1);
        String clientMoveId = SimpleJson.getString(moveData, "client_move_id", "").trim();

        if (clientMoveId.isEmpty()) return Result.fail("client_move_id is required.");
        if (moveNumber <= 0) return Result.fail("move_number must be greater than 0.");
        if (fromPos < 0 || fromPos > 57) return Result.fail("from_position must be between 0 and 57.");
        if (toPos < 0 || toPos > 57) return Result.fail("to_position must be between 0 and 57.");

        boolean isPerkExtraTurn = "PERK_EXTRA_TURN".equalsIgnoreCase(eventType);
        boolean isCapture = "CAPTURE".equalsIgnoreCase(eventType);
        boolean isPerkJetpack = "PERK_JETPACK".equalsIgnoreCase(eventType);
        boolean isPerkBanana = "PERK_BANANA".equalsIgnoreCase(eventType);

        if (isPerkExtraTurn) {
            if (diceRoll != 0) return Result.fail("dice_roll must be 0 for PERK_EXTRA_TURN.");
            if (fromPos != toPos) return Result.fail("from_position must equal to_position for PERK_EXTRA_TURN.");
        } else if (isCapture) {
            if (diceRoll != 0) return Result.fail("dice_roll must be 0 for CAPTURE event.");
            if (toPos != 0) return Result.fail("to_position must be 0 for CAPTURE event.");
        } else if (isPerkJetpack) {
            int diff = toPos - fromPos;
            if (diff <= 0 || diff > 4) return Result.fail("Invalid Jetpack movement difference.");
        } else if (isPerkBanana) {
            int diff = fromPos - toPos;
            if (diff <= 0 || diff > 3) return Result.fail("Invalid Banana Peel slide difference.");
        } else {
            // Standard move or exit from base
            if (diceRoll < 1 || diceRoll > 6) return Result.fail("dice_roll must be between 1 and 6.");
            if (fromPos == 0) {
                if (diceRoll != 6) {
                    return Result.fail("Piece cannot leave HOME base without rolling a 6.");
                }
                if (toPos != 1) {
                    return Result.fail("Piece leaving HOME base must land on step 1.");
                }
            } else {
                if (toPos - fromPos != diceRoll) {
                    return Result.fail("Movement mismatch: to_position - from_position (" + (toPos - fromPos)
                            + ") does not match dice roll (" + diceRoll + ").");
                }
            }
        }

        // Rule 3: Check database continuity (anti-forgery)
        try {
            int lastKnownPos = Database.getPieceLastPosition(gameId, pieceId);
            if (lastKnownPos != -1) {
                if (fromPos != lastKnownPos) {
                    return Result.fail("Positional inconsistency: Piece " + pieceId + " last recorded at "
                            + lastKnownPos + " but client claimed " + fromPos + ".");
                }
            } else {
                // First move for this piece in the game must start from 0
                if (fromPos != 0) {
                    return Result.fail("Piece " + pieceId + " has no prior moves, must start from position 0.");
                }
            }

            // Rule 4: Sequential move number check
            int lastMoveNum = Database.getLastMoveNumber(gameId);
            if (moveNumber != lastMoveNum + 1) {
                return Result.fail("Out of sequence move: Expected move #" + (lastMoveNum + 1)
                        + ", received #" + moveNumber + ".");
            }

        } catch (SQLException e) {
            return Result.fail("Database validation error: " + e.getMessage());
        }

        return Result.ok();
    }
}


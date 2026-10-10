import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Official MiniMoth REST API client for phone OTP request and verification ONLY.
 * Adheres strictly to MiniMoth documentation: https://minimoth.dev/docs/quickstart/
 */
public class MiniMothClient {

    private static final String BASE_URL = "https://api.minimoth.dev/v1";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class Result {
        public final boolean success;
        public final int statusCode;
        public final String otpId;
        public final String identityId;
        public final String errorCode;
        public final String errorMessage;

        public Result(boolean success, int statusCode, String otpId, String identityId, String errorCode, String errorMessage) {
            this.success = success;
            this.statusCode = statusCode;
            this.otpId = otpId;
            this.identityId = identityId;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public static Result success(String otpId, String identityId) {
            return new Result(true, 200, otpId, identityId, null, null);
        }

        public static Result error(int statusCode, String errorCode, String errorMessage) {
            return new Result(false, statusCode, null, null, errorCode, errorMessage);
        }
    }

    /**
     * Normalizes Indian mobile number to E.164 (+91XXXXXXXXXX) per MiniMoth spec.
     */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+91") && digits.length() == 13) {
            return digits;
        }
        if (digits.startsWith("91") && digits.length() == 12) {
            return "+" + digits;
        }
        if (digits.length() == 10 && digits.matches("^[6-9]\\d{9}$")) {
            return "+91" + digits;
        }
        return digits;
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        // Check +91 followed by 10 digits starting with 6-9
        return phone.matches("^\\+91[6-9]\\d{9}$");
    }

    /**
     * Step 1: Request OTP via MiniMoth (POST /v1/otp/send)
     */
    public static Result sendOtp(String rawPhone) {
        String apiKey = Config.getMiniMothApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_MINIMOTH_API_KEY_HERE")) {
            return Result.error(400, "CONFIG_REQUIRED",
                    "MiniMoth API key is not configured. Please set minimoth.api_key in config.properties.");
        }

        String phone = normalizePhone(rawPhone);
        if (!isValidPhone(phone)) {
            return Result.error(422, "INVALID_PHONE",
                    "Please enter a valid 10-digit mobile number starting with 6-9.");
        }

        String jsonPayload = "{\"phone\":\"" + phone + "\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/otp/send"))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            Map<String, Object> json = SimpleJson.parseObject(body);

            if (status >= 200 && status < 300) {
                String otpId = SimpleJson.getString(json, "otp_id", "");
                return Result.success(otpId, null);
            } else {
                String code = SimpleJson.getString(json, "code", "UNKNOWN_ERROR");
                String error = SimpleJson.getString(json, "error", "Failed to send OTP.");
                return Result.error(status, code, error);
            }
        } catch (IOException | InterruptedException e) {
            return Result.error(503, "NETWORK_ERROR", "Could not connect to MiniMoth OTP service: " + e.getMessage());
        }
    }

    /**
     * Step 2: Verify OTP via MiniMoth (POST /v1/otp/verify)
     */
    public static Result verifyOtp(String rawPhone, String code) {
        String apiKey = Config.getMiniMothApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_MINIMOTH_API_KEY_HERE")) {
            return Result.error(400, "CONFIG_REQUIRED",
                    "MiniMoth API key is not configured. Please set minimoth.api_key in config.properties.");
        }

        String phone = normalizePhone(rawPhone);
        if (!isValidPhone(phone)) {
            return Result.error(422, "INVALID_PHONE", "Please enter a valid 10-digit mobile number.");
        }

        if (code == null || !code.trim().matches("^\\d{6}$")) {
            return Result.error(422, "INVALID_OTP_CODE", "OTP code must be exactly 6 digits.");
        }

        String jsonPayload = "{\"phone\":\"" + phone + "\",\"code\":\"" + code.trim() + "\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/otp/verify"))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Api-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body();

            Map<String, Object> json = SimpleJson.parseObject(body);

            if (status >= 200 && status < 300) {
                String identityId = SimpleJson.getString(json, "identity_id", null);
                return Result.success(null, identityId);
            } else {
                String errCode = SimpleJson.getString(json, "code", "INVALID_OTP");
                String error = SimpleJson.getString(json, "error", "Verification failed.");
                return Result.error(status, errCode, error);
            }
        } catch (IOException | InterruptedException e) {
            return Result.error(503, "NETWORK_ERROR", "Could not connect to MiniMoth OTP service: " + e.getMessage());
        }
    }
}


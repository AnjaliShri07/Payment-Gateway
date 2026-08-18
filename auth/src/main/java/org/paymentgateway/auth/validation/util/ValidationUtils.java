package org.paymentgateway.auth.validation.util;

import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private ValidationUtils() {
        // Prevent instantiation
    }

    // Strict RFC 5322 compatible email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Username regex: 3-50 chars, alphanumeric, underscore, hyphen
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");

    // Password components
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[@#$%^&+=!._\\-*~()<>\\[\\]{}:;,?]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    // Reserved usernames that cannot be registered publicly
    private static final Set<String> RESERVED_USERNAMES = Set.of(
        "administrator", "root", "system", "null", "superuser",
        "anonymous", "guest", "support", "api", "auth", "security"
    );

    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.length() > 100) {
            return false;
        }
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isValidUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String trimmed = username.trim();
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        return !isReservedUsername(trimmed);
    }

    public static boolean isReservedUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return RESERVED_USERNAMES.contains(username.trim().toLowerCase());
    }

    public static boolean isStrongPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }

        if (password.length() < 8 || password.length() > 40) {
            return false;
        }

        if (WHITESPACE_PATTERN.matcher(password).find()) {
            return false;
        }

        boolean hasUpper = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLower = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_CHAR_PATTERN.matcher(password).find();

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Sanitizes input to prevent basic XSS and injection attacks.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .trim();
    }

    public static boolean isAlphaNumeric(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        return input.chars().allMatch(Character::isLetterOrDigit);
    }
}

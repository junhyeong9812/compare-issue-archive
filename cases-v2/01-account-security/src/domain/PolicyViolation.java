package domain;

public enum PolicyViolation {
    TOO_SHORT("password is too short"),
    MISSING_DIGIT("password must contain a digit"),
    CONTAINS_USERNAME("password must not contain the username"),
    RECENTLY_USED("password was used recently");

    private final String message;

    PolicyViolation(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}

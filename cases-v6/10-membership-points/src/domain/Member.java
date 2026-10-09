package domain;

/**
 * A membership account. Member numbers are opaque strings: store-card members
 * carry the 6-digit number printed on the card,
 * app members have shorter app-issued numbers, corporate members a C-code.
 */
public record Member(String memberNo, String name, Grade grade, String channel) {}

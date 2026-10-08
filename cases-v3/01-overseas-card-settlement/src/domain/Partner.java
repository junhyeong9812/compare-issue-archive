package domain;

/** Merchant partner. feeBps is the partner commission in basis points of gross KRW. */
public record Partner(String id, String name, int feeBps, boolean active) {}

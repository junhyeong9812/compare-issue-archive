import java.time.LocalDateTime;

public record Order(String id, LocalDateTime createdAt, long amountCents) {}

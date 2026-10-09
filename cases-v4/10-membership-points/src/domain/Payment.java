package domain;

public record Payment(String payId, String memberNo, String merchant, long amount, String status) {
    public boolean approved() {
        return "APPROVED".equals(status);
    }
}

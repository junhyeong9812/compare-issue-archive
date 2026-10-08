package api;

public record Response(int status, String body) {
    public static Response ok(String body) {
        return new Response(200, body);
    }
}

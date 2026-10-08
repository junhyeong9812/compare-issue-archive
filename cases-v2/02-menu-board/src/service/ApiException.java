package service;

public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String what) {
        return new ApiException(404, what + " not found");
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public int status() {
        return status;
    }
}

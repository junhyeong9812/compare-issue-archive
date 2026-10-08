package service;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final int status;
    private final List<String> problems;

    public ValidationException(int status, List<String> problems) {
        super(String.join("; ", problems));
        this.status = status;
        this.problems = List.copyOf(problems);
    }

    public static ValidationException of(int status, String problem) {
        return new ValidationException(status, List.of(problem));
    }

    public int status() {
        return status;
    }

    public List<String> problems() {
        return problems;
    }
}

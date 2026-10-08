package api;

import java.util.Map;

public record Request(String method, String path, Map<String, String> headers, Map<String, String> body) {
    public Request {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? Map.of() : Map.copyOf(body);
    }

    public String header(String name) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    public String field(String name) {
        return body.get(name);
    }
}

package api;

import java.util.Map;

public record Request(String method, String path, Map<String, String> headers, Map<String, String> params, String body) {
    public Request {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        params = params == null ? Map.of() : Map.copyOf(params);
        body = body == null ? "" : body;
    }

    public String header(String name) {
        return headers.get(name);
    }

    public String param(String name) {
        return params.get(name);
    }
}

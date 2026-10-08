import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStore implements KeyValueStore {
    private final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public void put(String key, String value) {
        data.put(key, value);
    }
}

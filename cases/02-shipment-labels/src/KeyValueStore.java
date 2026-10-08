public interface KeyValueStore {
    String get(String key);

    void put(String key, String value);
}

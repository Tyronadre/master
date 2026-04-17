package de.tyro.mcnetwork.simulation.protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProtocolSettings {
    private final Map<String, ProtocolSettingsEntry<?>> entries = new HashMap<>();

    public <T> void registerSetting(String key, Class<T> clazz, Supplier<T> valueSupplier, Consumer<T> onChange) {
        entries.put(key, new ProtocolSettingsEntry<>(key, clazz, valueSupplier, onChange));
    }

    public <T> void registerSetting(String key, Class<T> clazz, Supplier<T> value) {
        registerSetting(key, clazz, value, null);
    }

    public ProtocolSettingsEntry<?> getSetting(String key) {
        return entries.get(key);
    }

    public int size() {
        return entries.size();
    }

    public List<ProtocolSettingsEntry<?>> getAll() {
        return new ArrayList<>(entries.values());
    }

    public Collection<String> keys() {
        return entries.keySet();
    }

    public static class ProtocolSettingsEntry<T> {
        String key;
        Class<T> clazz;
        Supplier<T> value;
        Consumer<T> onChange;

        public ProtocolSettingsEntry(String key, Class<T> clazz, Supplier<T> value, Consumer<T> onChange) {
            this.key = key;
            this.clazz = clazz;
            this.value = value;
            this.onChange = onChange;
        }

        public boolean isSettable() {
            return onChange != null;
        }

        public String getKey() {
            return key;
        }

        public T getValue() {
            return value.get();
        }

        public Class<T> getValueClass() {
            return clazz;
        }

        public void setValue(Object value) {
            onChange.accept((T) value);
        }
    }

    @Override
    public String toString() {
        return ProtocolSettings.class.getSimpleName() + "[registeredSettings: " + entries.size() + "]";
    }
}

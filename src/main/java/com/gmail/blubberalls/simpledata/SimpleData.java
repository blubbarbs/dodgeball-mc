package com.gmail.blubberalls.simpledata;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Function;

public class SimpleData {
    public static class Key<T> {
        private final NamespacedKey key;
        private final PersistentDataType<?, T> pdType;

        public Key(NamespacedKey key, PersistentDataType<?, T> pdType) {
            this.key = key;
            this.pdType = pdType;
        }

        public Key(String namespace, String key, PersistentDataType<?, T> pdType) {
            this(new NamespacedKey(namespace, key), pdType);
        }

        public Key(String key, PersistentDataType<?, T> pdType) {
            this("pd", key, pdType);
        }

        public NamespacedKey getNamespacedKey() {
            return key;
        }

        public PersistentDataType<?, T> getPersistentDataType() {
            return pdType;
        }
    }

    public interface Serializable {
        void serialize(PersistentDataContainer container);
    }

    public static class DataType<T extends Serializable> implements PersistentDataType<PersistentDataContainer, T> {
        private final Class<T> clazz;
        private final Function<PersistentDataContainer, T> factory;

        public DataType(Class<T> clazz, Function<PersistentDataContainer, T> factory) {
            this.clazz = clazz;
            this.factory = factory;
        }

        @Override
        public Class<PersistentDataContainer> getPrimitiveType() {
            return PersistentDataContainer.class;
        }

        @Override
        public Class<T> getComplexType() {
            return clazz;
        }

        @Override
        public PersistentDataContainer toPrimitive(T t, PersistentDataAdapterContext persistentDataAdapterContext) {
            PersistentDataContainer pdc = persistentDataAdapterContext.newPersistentDataContainer();
            t.serialize(pdc);
            return pdc;
        }

        @Override
        public T fromPrimitive(PersistentDataContainer pdc, PersistentDataAdapterContext persistentDataAdapterContext) {
            return factory.apply(pdc);
        }
    }

    public static boolean has(PersistentDataContainer container, Key<?> key) {
        return container.has(key.getNamespacedKey());
    }

    public static boolean has(PersistentDataHolder holder, Key<?> key) {
        return has(holder.getPersistentDataContainer(), key);
    }

    public static <T> T getOrDefault(PersistentDataContainer pdc, Key<T> key, T dflt) {
        return has(pdc, key) ? pdc.get(key.getNamespacedKey(), key.getPersistentDataType()) : dflt;
    }

    public static <T> T getOrDefault(PersistentDataHolder holder, Key<T> key, T dflt) {
        return getOrDefault(holder.getPersistentDataContainer(), key, dflt);
    }

    public static <T> T get(PersistentDataContainer pdc, Key<T> key) {
        return getOrDefault(pdc, key, null);
    }

    public static <T> T get(PersistentDataHolder holder, Key<T> key) {
        return get(holder.getPersistentDataContainer(), key);
    }

    public static PersistentDataContainer getRaw(PersistentDataContainer pdc, Key<?> key) {
        return pdc.get(key.getNamespacedKey(), PersistentDataType.TAG_CONTAINER);
    }

    public static PersistentDataContainer getRaw(PersistentDataHolder holder, Key<?> key) {
        return getRaw(holder.getPersistentDataContainer(), key);
    }

    public static void remove(PersistentDataContainer pdc, Key<?> key) {
        pdc.remove(key.getNamespacedKey());
    }

    public static void remove(PersistentDataHolder holder, Key<?> key) {
        remove(holder.getPersistentDataContainer(), key);
    }

    public static <T> void set(PersistentDataContainer pdc, Key<T> key, T value) {
        if (value == null)
            return;

        pdc.set(key.getNamespacedKey(), key.getPersistentDataType(), value);
    }

    public static void setRaw(PersistentDataContainer pdc, Key<?> key, PersistentDataContainer value) {
        if (value == null)
            return;

        pdc.set(key.getNamespacedKey(), PersistentDataType.TAG_CONTAINER, value);
    }

    public static void setRaw(PersistentDataHolder holder, Key<?> key, PersistentDataContainer value) {
        setRaw(holder.getPersistentDataContainer(), key, value);
    }

    public static <T> void set(PersistentDataHolder holder, Key<T> key, T value) {
        set(holder.getPersistentDataContainer(), key, value);
    }
}
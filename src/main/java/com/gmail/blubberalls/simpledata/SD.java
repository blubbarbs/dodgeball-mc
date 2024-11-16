package com.gmail.blubberalls.simpledata;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SD {
    /**
     * A class to hold the {@link PersistentDataType} and {@link NamespacedKey} instances to simplify accessing
     * the PDC data. This links the data type to its key, allowing for automatic serialization/deserialization
     * by using the static methods in {@link SD}
     * @param <T> The data type to be stored at the given key
     */
    public static class Key<T> implements Keyed {
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
            this(new NamespacedKey("pd", key), pdType);
        }

        @Override
        public NamespacedKey getKey() {
            return key;
        }

        public PersistentDataType<?, T> getDataType() {
            return pdType;
        }
    }

    /**
     * This interface allows for serialization/deserialization of the implementing class using
     * {@link PersistentDataContainer}. This does not use any other data primitive.
     */
    public interface Serializable {
        /**
         * @param clazz The class to be serialized/deserialized
         * @param constructor This should be a constructor which takes in a single {@link PersistentDataContainer}
         * instance, which will act as the deserializer
         * @return A new {@link PersistentDataType} which will handle the serialization/deserialization of
         * the class
         * @param <T> The class implementing {@link Serializable}
         */
        static <T extends Serializable> PersistentDataType<PersistentDataContainer, T> createDataType(Class<T> clazz, Function<PersistentDataContainer, T> constructor) {
            return new PersistentDataType<>() {
                @Override
                public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
                    return PersistentDataContainer.class;
                }

                @Override
                public @NotNull Class<T> getComplexType() {
                    return clazz;
                }

                @Override
                public @NotNull PersistentDataContainer toPrimitive(@NotNull T t, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
                    PersistentDataContainer pdc = persistentDataAdapterContext.newPersistentDataContainer();
                    t.serialize(pdc);
                    return pdc;
                }

                @Override
                public @NotNull T fromPrimitive(@NotNull PersistentDataContainer pdc, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
                    return constructor.apply(pdc);
                }
            };
        }

        /**
         * @param pdc An empty {@link PersistentDataContainer} to be filled with your object's data
         */
        void serialize(PersistentDataContainer pdc);
    }

    public static boolean has(PersistentDataContainer pdc, Key<?> key) {
        return pdc.has(key.getKey());
    }

    public static boolean has(PersistentDataHolder holder, Key<?> key) {
        return has(holder.getPersistentDataContainer(), key);
    }

    public static <T> T getOrDefault(PersistentDataContainer pdc, Key<T> key, T dflt) {
        return has(pdc, key) ? pdc.get(key.getKey(), key.getDataType()) : dflt;
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

    public static void remove(PersistentDataContainer pdc, Key<?> key) {
        pdc.remove(key.getKey());
    }

    public static void remove(PersistentDataHolder holder, Key<?> key) {
        remove(holder.getPersistentDataContainer(), key);
    }

    public static <T> void set(PersistentDataContainer pdc, Key<T> key, T value) {
        if (value == null)
            return;

        pdc.set(key.getKey(), key.getDataType(), value);
    }

    public static <T> void set(PersistentDataHolder holder, Key<T> key, T value) {
        set(holder.getPersistentDataContainer(), key, value);
    }
}

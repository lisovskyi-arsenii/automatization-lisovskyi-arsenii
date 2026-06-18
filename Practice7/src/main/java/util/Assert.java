package util;

import java.util.List;

public final class Assert {
    private Assert() {
        throw new UnsupportedOperationException("Assert class cannot be instantiated - utility class");
    }

    public static void hasText(final String text) throws IllegalArgumentException {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }

        if (text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be blank");
        }
    }

    public static void notNull(final Object object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
    }

    public static void notEmpty(final List<?> list) throws IllegalArgumentException {
        notNull(list);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be empty");
        }
    }

    public static boolean isInstanceOf(final Object object, final Class<?> clazz) throws IllegalArgumentException {
        notNull(object);
        notNull(clazz);

        return clazz.isInstance(object);
    }

    public static void hasLength(final String text) throws IllegalArgumentException {
        notNull(text);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
    }
}

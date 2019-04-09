package jcog.event;

/** something that can be disabled. */
@Deprecated @FunctionalInterface public interface Off extends AutoCloseable {
    void close();
}

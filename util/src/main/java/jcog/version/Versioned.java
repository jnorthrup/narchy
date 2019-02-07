package jcog.version;

public interface Versioned<X> {
    X get();

    boolean set(X nextValue);

    void pop();

    /**
     * replaces the existing value (or invokes ordinary set(x) if none),
     * an implementation can refuse the new value, indicated by return value
     */
    boolean replace(X y);
}

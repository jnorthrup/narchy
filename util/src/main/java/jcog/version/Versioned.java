package jcog.version;

public interface Versioned<X> {
    X get();

    void pop();

    boolean set(X nextValue, Versioning<X> context);


    /**
     * replaces the existing value (or invokes ordinary set(x) if none),
     * an implementation can refuse the new value, indicated by return value
     */
    boolean replace(X y, Versioning<X> context);

}

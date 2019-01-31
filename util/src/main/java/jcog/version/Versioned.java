package jcog.version;

public interface Versioned<X> {
    X get();

    boolean set(X nextValue);

    void pop();

    /** forcefully sets the value, bypassing any checks or constraints that setAt(x) otherwise applies */
    void force(X y);
}

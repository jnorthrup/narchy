package org.teavm.classlib.java.lang;

import java.util.function.Supplier;

/**
 *
 * @author Alexey Andreev
 * @param <T> type of a value stored by thread local.
 */
public class TThreadLocal<T> extends TObject {
    private boolean initialized;
    private T value;

    public TThreadLocal() {
        super();
    }
    
    public static <T> ThreadLocal<T> withInitial(Supplier<? extends T> supplier) {
        return new ThreadLocal() {
            @Override
            protected Object initialValue() {
                return supplier.get();
            }
        };
    }


    protected T initialValue() {
        return null;
    }

    public T get() {
        if (!initialized) {
            value = initialValue();
            initialized = true;
        }
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public void remove() {
        initialized = false;
        value = null;
    }
}

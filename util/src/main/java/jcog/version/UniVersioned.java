package jcog.version;

import org.jetbrains.annotations.Nullable;

/** supports only one state and refuses change if a value is held */
public class UniVersioned<X> implements Versioned<X> {

    protected X value;

    public UniVersioned() {
    }

    @Override
    public X get() {
        return value;
    }

    @Override
    public final boolean replace(X x, Versioning<X> context) {
        if (value==null) {
            return set(x, context);
        } else {
            if (valid(x, context)) {
                value = x;
                return true;
            } else
                return false;
        }
    }

    @Override
    public final String toString() {
        X v = get();
        if (v != null)
            return v.toString();
        return "null";
    }

    @Override
    public boolean equals(Object obj) {
        return this==obj;
    }


    /** override to filter */
    protected boolean valid(X x, Versioning<X> context) {
        return true;
    }

    @Override
    public final boolean set(X next, @Nullable Versioning<X> context) {
        X prev = value;
        if (prev!=null) {

            int m = match(prev, next);
            if (m >= 0) {
                if (m == +1)
                    value = next;
                return true;
            }
        } else {
            if (valid(next, context) && (context==null || context.add(this))) {
                value = next;
                return true;
            }
        }
        return false;
    }

    /**
     * @return value:
     *      +1 accept, replace with new value
     *      0 accept, keep original value
     *      -1  refuse
     *
     */
    protected int match(X prevValue, X nextValue) {
        return prevValue.equals(nextValue) ? 0 : -1;
    }

    @Override
    public void pop() {
        value = null;
    }

}

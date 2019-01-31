package jcog.version;

import jcog.TODO;

/** supports only one state and refuses change if a value is held */
public class UniVersioned<X> implements Versioned<X> {

    @Deprecated protected final Versioning context;

    protected X value;

    public UniVersioned(Versioning context) {
        this.context = context;
    }

    @Override
    public X get() {
        return value;
    }

    @Override
    public void force(X x) {
        if (value==null) {
            if (!context.add(this)); //addAt callback so it can be erased
                throw new TODO();
        }
        value = x;
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
    protected boolean valid(X x) {
        return true;
    }

    @Override
    public final boolean set(X next) {
        X prev = value;
        if (prev!=null) {
            int m = match(prev, next);
            if (m >= 0) {
                if (m == +1)
                    value = next;
                return true;
            }
        } else {
            if (valid(next) && context.add(this)) {
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

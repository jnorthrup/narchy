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
            //add callback so it can be erased
            if (!context.add(this))
                throw new TODO("context overflow");
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
    public boolean set(X nextValue) {
        X v = value;
        if (v!=null) {
            int m = match(v, nextValue);
            if (m == 0) {
                return false;
            } else if (m == 1) {
                value = nextValue;
            } //else if (m == -1) { /* no change */            }
            return true;
        } else {
            if (!valid(nextValue))
                return false;

            //initialize
            value = nextValue;

            if (context.add(this))
                return true;
            else {
                value = null; //undo
                return false;
            }
        }

    }

    /**
     * @return value:
     *      +1 accept, replace with new value
     *      0  refuse
     *      -1 accept, keep original value
     */
    protected int match(X prevValue, X nextValue) {
        return prevValue.equals(nextValue) ? -1 : 0;
    }

    @Override
    public void pop() {
        value = null;
    }
}

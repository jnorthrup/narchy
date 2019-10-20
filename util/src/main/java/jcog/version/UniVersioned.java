package jcog.version;

/** supports only one state and refuses change if a value is held */
public class UniVersioned<X> implements Versioned<X> {

    protected X value;

    UniVersioned() {
    }

    @Override
    public final X get() {
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
        var v = get();
        if (v != null)
            return v.toString();
        return "null";
    }

    @Override
    public final boolean equals(Object obj) {
        return this==obj;
    }


    /** override to filter */
    protected boolean valid(X x, Versioning<X> context) {
        return true;
    }

    @Override
    public final boolean set(X next, /*@Nullable */Versioning<X> context) {
        var prev = value;
        if (prev == null) {
            if (valid(next, context) && (/*context==null || */context.add(this))) {
                value = next;
                return true;
            }
        } else {
            switch (merge(prev, next)) {
                case 0:
                    return true;
                case +1:
                    //assumes that merge has tested the new value, if necessary: valid(next, context)
                    value = next; //replace
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
    protected int merge(X prev, X next) {
        if (prev.equals(next))
            return 0;
        else
            return -1;
    }
//    @Override
//    protected int merge(Term prevValue, Term nextValue) {
//        if (prevValue.equals(nextValue))
//            return 0;
//
////            if (prevValue.unify(nextValue, (Unify) context)) {
////                if (nextValue.hasAny(Op.Temporal)) {
////                    //prefer more specific temporal matches, etc?
////                    if (prevValue.hasXternal() && !nextValue.hasXternal()) {
////                        return +1;
////                    }
////                }
////                return 0;
////            } else
//        else
//            return -1;
//    }


    @Override
    public void pop() {
        value = null;
    }

}

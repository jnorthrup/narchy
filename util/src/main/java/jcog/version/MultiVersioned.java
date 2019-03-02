package jcog.version;

import jcog.data.list.FasterList;
import org.jetbrains.annotations.Nullable;

/**
 * Versioned implementation supporting multiple snapshot values
 * Maintains a versioned snapshot history (stack) of a changing value.
 * Managed by a Versioning context
 */
public class MultiVersioned<X> extends FasterList<X> implements Versioned<X> {


    @Deprecated protected final Versioning context;


    public MultiVersioned(Versioning<X> sharedContext, int initialCap) {
        super(initialCap);
        this.context = sharedContext;
    }

//    public MultiVersioned(Versioning<X> sharedContext, X[] emptyArray) {
//        super(0, emptyArray);
//        this.context = sharedContext;
//    }

    @Override
    public final boolean equals(Object otherVersioned) {
        return this == otherVersioned;
    }

    @Override
    public boolean replace(X y) {
        if (size==0) {
            return set(y);
        } else {
            replaceLast(y);
            return true;
        }
    }

    /**
     * gets the latest value
     */
    @Override
    @Nullable
    public X get() {
        int s = this.size;
        return s > 0 ? this.items[s - 1] : null;
    }


    /**
     * sets thens commits
     * returns null if the capacity was hit, or some other error
     */
    @Override
    public boolean set(X nextValue) {

        //unique value mode only:
//        X existing = get();
//        if (existing!=null) { return existing.equals(nextValue); }

        if (size > 0 && get().equals(nextValue))
            return true;

        if (addWithoutResize(nextValue)) {
            if (context.add(this))
                return true;
            else
                pop();
        }
        return false;
    }


    @Override
    public final String toString() {
        X v = get();
        if (v != null)
            return v.toString();
        return "null";
    }

    public final String toStackString() {
        StringBuilder sb = new StringBuilder("(");
        int s = size();
        for (int i = 0; i < s; i++) {

            sb.append(get(i));

            if (i < s - 1)
                sb.append(", ");
        }
        return sb.append(')').toString();

    }


    @Override
    public void pop() {


        items[--size] = null;


    }


}

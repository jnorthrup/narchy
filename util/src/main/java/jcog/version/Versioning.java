package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * versioning context that holds versioned instances
 * a maximum stack size is provided at construction and will not be exceeded
 */
public class Versioning<X> extends
        //FastList<Versioned<X>> {
        FasterList<Versioned<X>> {

    public int ttl;

    public Versioning(int stackMax, int initialTTL) {
        super(0, new Versioned[stackMax]);
        assert(stackMax > 0);
        setTTL(initialTTL);
    }

    @NotNull
    @Override
    public String toString() {
        return size() + ":" + super.toString();
    }


    public final boolean revertLive(int to) {
        revert(to);
        return live();
    }

    /**
     * reverts/undo to previous state
     * returns whether any revert was actually applied
     */
    public final boolean revert(int when) {

        int s = size;
        if (s == 0 || s==when)
            return false;

        int c = s - when;
        final Versioned<X>[] i = this.items;

        while (c-- > 0) {
            i[--s].pop();
            i[s] = null;
        }

        this.size = s;
        return true;
    }


    @Override
    public void clear() {
        revert(0);
    }

    @Override
    public final boolean add(/*@NotNull*/ Versioned<X> newItem) {
        Versioned<X>[] ii = this.items;
        if (ii.length > this.size) {
            ii[this.size++] = newItem; //cap
            return true;
        }
        return false;
    }

    @Override
    public void add(int index, Versioned<X> element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Versioned<X>> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Versioned<X>> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAllIterable(Iterable<? extends Versioned<X>> iterable) {
        throw new UnsupportedOperationException();
    }

    /** returns remaining TTL */
    public final int stop() {
        int t = ttl;
        setTTL(0);
        return t;
    }



//    /**
//     * empty for subclass impl
//     */
//    public void onDeath() {
//
//    }

    /**
     * whether the unifier should continue: if TTL is non-zero.
     */
    public final boolean live() {
        return ttl > 0;
    }

    public final void setTTL(int ttl) {
        this.ttl = ttl;
    }
}

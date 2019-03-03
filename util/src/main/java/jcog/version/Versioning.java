package jcog.version;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * versioning context that holds versioned instances
 * a maximum stack size is provided at construction and will not be exceeded
 */
public class Versioning<X> {

    protected final Versioned[] items;
    protected int size = 0;

    public int ttl;

    public Versioning(int stackMax) {
        this.items = new Versioned[stackMax];
        assert (stackMax > 0);
    }

    public Versioning(int stackMax, int initialTTL) {
        this(stackMax);
        setTTL(initialTTL);
    }

    @Override
    public String toString() {
        return size + ":" + super.toString();
    }


    public final boolean revertLive(int before, int cost) {
        ttl -= cost;
        return revertLive(before);
    }

    public final boolean revertLive(int before) {
        if (live()) {
            revert(before);
            return true;
        } else {
            return false;
        }
    }


    /**
     * reverts/undo to previous state
     * returns whether any revert was actually applied
     */
    public final boolean revert(int when) {

        int s = size;
        if (s <= when)
            return false;

        final Versioned[] i = this.items;

        while (s>when) {
            i[--s].pop();
        }
        Arrays.fill(i, when, size, null);
        this.size = s;

        return true;
    }
    public final void forEach(Consumer<Versioned<X>> each) {


        int s = size;
        if (s <= 0)
            return;

        final Versioned[] i = this.items;

        while (s>0) {
            each.accept(i[--s]);
        }

    }

    public final boolean revert(int when, Consumer<Versioned<X>> each) {

        int s = size;
        if (s <= when)
            return false;

        final Versioned[] i = this.items;

        while (s>when) {
            Versioned<X> victim = i[--s];
            each.accept(victim);
            victim.pop();
        }
        Arrays.fill(i, when, size, null);
        this.size = s;

        return true;
    }

    public Versioning clear() {
        revert(0);
        return this;
    }

    public final boolean add(/*@NotNull*/ Versioned<X> newItem) {
        Versioned<X>[] ii = this.items;
        if (ii.length > this.size) {
            ii[this.size++] = newItem;
            return true;
        }
        return false; //capacity exceeded
    }

    /**
     * returns remaining TTL
     * callees should have something to do with the returned TTL value, otherwise it may indicate a stop() is zero'ing via mistakenly lost ttl
     */
    @Deprecated public int stop() {
        int t = this.ttl;
        ttl = 0;
        return t;
    }


    /**
     * whether the unifier should continue: if TTL is non-zero.
     */
    public boolean live() {
        return ttl > 0;
    }

    public final void setTTL(int ttl) {
//        assert (ttl > 0);
        this.ttl = ttl;
    }

    /**
     * stack height counter
     */
    public final int size() {
        return size;
    }


    public final void pop() {
        revert(size - 1);
    }

    public final void clear(Consumer<Versioned<X>> each) {
        if (each == null) {
            clear();
        } else {
            revert(0, each);
        }
    }

    public final boolean set(Versioned<X> x, X y) {
        return x.set(y, this);
    }
}

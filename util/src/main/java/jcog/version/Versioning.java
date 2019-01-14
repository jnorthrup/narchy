package jcog.version;

import java.util.Arrays;

/**
 * versioning context that holds versioned instances
 * a maximum stack size is provided at construction and will not be exceeded
 */
public class Versioning<X> {

    private final Versioned[] items;
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
     */
    public final void stop() {
        ttl = 0;
    }


    /**
     * whether the unifier should continue: if TTL is non-zero.
     */
    private boolean live() {
        return ttl > 0;
    }

    public final void setTTL(int ttl) {
        assert (ttl > 0);
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

}

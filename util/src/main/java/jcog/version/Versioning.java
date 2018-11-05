package jcog.version;

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
        assert(stackMax > 0);
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

    /** hard clear; use with caution  */
    private void reset() {

        int s = this.size;
        this.size = 0;

        for (int i = 0; i < s; i++) {
            items[i].clear();
            items[i] = null;
        }

    }

    /**
     * reverts/undo to previous state
     * returns whether any revert was actually applied
     */
    public final boolean revert(int when) {

        int s = size;
        if (s == 0 || s <= when)
            return false;

        if (when == 0) {
            reset();
        } else {

            int c = s - when;
            final Versioned[] i = this.items;

            while (c-- > 0) {
                Versioned x = i[--s];
                if (x != null) {
                    x.pop();
                    i[s] = null;
                }
            }

            this.size = s;
            assert (s == when);

        }
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
        return false;
    }

    /** returns remaining TTL */
    public final void stop() {
        setTTL(0);
    }
















    /**
     * whether the unifier should continue: if TTL is non-zero.
     */
    private boolean live() {
        return ttl > 0;
    }

    public final void setTTL(int ttl) {
        this.ttl = ttl;
    }

    /**
     * stack height counter
     */
    public final int size() {
        return size;
    }


}

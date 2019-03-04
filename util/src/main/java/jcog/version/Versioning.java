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
    public final void forEach(Consumer<Versioned<X>> each) {


        int s = size;
        if (s <= 0)
            return;

        final Versioned[] i = this.items;

        while (s>0) {
            each.accept(i[--s]);
        }

    }

    /**
     * reverts/undo to previous state
     * returns whether any revert was actually applied
     */
    public final boolean revert(int when) {

        final int sizePrev;
        if ((sizePrev = size) <= when)
            return false;

        int sizeNext = sizePrev;
        final Versioned[] i = this.items;

        while (sizeNext>when) {

            i[--sizeNext].pop();

//            Versioned ii;
//            if ((ii = i[--sizeNext])==null)
//                throw new WTF();
//            ii.pop();

        }
        Arrays.fill(i, when, sizePrev, null);
        this.size = sizeNext;

        return true;
    }


    public final boolean revert(int when, Consumer<Versioned<X>> each) {

        final int sizePrev;
        if ((sizePrev = size) <= when)
            return false;

        int sizeNext = sizePrev;
        final Versioned[] i = this.items;

        while (sizeNext>when) {
            Versioned<X> victim = i[--sizeNext];
            each.accept(victim);
            victim.pop();
        }
        Arrays.fill(i, when, sizePrev, null);
        this.size = sizeNext;

        return true;
    }

    public Versioning clear() {
        revert(0);
        return this;
    }

    public final boolean add(/*@NotNull*/ Versioned<X> newItem) {
        if (newItem == null)
            throw new NullPointerException();

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



    public final boolean set(Versioned<X> x, X y) {
        return x.set(y, this);
    }
}

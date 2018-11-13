package jcog.data.bit;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/** atomic metal */
public class AtomicMetalBitSet extends MetalBitSet {

    private static final AtomicIntegerFieldUpdater<AtomicMetalBitSet> X = AtomicIntegerFieldUpdater.newUpdater(AtomicMetalBitSet.class, "x");

    private volatile int x;

    @Override
    public void setAll() {
        setDirect(0xffffffff);
    }

    @Override
    public boolean get(int i) {
        return (X.get(this) & (1 << i)) != 0;
    }

    public boolean compareAndSet(int i, boolean expect, boolean set) {
        int mask = 1 << i;
        final boolean[] got = {false};
        X.accumulateAndGet(this, mask, (v,m)->{
            if (((v & m) != 0)==expect) {
                
                got[0] = true;
                return set ? v|m : v&(~m);
            } else {
                
                return v;
            }
        });
        return got[0];
    }

    @Override
    public void set(int i) {
        int mask = 1<<i;
        X.accumulateAndGet(this, mask, (v,m)-> v|m);
        //X.getAndUpdate(this, v-> v|(mask) );
    }

    public boolean getAndSet(int i) {
        int mask = 1<<i;
        return ((X.getAndAccumulate(this, mask, (v,m) -> v|m)) & mask) != 0;
        //return (X.getAndUpdate(this, v-> v|(mask) ) & mask) != 0;
    }

    @Override
    public void clear(int i) {
        int antimask = ~(1<<i);
        X.accumulateAndGet(this, antimask, (v,am)-> v&(am) );
    }

    public boolean getAndClear(int i) {
        int mask = (1<<i);
        int antimask = ~mask;
        return (X.accumulateAndGet(this, antimask, (v,am)-> v&(am) ) & mask) != 0;
    }

    @Override
    public void clear() {
         setDirect(0);
    }

    @Override
    public int cardinality() {
        return Integer.bitCount(X.get(this));
    }

    public void copyFrom(AtomicMetalBitSet copyFrom) {
        setDirect(copyFrom.getDirect());
    }

    public void setDirect(int bitmask) {
        X.set(this, bitmask);
    }
    public int getDirect() {
        return X.get(this);
    }

    public String toBitString() {
        return Integer.toBinaryString(getDirect());
    }

    public boolean getAndSet(short b, boolean pressed) {
        return pressed ? getAndSet(b) : getAndClear(b);
    }

}

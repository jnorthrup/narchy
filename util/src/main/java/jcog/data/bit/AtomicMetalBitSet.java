package jcog.data.bit;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/** atomic metal */
public class AtomicMetalBitSet extends MetalBitSet {

    static final AtomicIntegerFieldUpdater<AtomicMetalBitSet> _x = AtomicIntegerFieldUpdater.newUpdater(AtomicMetalBitSet.class, "x");

    private volatile int x;

    @Override
    public void setAll() {
        x = 0xffffffff;
    }

    @Override
    public boolean get(int i) {
        return (x & (1 << i)) != 0;
    }

    public boolean compareAndSet(int i, boolean expect, boolean set) {
        int mask = 1 << i;
        final boolean[] got = {false};
        _x.updateAndGet(this, v->{
            if (((v & mask) != 0)==expect) {
                //set
                got[0] = true;
                return set ? v|mask : v&(~mask);
            } else {
                //no change
                return v;
            }
        });
        return got[0];
    }

    @Override
    public void set(int i) {
        int mask = 1<<i;
        _x.getAndUpdate(this, v-> v|(mask) );
    }

    public boolean getAndSet(int i) {
        int mask = 1<<i;
        return (_x.getAndUpdate(this, v-> v|(mask) ) & mask) > 0;
    }

    @Override
    public void clear(int i) {
        int antimask = ~(1<<i);
        _x.getAndUpdate(this, v-> v&(antimask) );
    }

    public boolean getAndClear(int i) {
        int mask = (1<<i);
        int antimask = ~mask;
        return (_x.getAndUpdate(this, v-> v&(antimask) ) & mask) > 0;
    }

    @Override
    public void clearAll() {
        x = 0;
    }

    @Override
    public int getCardinality() {
        return Integer.bitCount(x);
    }

    @Override
    public boolean isAllOff() {
        return x == 0;
    }

    public void copyFrom(AtomicMetalBitSet copyFrom) {
        setDirect(copyFrom.x);
    }

    public void setDirect(int bitmask) {
        _x.set(this, bitmask);
    }

    public String toBitString() {
        return Integer.toBinaryString(x);
    }

    public boolean getAndSet(short b, boolean pressed) {
        return pressed ? getAndSet(b) : getAndClear(b);
    }

}

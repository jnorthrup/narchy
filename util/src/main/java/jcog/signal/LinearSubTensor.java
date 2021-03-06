package jcog.signal;

import jcog.Util;

public class LinearSubTensor implements Tensor, Comparable<LinearSubTensor> {

    private final int volume;
    private final int linearEnd;
    private final int linearStart;
    private final Tensor parent;
    private final int hash;

    public LinearSubTensor(int linearEnd, int linearStart, Tensor parent) {
        this.linearEnd = linearEnd;
        this.linearStart = linearStart;
        this.parent = parent;
        this.volume = linearEnd - linearStart;
        this.hash = Util.hashCombine(System.identityHashCode(parent), linearStart, linearEnd);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof LinearSubTensor) {
            LinearSubTensor t = (LinearSubTensor)obj;
            return (parent == t.parent && linearStart == t.linearStart && linearEnd == t.linearEnd);
        }
        return false;
    }

    @Override
    public float getAt(int linearCell) {
        assert(linearCell < volume);
        return parent.getAt(linearCell + linearStart);
    }

    @Override
    public int volume() {
        return volume;
    }

    @Override
    public int[] shape() {
        return new int[] {volume};
    }

    @Override
    public final int compareTo(LinearSubTensor o) {
        if (this == o) return 0;
        int a = Long.compare((long) linearStart, (long) o.linearStart);
        if (a == 0) {
            int b = Long.compare((long) linearEnd, (long) o.linearEnd);
            if (b == 0) {
                return Integer.compare(System.identityHashCode(parent), System.identityHashCode(o.parent));
            } else return b;
        } else return a;
    }

}


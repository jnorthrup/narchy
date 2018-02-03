package jcog.pri;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable PriReference
 */
public class PLink<X> extends AbstractPLink<X> {

    public final X id;

    public PLink(X x, float p) {
        super(p);
        this.id = x;
    }

    @Nullable @Override
    public PLink<X> clonePri() {
        float p = pri;
        return (p==p) ? new PLink<>(id, p) : null;
    }
    @Override
    public boolean equals(Object that) {
        return (this == that) || id.equals(((AbstractPLink) that).get());
    }

    @Override
    public boolean isDeleted() {
        float p = pri;
        return p!=p; //fast NaN check
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    final public X get() {
        return id;
    }

}

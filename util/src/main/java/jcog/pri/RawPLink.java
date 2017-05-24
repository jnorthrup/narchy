package jcog.pri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Created by me on 2/17/17.
 */
public class RawPLink<X> extends Pri implements PLink<X> {

    @NotNull protected final X id;

    public RawPLink(@NotNull X x, float p) {
        super(p);
        this.id = x;
    }

    @Nullable @Override
    public RawPLink<X> clone() {
        float p = pri();
        return (p==p) ? new RawPLink<>(id, p) : null;
    }

    @Override
    public boolean equals(@NotNull Object that) {
        return ((this == that) || this.id.equals(that)) ||
                ((that instanceof Supplier) && id.equals(((Supplier) that).get()));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @NotNull
    final public X get() {
        return id;
    }

    @Override
    public String toString() {
        return id + "=" + pri();
    }

}

package jcog.memoize;

import jcog.data.map.CustomConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/** WARNING dont call get() directly; use apply().
 * allows storing null values
 * */
public class SoftMemoize<X, Y> extends CustomConcurrentHashMap<X, Object> implements Memoize<X, Y> {

    private final Function<X, Object> f;

    final private Object NULL = new Object();

    public SoftMemoize(@NotNull Function<X, Y> g, int expSize, boolean softOrWeak) {
        super(STRONG, EQUALS, softOrWeak ? SOFT : WEAK, EQUALS, expSize);
        this.f = (x) -> {
            Y y = g.apply(x);
            return y == null ? NULL : y;
        };
    }

    @Override
    public String summary() {
        return "size=" + super.size();
    }

    @Override
    public Y apply(X x) {
        Object y = computeIfAbsent(x, f);
        if (y == NULL)
            return null;
        else
            return (Y) y;
    }


}

package spacegraph.space2d.widget.chip;

import java.util.function.Function;

public class FunctionChip<X,Y> extends AbstractFunctionChip<X,Y> {

    private final Function<X, Y> f;

    public FunctionChip(Function<X,Y> f) {
        super();
        this.f = f;
    }

    @Override protected Function<X,Y> f() {
        return f;
    }

    public FunctionChip<X,Y> buffered() {
        return new FunctionChip<>(new Function<>() {
            X last = null;
            Y lastY = null;

            @Override
            public Y apply(X xx) {
                if (last == xx)
                    return lastY;
                return lastY = f.apply(last = xx);
            }
        });
    }
}

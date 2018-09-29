package spacegraph.space2d.widget.chip;

import java.util.function.BiFunction;

public class BiFunctionChip<X,Y,Z> extends AbstractBiFunctionChip<X,Y,Z> {

    final BiFunction<X,Y,Z> f;

    public BiFunctionChip(BiFunction<X, Y, Z> f) {
        this.f = f;
    }

    @Override
    protected BiFunction f() {
        return f;
    }
}

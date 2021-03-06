package spacegraph.space2d.container.grid;

import jcog.data.map.MRUMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;

import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface GridRenderer<X> {

    /** adapter for value-only usage */
    static <X> GridRenderer<X> value(Function<X, Surface> builder) {
        return new GridRenderer<X>() {
            @Override
            public Surface apply(int x, int y, X v) {
                return builder.apply(v);
            }
        };
    }

    static <X> GridRenderer<X> valueCached(Function<X, Surface> builder, int capacity) {
        return new GridRenderer<>() {

//                final HijackMemoize<X,Surface> cache = new HijackMemoize<X,Surface>((x->{
//                    throw new UnsupportedOperationException();
//                }), capacity, 3, true) {
//                    @Override
//                    protected void removed(PriProxy<X, Surface> value) {
//                        Surface s = value.get();
//                        assert(s!=null);
//                        s.stop();
//                    }
//                };

            final MRUMap<X, Surface> cache = new MRUMap<>(capacity) {
                @Override
                protected void onEvict(Map.Entry<X, Surface> entry) {
                    entry.getValue().stop();
                }
            };

            @Override
            public Surface apply(int x, int y, X value) {
                @Nullable Surface s = cache.computeIfAbsent(value, builder);
                //s.show();
                return s;
            }

            @Override
            public void hide(X key, Surface s) {
                //nothing
            }
        };
    }

    Surface apply(int x, int y, X value);


    default void hide(X key, Surface s) {
        s.stop();
    }
}

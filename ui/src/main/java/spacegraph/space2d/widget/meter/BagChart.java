package spacegraph.space2d.widget.meter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * TreeChart visualization of items in a collection
 */
@Deprecated public class BagChart<X> extends TreeChart<X> implements BiConsumer<X, TreeChart.ItemVis<X>> {

    private final AtomicBoolean busy = new AtomicBoolean(false);

    private final Iterable<? extends X> input;
    private final Function<X, TreeChart.ItemVis<X>> updater;

    public BagChart(Iterable<X> b) {
        super();
        this.input = b;
        this.updater = cached(this::newItem);
        update();
    }

    public void update() {
        if (busy.compareAndSet(false, true)) {
            try {
                update(input, this, updater);
            } finally {
                busy.set(false);
            }
        }
    }

    private ItemVis<X> newItem(X i) {
        return new ItemVis<>(i, label(i, 50));
    }

    






    protected String label( X i, int MAX_LEN) {
        String s = i.toString();
        if (s.length() > MAX_LEN)
            s = s.substring(0, MAX_LEN);
        return s;
    }

    @Override
    public void accept(X x, ItemVis<X> xItemVis) {

    }



















}

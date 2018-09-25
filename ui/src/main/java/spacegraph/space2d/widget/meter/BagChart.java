package spacegraph.space2d.widget.meter;

import spacegraph.space2d.container.Graph2D;
import spacegraph.space2d.container.TreeMap2D;

import java.util.function.Consumer;

/**
 * TreeChart visualization of items in a collection
 * TODO
 */
public class BagChart<X> extends Graph2D<X> {

    private final Iterable<X> input;

    /** decorator should also assign pri to each node vis */
    public BagChart(Iterable<X> b, Consumer<NodeVis<X>> decorator) {
        super();
        this.input = b;
        build(decorator);
        update(new TreeMap2D<>());
        update();
    }

    public void update() {
        set(input);
    }



    protected String label(X i, int MAX_LEN) {
        String s = i.toString();
        if (s.length() > MAX_LEN)
            s = s.substring(0, MAX_LEN);
        return s;
    }


}

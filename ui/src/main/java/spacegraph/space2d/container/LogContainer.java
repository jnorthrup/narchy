package spacegraph.space2d.container;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.text.AbstractLabel;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.function.Consumer;

public class LogContainer extends RingContainer<AbstractLabel> {

    public LogContainer(int len) {
        super(new AbstractLabel[len]);
    }

    @Override
    protected void reallocate(AbstractLabel[] x) {
        for (int i = 0; i < x.length; i++) {
            AbstractLabel r = new VectorLabel();
            r.pos(RectFloat.Unit);
            r.start(this);
            x[i] = r;
        }
    }

    public void append(String s) {
        next(new Consumer<AbstractLabel>() {
            @Override
            public void accept(AbstractLabel v) {
                v.text(s);
            }
        });
    }

}

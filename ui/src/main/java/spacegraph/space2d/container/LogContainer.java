package spacegraph.space2d.container;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.widget.text.VectorLabel;

public class LogContainer extends RingContainer<VectorLabel> {

    public LogContainer(int len) {
        super(new VectorLabel[len]);
    }

    @Override
    protected void reallocate(VectorLabel[] x) {
        for (int i = 0; i < x.length; i++) {
            VectorLabel r = new VectorLabel();
            r.pos(RectFloat.Unit);
            r.start(this);
            x[i] = r;
        }
    }

    public void append(String s) {
        next((v)->v.text(s));
    }

}

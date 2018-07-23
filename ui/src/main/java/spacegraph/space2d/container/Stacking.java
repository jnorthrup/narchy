package spacegraph.space2d.container;

import spacegraph.space2d.Surface;


public class Stacking extends MutableContainer {


    public Stacking(Surface... children) {
        super(children);
    }

    @Override
    protected void doLayout(int dtMS) {
        forEach(c -> c.pos(bounds));
    }

}

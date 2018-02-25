package spacegraph.container;

import spacegraph.Surface;

/**
 * TODO
 */
public class Stacking extends MutableContainer {


    public Stacking(Surface... children) {
        super(children);
//        clipTouchBounds = false;
    }

    @Override
    public void doLayout(int dtMS) {
        for (Surface c : children())
            c.pos(bounds);
    }



}

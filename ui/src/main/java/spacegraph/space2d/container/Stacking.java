package spacegraph.space2d.container;

import spacegraph.space2d.Surface;

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

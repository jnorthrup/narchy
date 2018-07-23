package spacegraph.space2d.widget.windo;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Stacking;

/**
 * a wall (virtual surface) contains zero or more windows;
 * anchor region for Windo's to populate
 * <p>
 * TODO move active window to top of child stack
 */
public class Wall extends Stacking {


    public Wall() {

        clipBounds = false;
    }


    @Override
    public void doLayout(int dtMS) {


        for (Surface c : children()) {
            c.fence(bounds);
            c.layout();
        }
    }


}





































































































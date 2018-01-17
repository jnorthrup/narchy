package spacegraph.layout;

import spacegraph.Surface;

/**
 * TODO
 */
public class Stacking extends MutableLayout {

    public Stacking(Surface... children) {
        super(children);
//        clipTouchBounds = false;
    }

    @Override
    public void doLayout(int dtMS) {
        children.forEach((c) -> c.pos(bounds));
    }

    public void add(Surface s) {
        if (children.add(s))
            layout();
    }
    public boolean remove(Surface s) {
        if (children.remove(s)) {
            layout();
            return true;
        }
        return false;
    }

}

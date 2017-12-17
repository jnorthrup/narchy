package spacegraph.layout;

import spacegraph.Surface;

/**
 * Splits a surface into a top and bottom vertical column
 */
public class VSplit<X extends Surface, Y extends Surface> extends MutableLayout {

    public float split; //0.5f = middle, 0.0 = all top, 1.0 = all bottom

    public VSplit() {
        this(null, null);
    }

    public VSplit(X top, Y bottom) {
        this(top, bottom, 0.5f);
    }

    public VSplit(X top, Y bottom, float split) {
        super(null,null);
        set(top, bottom, split);
    }


    public void set(X top, Y bottom, float split) {
        this.split = split;
        top(top);
        bottom(bottom);
        layout();
    }

    @Override
    public void doLayout() {

        float margin = 0.0f;
        //float content = 1f - margin;
        float x = margin / 2f;


        float X = x();
        float Y = y();
        float h = h();
        float w = w();
        float Ysplit = Y + split * h;

        Surface top = top();
        if (top!=null) {
            top.pos(X, Ysplit, X+w, Y+h);
        }

        Surface bottom = bottom();
        if (bottom != null) {
            bottom.pos(X,  Y, X+w, Ysplit);
        }

        super.doLayout();
    }

    public final void top(X s) {
        //s.start(this);
        children.set(0, s);
    }
    public final void bottom(Y s) {
        //s.start(this);
        children.set(1, s);
    }

    public final X top() {
        return (X) children.get(0);
    }
    public final Y bottom() {
        return (Y) children.get(1);
    }

}


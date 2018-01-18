package spacegraph.layout;

import jcog.Util;
import spacegraph.Surface;

/**
 * Splits a surface into a top and bottom vertical column
 */
public class VSplit<X extends Surface, Y extends Surface> extends MutableContainer {

    public float split; //0.5f = middle, 0.0 = all top, 1.0 = all bottom

    public VSplit() {
        super();
    }

    public VSplit(X top, Y bottom) {
        this(top, bottom, 0.5f);
    }

    public VSplit(X top, Y bottom, float split) {
        super(top,bottom);
        set(split);
    }


    public void set(float split) {
        float s = this.split;
        this.split = split;
        if (!Util.equals(s, split, 0.001f)) {
            layout();
        }
    }

    public void set(X top, Y bottom, float split) {
        children(top, bottom);
        set(split);
        layout();
    }

    @Override
    public void doLayout(int dtMS) {

        float margin = 0.0f;
        //float content = 1f - margin;
        float x = margin / 2f;


        float X = x();
        float Y = y();
        float h = h();
        float w = w();
        float Ysplit = Y + split * h;

        Surface top = above();
        if (top!=null) {
            top.pos(X, Ysplit, X+w, Y+h);
        }

        Surface bottom = below();
        if (bottom != null) {
            bottom.pos(X,  Y, X+w, Ysplit);
        }

        super.doLayout(dtMS);
    }

    public final void top(X s) {
        //s.start(this);
        children.set(0, s);
    }
    public final void bottom(Y s) {
        //s.start(this);
        children.set(1, s);
    }

    public final X above() {
        return (X) children.get(0);
    }
    public final Y below() {
        return (Y) children.get(1);
    }

}


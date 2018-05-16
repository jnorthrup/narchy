package spacegraph.space2d;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.hud.Ortho;

/** surface rendering context */
public class SurfaceRender {

    final static int STACK_CAPACITY = 4 * 256;

    /**
     * quads of floats containin gcoordinates relative to view of the screen
     * for use in determing LOD and visibility culling.
     *
     * 0 sx, 1 sy - proportional to viewable area
     * 2 sw, 3 sh - proportional to viewable area
     * */
    float[] stack = new float[STACK_CAPACITY];
    /** stack pointer */
    int sp = 0;


    /** viewable pixel resolution */
    public final int pw, ph;

    /** ms since last update */
    public final int dtMS;

    public SurfaceRender(int pw, int ph, int dtMS) {
        this.pw = pw;
        this.ph = ph;
        this.dtMS = dtMS;
    }

    public boolean push(Surface child) {
        RectFloat2D next = child.bounds;
        //compare bounds to next bounds
        float prevSW = sw();
        float prevSH = sh();
        float nextW = next.w;
        float nextH = next.h;
        float wRatio = nextW / (parentW());
        float hRatio = nextH / (parentH());

//        if (child instanceof Label)
//            System.out.println(sp + ":" + wRatio + " * " + prevSW + " " + sw());

        //System.out.println(child + " " + sw + " " + sh);
//        if (sw < 0.05f || sh < 0.05f)
//            return false;

        //TODO cull off-screen

        push(this.sx() + (wRatio - prevSW)/2, this.sy() + (hRatio - prevSH)/2, prevSW * wRatio, prevSH * hRatio);
        return true;
    }

    public void push(float sx, float sy) {
        stack[sp] = sx;
        stack[sp+1] = sy;
        sp += 2;
    }

    public void push(float sx, float sy, float sw, float sh) {
        stack[sp] = sx;
        stack[sp+1] = sy;
        stack[sp+2] = sw;
        stack[sp+3] = sh;
        sp += 4;
    }

    public void pop() {
        sp-=4;
    }

    float sx() {
        return stack[sp-4];
    }
    float sy() {
        return stack[sp-3];
    }
    public float sw() {
        return stack[sp-2];
    }
    float sh() {
        return stack[sp-1];
    }

    float parentW() {
        return stack[sp-2];
    }
    float parentH() {
        return stack[sp-1];
    }

    public void reset(Ortho o) {
        sp = 0;
        float sw = o.scale.x;
        float sh = o.scale.y;
        //prePush(o);
        push(sw, sh);
        push(o.cam.x, o.cam.y, pw, ph);
    }

    /** cache parent geometry for comparison with children */
    public void prePush(Surface parent) {
        RectFloat2D b = parent.bounds;
        push(b.w * stack[sp-6], b.h * stack[sp-5]);
    }
    public void prePop() {
        sp-=2;
    }

    /** min # of visible pixels in either W,H direction */
    public float visP() {
        return //Math.min(sw() * pw, sh() * ph);
                Math.min(sw(), sh());
    }
}

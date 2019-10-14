package spacegraph.space2d.container.graph;

import com.jogamp.opengl.GL2;
import jcog.Util;
import spacegraph.video.Draw;

public class EdgeVis<X> {
    volatile public NodeVis<X> to;

    public volatile boolean invalid;

    float r, g, b, a;
    public float weight;
    private volatile EdgeVisRenderer renderer;

    public EdgeVis() {
        clear();
    }

    public void clear() {
        invalid = true;
        r = g = b = 0f;
        a = 0.75f;
        to = null;
        weight = 1f;
        renderer = EdgeVisRenderer.Triangle;
    }



//        protected void merge(EdgeVis<X> x) {
//            weight += x.weight;
//            r = Util.or(r, x.r);
//            g = Util.or(g, x.g);
//            b = Util.or(b, x.b);
//            a = Util.or(a, x.a);
//        }

    enum EdgeVisRenderer {
        Line {
            @Override
            public void render(EdgeVis e, NodeVis from, GL2 gl) {
                float x = from.cx(), y = from.cy();
                gl.glLineWidth(1f + e.weight * 4f);
                e.color(gl);
                NodeVis to = e.to;
                Draw.line(x, y, to.cx(), to.cy(), gl);
            }
        },
        Triangle {
            @Override
            public void render(EdgeVis e, NodeVis from, GL2 gl) {

                NodeVis to = e.to;
                if (to == null)
                    return;


                float scale = Math.min(from.w(), from.h());
                float base = Util.lerpSafe(e.weight, scale / 2, scale);

                e.color(gl);
                float fx = from.cx(), fy = from.cy();
                float tx = to.cx(), ty = to.cy();
                Draw.halfTriEdge2D(fx, fy, tx, ty, base, gl);

            }
        };

        protected abstract void render(EdgeVis e, NodeVis from, GL2 gl);
    }

    private void color(GL2 gl) {
        gl.glColor4f(r, g, b, a);
    }


    public EdgeVis<X> weight(float w) {
        weight = w;
        return this;
    }

    public EdgeVis<X> weightAddLerp(float w, float rate) {
        this.weight = Util.lerpSafe(rate, this.weight, this.weight + w);
        return this;
    }
    public EdgeVis<X> weightLerp(float w, float rate) {
        this.weight = Util.lerpSafe(rate, this.weight, w);
        return this;
    }

    public EdgeVis<X> color(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }

    public EdgeVis<X> colorLerp(float r, float g, float b /* TODO type */, float rate) {
        if (r==r) this.r = Util.lerpSafe(rate, this.r, r);
        if (g==g) this.g = Util.lerpSafe(rate, this.g, g);
        if (b==b) this.b = Util.lerpSafe(rate, this.b, b);
        return this;
    }
    public EdgeVis<X> colorAdd(float r, float g, float b /* TODO type */, float rate) {
        if (r==r) this.r = Util.lerpSafe(rate, this.r, r + this.r);
        if (g==g) this.g = Util.lerpSafe(rate, this.g, g + this.g);
        if (b==b) this.b = Util.lerpSafe(rate, this.b, b + this.b);
        return this;
    }

    final void draw(NodeVis<X> from, GL2 gl) {

        NodeVis<X> t = this.to;
        if (t == null || !t.visible())
            return;

        renderer.render(this, from, gl);
    }
}

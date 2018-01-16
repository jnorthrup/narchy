package spacegraph;

import org.jetbrains.annotations.Nullable;
import spacegraph.layout.Layout;

import java.util.function.Consumer;

public class Scale extends Layout {

    protected float scale;

    public final Surface the;

    public Scale(Surface the, float s) {
        this.the = the;
        this.scale = s;
    }

    public Scale scale(float scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public int childrenCount() {
        return 1;
    }

    public float scale() {
        return scale;
    }

    @Override
    public synchronized void start(@Nullable Surface parent) {
        super.start(parent);
        the.start(this);
    }

    @Override
    public synchronized void stop() {
        the.stop();
        super.stop();
    }

    @Override
    protected void doLayout(int dtMS) {

        //        v2 scale = this.scale;
//
//        float sx, sy;

        float w = w();
        float vw = w * scale; //TODO factorin scale
        float h = h();
        float vh = h * scale;
        float marginAmt = (1f - scale) / 2;
        float tx = x() + w * marginAmt, ty = y() + h * marginAmt;

        the.pos(tx, ty, tx+vw, ty+vh);
    }


    @Override
    public void forEach(Consumer<Surface> o) {
        o.accept(the);
    }
}

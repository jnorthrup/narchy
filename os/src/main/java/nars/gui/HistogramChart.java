package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.util.math.Color3f;
import spacegraph.video.Draw;

import java.util.function.Supplier;

/**
 * Created by me on 9/2/16.
 */
public class HistogramChart extends PaintSurface {


    private final Supplier<float[]> data;
    private final Color3f dark;
    private final Color3f light;

    public HistogramChart(Supplier<float[]> source, Color3f dark, Color3f light) {

        this.data = source;
        this.dark = dark;
        this.light = light;

    }


    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        Draw.bounds(gl, this, this::paintUnit);
    }

    protected void paintUnit(GL2 gl) {

        gl.glColor4f(0f, 0f, 0f, 0.5f);
        Draw.rect(gl, 0, 0, 1, 1);

        var data = this.data.get();

        var N = data.length;
        var max = data[Util.argmax(data)];
        if (max == 0)
            return;

        float x = 0;

        var ra = dark.x;
        var ga = dark.y;
        var ba = dark.z;
        var rb = light.x;
        var gb = light.y;
        var bb = light.z;

        var dx = 1f / N;
        for (var i = 0; i < N; i++) {

            var v = data[i] / max;

            gl.glColor3f(Util.lerpSafe(v, ra, rb), Util.lerpSafe(v, ga, gb), Util.lerpSafe(v, ba, bb));

            Draw.rect(x, 0, dx, v, gl);

            x += dx;
        }

    }

}


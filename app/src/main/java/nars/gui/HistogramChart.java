package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.render.Draw;

import java.util.function.Supplier;

/**
 * Created by me on 9/2/16.
 */
public class HistogramChart extends Surface {


    private final Supplier<float[]> data;
    private final Color3f dark, light;

    public HistogramChart(Supplier<float[]> source, Color3f dark, Color3f light) {

        this.data = source;
        this.dark = dark;
        this.light = light;

    }

//    public HistogramChart(NAR nar, FloatFunction<PLink<Concept>> meter, int bins, Color3f dark, Color3f light) {
//
//        float[] data = new float[bins];
//        this.data = () -> data;
//        this.dark = dark;
//        this.light = light;
//
//        nar.onCycle(nn -> {
//            nn.conceptsActive().forEach(c -> {
//                float p = meter.floatValueOf(c);
//                int b = Util.bin(p, bins - 1);
//                data[b]++;
//            });
//
//            float total = 0;
//            for (float e : data) {
//                total += e;
//            }
//            if (total > 0) {
//                for (int i = 0; i < bins; i++)
//                    data[i] /= total;
//            }
//
//                    //priHistogram(data);
//        });
//
//    }

    @Override
    protected void paint(GL2 gl, int dtMS) {
        Draw.bounds(gl, this, this::paintUnit);
    }

    protected void paintUnit(GL2 gl) {

        gl.glColor4f(0f, 0f, 0f, 0.5f);
        Draw.rect(gl, 0, 0, 1, 1);

        float[] data = this.data.get();

        int N = data.length;
        float max = data[Util.argmax(data)];
        if (max == 0)
            return; //empty

        float x = 0;

        float ra = dark.x;
        float ga = dark.y;
        float ba = dark.z;
        float rb = light.x;
        float gb = light.y;
        float bb = light.z;

        float dx = 1f / N;
        for (int i = 0; i < N; i++) {

            float v = data[i] / max;

            gl.glColor3f(Util.lerp(v, ra, rb), Util.lerp(v, ga, gb), Util.lerp(v, ba, bb));

            Draw.rect(gl, x, 0, dx, v);

            x += dx;
        }

    }
}


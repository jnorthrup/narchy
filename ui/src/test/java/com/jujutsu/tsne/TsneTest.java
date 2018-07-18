package com.jujutsu.tsne;

import com.jogamp.opengl.GL2;
import com.jujutsu.tsne.matrix.MatrixOps;
import jcog.Util;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.video.Draw;

public class TsneTest {

    @Disabled
    @Test
    public void test1() {
























        int DIM = 4;
        int N = 128;
        double[][] data = new double[N][DIM];
        int j = 0;
        for (int i = 0; i < N/2; i++) {
            data[j++] = new double[] { 0, 0, 1 + Math.random()/2f, 1 + Math.random()/2f };
        }
        for (int i = 0; i < N/2; i++) {
            data[j++] = new double[] { 1, 0, -1 + Math.random()/2f, -1 + Math.random()/2f };
        }


        SimpleTSne t = new SimpleTSne() {
            Surface plot = new Surface() {

                @Override
                protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                    Draw.bounds(bounds, gl, this::paint);
                }

                protected void paint(GL2 gl) {
                    double[][] vv = Y;
                    if (vv == null)
                        return;
                    vv = vv.clone();




                    float scale = 0.1f;
                    float w, h;
                    w = h = 1f/vv.length;
                    for (int i = 0, yLength = vv.length; i < yLength; i++) {
                        double[] v = vv[i];

                        float x = (float) (v[0]);
                        float y = (float) (((v.length > 1) ? v[1] : 0));

                        x *= scale;
                        y *= scale;

                        Draw.colorHash(gl, i, 0.75f);
                        Draw.rect(gl, x, y, w, h);
                    }
                }
            };
            {
                SpaceGraph.window(plot, 800, 800);
            }

            @Override
            protected void next(int iter) {

                super.next(iter);

                Util.sleep(50);
            }
        };

        double[][] y = t.tsne(new TSneConfig(
                data, 2, -1, 5f,
                1000,
                false, 0.5,false, true
        ));
        System.out.println(MatrixOps.doubleArrayToPrintString(y));


    }
}

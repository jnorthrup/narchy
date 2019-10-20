package jcog.test.control;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.MutableEnum;
import jcog.signal.wave2d.ArrayBitmap2D;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static jcog.Util.unitize;

/* 1D and 2D grid tracking */
public class TrackXY  {

    public final ArrayBitmap2D grid;

    public final int W;
    public final int H;

    /** target coordinates: to be observed by the experiment */
    public volatile float tx;
    public volatile float ty;

    /** current coordinates: to be moved by the experiment */
    public volatile float cx;
    public volatile float cy;

    public final FloatRange controlSpeed = new FloatRange(0.05f, 0, 0.25f);

    public final FloatRange targetSpeed = new FloatRange(0.01f, 0, 0.25f);

    public final FloatRange visionContrast = new FloatRange(4f, 0, 4f);

    public final MutableEnum<TrackXYMode> mode = new MutableEnum<>(TrackXYMode.CircleTarget);


    public TrackXY(int W, int H) {
        this.grid = new ArrayBitmap2D(this.W = W, this.H = H);
    }

    public static Random random() {
        return ThreadLocalRandom.current();
    }

    public void randomize() {
        this.tx = random().nextInt(grid.width());
        //this.sx = random().nextInt(view.width());
        if (grid.height() > 1) {
            this.ty = random().nextInt(grid.height());
            //this.sy = random().nextInt(view.height());
        } else {
            this.ty = this.cy = 0;
        }
    }

    public void act() {


        mode.get().accept(this);

        grid.set((x, y) -> {


            var distOther = (float) (Util.sqr((tx - x)/((double)W)) + Util.sqr((ty - y)/((double)H)));
            //return distOther > visionContrast.floatValue() ? 0 : 1;
            //float distSelf = (float) Math.sqrt(Util.sqr(sx - x) + Util.sqr(sy - y));
            return unitize(1 - distOther * Math.max(W,H) * visionContrast.floatValue());
//                return Util.unitize(
//                        Math.max(1 - distOther * visionContrast,
//                                1 - distSelf * visionContrast
//                        ));


        });
    }

    public float distMax() {
        return (float) Math.sqrt(Util.sqr(W-1f) + Util.sqr(H-1f));
    }

    /** linear distance from current to target */
    public float dist() {
        return (float) Math.sqrt(Util.sqr(Math.max(0.5f,Math.abs(tx - cx))-0.5f) + Util.sqr(Math.max(0.5f,Math.abs(ty - cy))-0.5f));
    }

    public void control(float dcx, float dcy) {
        cx = Util.clamp(cx + dcx, 0, W-1);
        cy = Util.clamp(cy + dcy, 0, H-1);
    }


    public enum TrackXYMode implements Consumer<TrackXY> {

        FixedCenter {
            float x;

            @Override
            public void accept(TrackXY t) {
                x += t.targetSpeed.floatValue();
                t.tx = t.W / 2f;
                t.ty = t.H / 2f;
            }
        },

        RandomTarget {
            @Override
            public void accept(TrackXY t) {
                var targetSpeed = t.targetSpeed.floatValue();
                float ty;
                var tx = Util.clamp(t.tx + 2 * targetSpeed * (TrackXY.random().nextFloat() - 0.5f), 0, t.W - 1);
                if (t.H > 1) {
                    ty = Util.clamp(t.ty + 2 * targetSpeed * (TrackXY.random().nextFloat() - 0.5f), 0, t.H - 1);
                } else {
                    ty = 0;
                }

                t.tx = tx;
                t.ty = ty;
            }
        },

        CircleTarget {
            float theta;

            @Override
            public void accept(TrackXY t) {
                theta += t.targetSpeed.floatValue();

                t.tx = (((float) Math.cos(theta) * 0.5f) + 0.5f) * (t.W - 1);

                if (t.H > 1)
                    t.ty = (((float) Math.sin(theta) * 0.5f) + 0.5f) * (t.H - 1);
                else
                    t.ty = 0;

            }
        }

    }


}


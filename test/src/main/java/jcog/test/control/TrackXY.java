package jcog.test.control;

import jcog.Util;
import jcog.func.IntIntToFloatFunction;
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

    public final FloatRange controlSpeed = new FloatRange(0.05f, (float) 0, 0.25f);

    public final FloatRange targetSpeed = new FloatRange(0.01f, (float) 0, 0.25f);

    public final FloatRange visionContrast = new FloatRange(4f, (float) 0, 4f);

    public final MutableEnum<TrackXYMode> mode = new MutableEnum<>(TrackXYMode.CircleTarget);


    public TrackXY(int W, int H) {
        this.grid = new ArrayBitmap2D(this.W = W, this.H = H);
    }

    public static Random random() {
        return ThreadLocalRandom.current();
    }

    public void randomize() {
        this.tx = (float) random().nextInt(grid.width());
        //this.sx = random().nextInt(view.width());
        if (grid.height() > 1) {
            this.ty = (float) random().nextInt(grid.height());
            //this.sy = random().nextInt(view.height());
        } else {
            this.ty = this.cy = (float) 0;
        }
    }

    public void act() {


        mode.get().accept(this);

        grid.set(new IntIntToFloatFunction() {
            @Override
            public float value(int x, int y) {


                float distOther = (float) (Util.sqr((double) (tx - (float) x) / ((double) W)) + Util.sqr((double) (ty - (float) y) / ((double) H)));
                //return distOther > visionContrast.floatValue() ? 0 : 1;
                //float distSelf = (float) Math.sqrt(Util.sqr(sx - x) + Util.sqr(sy - y));
                return unitize(1.0F - distOther * (float) Math.max(W, H) * visionContrast.floatValue());
//                return Util.unitize(
//                        Math.max(1 - distOther * visionContrast,
//                                1 - distSelf * visionContrast
//                        ));


            }
        });
    }

    public float distMax() {
        return (float) Math.sqrt((double) (Util.sqr((float) W - 1f) + Util.sqr((float) H - 1f)));
    }

    /** linear distance from current to target */
    public float dist() {
        return (float) Math.sqrt((double) (Util.sqr(Math.max(0.5f, Math.abs(tx - cx)) - 0.5f) + Util.sqr(Math.max(0.5f, Math.abs(ty - cy)) - 0.5f)));
    }

    public void control(float dcx, float dcy) {
        cx = Util.clamp(cx + dcx, (float) 0, (float) (W - 1));
        cy = Util.clamp(cy + dcy, (float) 0, (float) (H - 1));
    }


    public enum TrackXYMode implements Consumer<TrackXY> {

        FixedCenter {
            float x;

            @Override
            public void accept(TrackXY t) {
                x += t.targetSpeed.floatValue();
                t.tx = (float) t.W / 2f;
                t.ty = (float) t.H / 2f;
            }
        },

        RandomTarget {
            @Override
            public void accept(TrackXY t) {
                float targetSpeed = t.targetSpeed.floatValue();
                float ty;
                float tx = Util.clamp(t.tx + 2.0F * targetSpeed * (TrackXY.random().nextFloat() - 0.5f), (float) 0, (float) (t.W - 1));
                if (t.H > 1) {
                    ty = Util.clamp(t.ty + 2.0F * targetSpeed * (TrackXY.random().nextFloat() - 0.5f), (float) 0, (float) (t.H - 1));
                } else {
                    ty = (float) 0;
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

                t.tx = (((float) Math.cos((double) theta) * 0.5f) + 0.5f) * (float) (t.W - 1);

                if (t.H > 1)
                    t.ty = (((float) Math.sin((double) theta) * 0.5f) + 0.5f) * (float) (t.H - 1);
                else
                    t.ty = (float) 0;

            }
        }

    }


}


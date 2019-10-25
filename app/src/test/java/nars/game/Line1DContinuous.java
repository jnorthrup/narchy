package nars.game;

import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.game.action.ActionSignal;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.Arrays;

import static java.lang.System.out;


/**
 * Created by me on 5/4/16.
 */
public class Line1DContinuous extends Game {

    static {

    }

    @FunctionalInterface
    public interface IntToFloatFunction {
        float valueOf(int i);
    }

    private final IntToFloatFunction targetFunc;
    int size;
    public boolean print;
    private float yHidden;
    private float yEst;
    float speed = 5f;
    final float[] ins;

    public Line1DContinuous(NAR n, int size, IntToFloatFunction target) {
        super("x", GameTime.durs(1));
        this.size = size;
        ins = new float[size * 2];
        this.targetFunc = target;

        yEst = size / 2f;
        yHidden = size / 2f;


        for (int i = 0; i < size; i++) {
            int ii = i;
            sense($.INSTANCE.func("h", Atomic.atom("x"), $.INSTANCE.the(i)), new FloatSupplier() {
                @Override
                public float asFloat() {
                    return ins[ii];
                }
            });
            sense($.INSTANCE.func("e", Atomic.atom("x"), $.INSTANCE.the(i)), new FloatSupplier() {
                @Override
                public float asFloat() {
                    return ins[size + ii];
                }
            });
        }

        ActionSignal a;

        actionBipolar($.INSTANCE.inh(Atomic.the("move"), Atomic.the("x")), new FloatToFloatFunction() {
            @Override
            public float valueOf(float v) {

                yEst += (v) * speed;

                return yEst;
            }
        });

        reward(new FloatSupplier() {
            @Override
            public float asFloat() {
                yHidden = Math.round(targetFunc.valueOf((int) nar.time()) * (size - 1));

                yHidden = Math.min(size - 1, Math.max(0, yHidden));
                yEst = Math.min(size - 1, Math.max(0, yEst));


                Arrays.fill(ins, 0f);
                float smoothing = 1 / 2f;
                for (int i = 0; i < size; i++) {
                    ins[i] = Math.abs(yHidden - i) / (size * smoothing);
                    ins[i + Line1DContinuous.this.size] = Math.abs(yEst - i) / (size * smoothing);
                }


                float dist = Math.abs(yHidden - yEst) / Line1DContinuous.this.size;


                float reward =
                        -dist * 2f + 1f;


                if (yEst > Line1DContinuous.this.size - 1) yEst = Line1DContinuous.this.size - 1;
                if (yEst < 0) yEst = 0;


                if (print) {


                    int colActual = Math.round(yHidden);
                    int colEst = Math.round(yEst);
                    for (int i = 0; i < Line1DContinuous.this.size; i++) {

                        char c;
                        if (i == colActual && i == colEst) {
                            c = '@';
                        } else if (i == colActual)
                            c = 'X';
                        else if (i == colEst)
                            c = '+';
                        else
                            c = '.';

                        out.print(c);
                    }


                    out.print(' ');
                    out.print(Line1DContinuous.this.summary());
                    out.println();
                }

                return reward;

            }
        });
    }


    public static IntToFloatFunction sine(float targetPeriod) {
        return new IntToFloatFunction() {
            @Override
            public float valueOf(int t) {
                return 0.5f + 0.5f * (float) Math.sin(t / (targetPeriod));
            }
        };
    }

    public static IntToFloatFunction random(float targetPeriod) {
        return new IntToFloatFunction() {
            @Override
            public float valueOf(int t) {
                return (((((int) (t / targetPeriod)) * 31) ^ 37) % 256) / 256.0f;
            }
        };
    }

    public static void main(String[] args) {

        NAR nar = new NARS().get();
        nar.termVolMax.set(32);


        nar.beliefConfDefault.set(0.9f);
        nar.goalConfDefault.set(0.9f);


        Line1DContinuous l = new Line1DContinuous(nar, 6,
                random(16)
        );

        l.print = true;


    }


}

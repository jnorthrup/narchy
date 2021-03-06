package nars.experiment;

import java4k.gradius4k.Gradius4K;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.ScaledBitmap2D;
import jcog.util.FloatConsumer;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.game.Game;
import nars.game.Reward;
import nars.game.action.GoalActionConcept;
import nars.game.sensor.DigitizedScalar;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static java4k.gradius4k.Gradius4K.*;
import static nars.$.*;
import static nars.game.GameTime.fps;

/**
 * Created by me on 4/30/17.
 */
public class Gradius extends GameX {

    private final Gradius4K g = new Gradius4K();
    {
        g.paused = false;
    }

    int lastScore;

    public static void main(String[] args) {
        GameX.Companion.initFn(40f, new Function<NAR, Game>() {
            @Override
            public Game apply(NAR nar) {
                Gradius g = new Gradius(nar);
                nar.add(g);
//            g.what().onTask(t -> {
//                if (t instanceof DerivedTask && t.isGoal())
//                    nar.proofPrint(t);
//            });
                return g;
            }
        });
    }

    public Gradius(NAR nar) {
        super(INSTANCE.$$("g"), fps(25.0F), nar);

        //TODO coordinate with fps
        g.updateMS =
                //50; //20fps
                //30;
                //25; //40fps
                20L; //50fps
                //10; //100fps
                //5; //200fps
        //10;

        int dx = 2, dy = 2;
        int px = 24, py = 24;
//        int dx = 4, dy = 4;
//        int px = 12, py = 12;


        assert px % dx == 0 && py % dy == 0;

//        {
//            PixelBag retina = new PixelBag(new MonoBufImgBitmap2D(() -> g.image), px, py) {
//                @Override
//                protected float missing() {
//                    return 0;
//                }
//            };
////            retina.addActions(id,this, false, false, true);
////            onFrame(()->{
////               retina.setXRelative(g.player[OBJ_X] / g.getWidth());
////                retina.setYRelative(g.player[OBJ_Y] / g.getHeight());
////            });
//            Bitmap2DSensor sensor = new Bitmap2DSensor(id, new BrightnessNormalize(retina), nar);
//            addCamera(sensor);
//            retina.addActions(id, this);
//            window(new VectorSensorView(sensor, this).withControls(), 400, 400);
//        }

        {

            List<Bitmap2DSensor> cams = new FasterList();
            //AttNode camGroup = addGroup(cams)


            Atomic big = Atomic.atom("C");
            Atomic small = Atomic.atom("c");
            for (int i = 0; i < dx; i++)
                for (int j = 0; j < dy; j++) {
                    int ii = i;
                    int jj = j;
                    Term Pbig = //$.p(big, $.p($.the(ii), $.the(jj)));
                        $.INSTANCE.p(Atomic.atom("c" + ii), Atomic.atom("c" + jj));

                    //Term subSection = $.p(id, $.the(ii), $.the(jj));
                    Bitmap2DSensor c = senseCamera(new IntIntToObjectFunction<Term>() {
                                                       @Override
                                                       public Term apply(int x, int y) {
                                                           return $.INSTANCE.inh(id, $.INSTANCE.p(Pbig, $.INSTANCE.the(x), $.INSTANCE.the(y)));
                                                       }
                                                   },
                            //$.p(
                            //$.inh(
//                                        $.p(x, y),
//                                        subSection
                            new ScaledBitmap2D(new Supplier<BufferedImage>() {
                                @Override
                                public BufferedImage get() {
                                    return g.image;
                                }
                            }, px, py)
                                    .crop(
                                            (float) i / (float) dx, (float) j / (float) dy,
                                            (float) (i + 1) / (float) dx, (float) (j + 1) / (float) dy))
                            .resolution(0.02f);

                    cams.add(c);
                }

//            SpaceGraph.window(new Gridding(
//                            cams.stream().map(c -> new VectorSensorView(c, this).withControls()).collect(toList())),
//                    400, 900);
        }



        float width = (float) g.getWidth();
        float height = (float) g.getHeight();
        int gpsDigits = 3;
        float gpsRes = 0.05f;
        senseNumber(gpsDigits, DigitizedScalar.FuzzyNeedle, new FloatSupplier() {
                    @Override
                    public float asFloat() {
                        return g.player[OBJ_Y] / height;
                    }
                }, new IntFunction<Term>() {
                    @Override
                    public Term apply(int level) {
                        return $.INSTANCE.p($.INSTANCE.the(level), $.INSTANCE.p(id, $.INSTANCE.the("y")));
                    }
                }
        ).resolution(gpsRes);
        senseNumber(gpsDigits, DigitizedScalar.FuzzyNeedle, new FloatSupplier() {
                    @Override
                    public float asFloat() {
                        return g.player[OBJ_X] / width;
                    }
                }, new IntFunction<Term>() {
                    @Override
                    public Term apply(int level) {
                        return $.INSTANCE.p($.INSTANCE.the(level), $.INSTANCE.p(id, $.INSTANCE.the("x")));
                    }
                }
        ).resolution(gpsRes);


        actionUnipolar($.INSTANCE.inh(id, INSTANCE.$$("speed")), new FloatConsumer() {
            @Override
            public void accept(float s) {
                g.SPEED = Util.lerp(s, 0.1f, 4.0F);
            }
        }).resolution(0.1f);


        actionPushButton($.INSTANCE.inh(id, INSTANCE.$$("fire")), new BooleanProcedure() {
            @Override
            public void value(boolean b) {
                g.keys[VK_SHOOT] = b;
            }
        });

        //        if (canPause) {
//            actionToggle($$("pause"),
//                    b -> g.paused = b);
//        }


        initToggle();
        //initBipolar();

        Reward alive = reward("alive", 1.0F, new FloatSupplier() {
            @Override
            public float asFloat() {
                if (g.paused) return Float.NaN;

                if (g.playerDead > 1)
                    return (float) 0;
                else
                    return (float) +1;
            }
        });
        //alive.setDefault($.t(1, nar.beliefConfDefault.floatValue()));

        reward(rewardTerm("destroy"), new FloatSupplier() {
            @Override
            public float asFloat() {

                if (g.paused) return Float.NaN;

                int nextScore = g.score;

                float r = (float) (nextScore - lastScore);

                lastScore = nextScore;

                return Util.unitize(r);
                //return r!=0 ? Util.unitize(r) : Float.NaN;
            }
        }).conf(nar.goalConfDefault.floatValue()*0.75f);
       //destroy.setDefault($.t(0, nar.beliefConfDefault.floatValue()));


    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        if (g!=null)
            g.paused = false;
    }

    @Override
    protected void stopping(NAR nar) {
        g.paused = true;
        super.stopping(nar);
    }

    void initToggle() {
        //TODO boundary feedback
        Atomic X = $.INSTANCE.the("x");
        Atomic Y = $.INSTANCE.the("y");
        Term left =  $.INSTANCE.inh(id, $.INSTANCE.p(X, NEG));
        Term right =  $.INSTANCE.inh(id, $.INSTANCE.p(X, POS));
        Term down =  $.INSTANCE.inh(id, $.INSTANCE.p(Y, NEG));
        Term up =  $.INSTANCE.inh(id, $.INSTANCE.p(Y, POS));
        GoalActionConcept[] lr = actionPushButtonMutex(left, right,
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return g.keys[VK_LEFT] = b;
                    }
                },
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return g.keys[VK_RIGHT] = b;
                    }
                });
        GoalActionConcept[] ud = actionPushButtonMutex(down, up,
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return g.keys[VK_DOWN] = b;
                    }
                },
                new BooleanPredicate() {
                    @Override
                    public boolean accept(boolean b) {
                        return g.keys[VK_UP] = b;
                    }
                });

//        for (GoalActionConcept x : ArrayUtil.addAll(lr, ud))
//            x.goalDefault($.t(0, 0.001f), nar); //bias
    }

    void initBipolar() {

        float thresh = 0.1f;
        actionBipolar($.INSTANCE.inh(id, $.INSTANCE.p(INSTANCE.$$("y"), $.INSTANCE.varQuery(1))), new FloatToFloatFunction() {
            @Override
            public float valueOf(float dy) {
                if (dy < -thresh) {
                    g.keys[VK_UP] = false;
                    g.keys[VK_DOWN] = true;
                    return -1f;
                } else if (dy > +thresh) {
                    g.keys[VK_UP] = true;
                    g.keys[VK_DOWN] = false;
                    return 1f;
                } else {
                    g.keys[VK_UP] = false;
                    g.keys[VK_DOWN] = false;
                    return (float) 0;
                }

            }
        });
        actionBipolar($.INSTANCE.inh(id, $.INSTANCE.p(INSTANCE.$$("x"), $.INSTANCE.varQuery(1))), new FloatToFloatFunction() {
            @Override
            public float valueOf(float dx) {
                if (dx < -thresh) {
                    g.keys[VK_LEFT] = false;
                    g.keys[VK_RIGHT] = true;
                    return -1f;
                } else if (dx > +thresh) {
                    g.keys[VK_LEFT] = true;
                    g.keys[VK_RIGHT] = false;
                    return 1f;
                } else {
                    g.keys[VK_LEFT] = false;
                    g.keys[VK_RIGHT] = false;
                    return 0f;
                }

            }
        });
    }


}

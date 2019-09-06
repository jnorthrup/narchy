package nars.experiment;

import java4k.gradius4k.Gradius4K;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.agent.Reward;
import nars.concept.action.GoalActionConcept;
import nars.concept.sensor.DigitizedScalar;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import java.util.List;

import static java4k.gradius4k.Gradius4K.*;
import static nars.$.$$;
import static nars.agent.GameTime.fps;

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
        GameX.runRT(Gradius::new, 40f);
    }

    public Gradius(NAR nar) {
        super("g", fps(25), nar);

        //TODO coordinate with fps
        g.updateMS =
                //50; //20fps
                //30;
                //25; //40fps
                20; //50fps
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
                    Term Pbig = $.p(big, $.p($.the(ii), $.the(jj)));
                    //Term subSection = $.p(id, $.the(ii), $.the(jj));
                    Bitmap2DSensor c = senseCamera((x, y) ->
                                    $.inh(
                                        id,
                                        $.p(Pbig, $.p(small, $.p($.the(x), $.the(y))))
                                    ),
                            //$.p(
                            //$.inh(
//                                        $.p(x, y),
//                                        subSection
//                                ),
                            new ScaledBitmap2D(() -> g.image, px, py)
                                    .crop(
                                            (float) i / dx, (float) j / dy,
                                            (float) (i + 1) / dx, (float) (j + 1) / dy))
                            .resolution(0.02f);

                    cams.add(c);
                }

//            SpaceGraph.window(new Gridding(
//                            cams.stream().map(c -> new VectorSensorView(c, this).withControls()).collect(toList())),
//                    400, 900);
        }


        float width = g.getWidth();
        float height = g.getHeight();
        int gpsDigits = 3;
        float gpsRes = 0.05f;
        senseNumber(level -> $.p($.the(level), $.p(id, $.the("y"))),
                () -> g.player[OBJ_Y] / height,
                gpsDigits, DigitizedScalar.FuzzyNeedle
        ).resolution(gpsRes);
        senseNumber(level -> $.p($.the(level), $.p(id, $.the("x"))),
                () -> g.player[OBJ_X] / width,
                gpsDigits, DigitizedScalar.FuzzyNeedle
        ).resolution(gpsRes);


        actionUnipolar($.inh(id,$$("speed")), (s)->{
           g.SPEED = Util.lerp(s, 0.1f, 4);
        });


        actionPushButton($.inh(id,$$("fire")), (BooleanProcedure) b -> g.keys[VK_SHOOT] = b);

        //        if (canPause) {
//            actionToggle($$("pause"),
//                    b -> g.paused = b);
//        }


        initToggle();
        //initBipolar();

        Reward alive = rewardNormalized("alive",  -1, +1, ()->{
            if (g.paused) return Float.NaN;

            if (g.playerDead > 1)
                return -1f;
            else
                return Float.NaN; //return +1;
        });
        //alive.setDefault($.t(1, nar.beliefConfDefault.floatValue()));

        Reward destroy = rewardNormalized("destroy", 0.75f,0, 1, ()->{

            if (g.paused) return Float.NaN;

            int nextScore = g.score;

            float r = nextScore - lastScore;

            lastScore = nextScore;

            //return Util.unitize(r);
            return r!=0 ? Util.unitize(r) : Float.NaN;
       });
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
        Atomic X = $.the("x");
        Atomic Y = $.the("y");
        Term left =  $.inh(id, $.p(X, NEG));
        Term right =  $.inh(id, $.p(X, POS));
        Term down =  $.inh(id, $.p(Y, NEG));
        Term up =  $.inh(id, $.p(Y, POS));
        GoalActionConcept[] lr = actionPushButtonMutex(left, right,
                (BooleanPredicate) b -> g.keys[VK_LEFT] = b,
                (BooleanPredicate) b -> g.keys[VK_RIGHT] = b);
        GoalActionConcept[] ud = actionPushButtonMutex(down, up,
                (BooleanPredicate) b -> g.keys[VK_DOWN] = b,
                (BooleanPredicate) b -> g.keys[VK_UP] = b);

//        for (GoalActionConcept x : ArrayUtil.addAll(lr, ud))
//            x.goalDefault($.t(0, 0.001f), nar); //bias
    }

    void initBipolar() {

        float thresh = 0.1f;
        actionBipolar($.inh(id, $.p($$("y"), $.varQuery(1))), dy -> {
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
                return 0;
            }

        });
        actionBipolar($.inh(id, $.p($$("x"), $.varQuery(1))), dx -> {
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

        });
    }


}

package nars.experiment;

import java4k.gradius4k.Gradius4K;
import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.scalar.DigitizedScalar;
import nars.term.atom.Atomic;
import nars.video.Scale;

import static java4k.gradius4k.Gradius4K.*;
import static nars.$.$$;
import static nars.$.p;

/**
 * Created by me on 4/30/17.
 */
public class Gradius extends NAgentX {


    private final Gradius4K g;
    int lastScore;

    public Gradius(NAR nar) {

        super("g", nar);

        this.g = new Gradius4K();

        g.updateMS = 20;


        int dx = 3, dy = 2;
        int px = 12, py = 12;

        assert(px%dx==0 && py%dy ==0);
        for (int i = 0; i < dx; i++)
            for (int j = 0; j < dy; j++) {
                int ii = i;
                int jj = j;
                //Term subSection = $.p(id, $.the(ii), $.the(jj));
                senseCamera((x, y) ->
                        $.func((Atomic)id, $.p($.the(ii), $.the(jj)), $.p($.the(x), $.the(y)) ),
                                //$.p(
                                //$.inh(
//                                        $.p(x, y),
//                                        subSection
//                                ),
                        new Scale(() -> g.image, px, py)
                                .window(
                                        (((float)i) / dx), (((float)j) / dy),
                                        ((float)(i + 1)) / dx, ((float)(j + 1)) / dy))
                        .resolution(0.02f);
            }


        float width = g.getWidth();
        float height = g.getHeight();
        senseNumber((level) -> (p($.the("Y"), $.the(level))),
                () -> g.player[OBJ_Y] / height,
                2, DigitizedScalar.FuzzyNeedle
        ).resolution(0.02f);
        senseNumber((level) -> (p($.the("X"), $.the(level))),
                () -> g.player[OBJ_X] / width,
                2, DigitizedScalar.FuzzyNeedle
        ).resolution(0.02f);


        actionToggle($.inh("fire", id),
                (b) -> g.keys[VK_SHOOT] = b);

//        actionToggle($.inh("pause", id),
//                (b) -> g.paused = b);

        actionUnipolar($.prop(nar.self(), $.the("deep")), (d)->{
            //deep incrases both duration and max term volume
            nar.time.dur(Util.lerp(d*d, 20, 120));
            nar.termVolumeMax.set(Util.lerp(d, 24, 50));
            return d;
        });

        actionUnipolar($.prop(nar.self(), $.the("awake")), (a)->{
            nar.activateConceptRate.set(Util.lerp(a, 0.4f, 1f));
            return a;
        });
        actionUnipolar($.prop(nar.self(), $.the("focus")), (a)->{
            nar.forgetRate.set(Util.lerp(a, 0.9f, 0.4f)); //inverse forget rate
            return a;
        });

        initToggle();


    }

    public static void main(String[] args) {

        NAgentX.runRT(Gradius::new, 20f);

    }

    void initToggle() {
        actionPushButtonMutex($$("left"), $$("right"),
                (b) -> g.keys[VK_LEFT] = b,
                (b) -> g.keys[VK_RIGHT] = b);
        actionPushButtonMutex($$("up"), $$("down"),
                (b) -> g.keys[VK_UP] = b,
                (b) -> g.keys[VK_DOWN] = b);
    }

    void initBipolar() {

        float thresh = 0.1f;
        actionBipolar(p($.the("y"), id), (dy) -> {
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
        actionBipolar(p($.the("x"), id), (dx) -> {
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

    @Override
    protected float act() {

        if (g.paused)
            return 0;

        int nextScore = g.score;

        float r = (nextScore - lastScore);


        lastScore = nextScore;

        if (g.playerDead > 1)
            return -1f;
        else
            return r;
    }

}

package nars.experiment;

import java4k.gradius4k.Gradius4K;
import jcog.data.list.FasterList;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.attention.AttnDistributor;
import nars.concept.sensor.DigitizedScalar;
import nars.sensor.Bitmap2DSensor;
import nars.video.VectorSensorView;
import spacegraph.space2d.container.grid.Gridding;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java4k.gradius4k.Gradius4K.*;
import static nars.agent.FrameTrigger.fps;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 4/30/17.
 */
public class Gradius extends NAgentX {


    private final Gradius4K g = new Gradius4K();

    private final boolean canPause = false;


    int lastScore;

    public static void main(String[] args) {

        NAgentX.runRT(Gradius::new, 25f);

    }

    public Gradius(NAR nar) {
        super("g", fps(25), nar);


        //TODO coordinate with fps
        g.updateMS =
                //50; //25fps
                25; //50fps
        //10;


        int dx = 2, dy = 2;
        int px = 24, py = 24;

        assert px % dx == 0 && py % dy == 0;

        List<Bitmap2DSensor> cams = new FasterList();
        for (int i = 0; i < dx; i++)
            for (int j = 0; j < dy; j++) {
                int ii = i;
                int jj = j;
                //Term subSection = $.p(id, $.the(ii), $.the(jj));
                Bitmap2DSensor c = senseCamera((x, y) ->
                                $.p(id, $.p($.the(ii), $.the(jj)), $.the(x), $.the(y)),
                        //$.p(
                        //$.inh(
//                                        $.p(x, y),
//                                        subSection
//                                ),
                        new ScaledBitmap2D(() -> g.image, px, py)
                                .crop(
                                        (float) i / dx, (float) j / dy,
                                        (float) (i + 1) / dx, (float) (j + 1) / dy))
                        .resolution(0.04f);

                cams.add(c);
            }

        window(new Gridding(
                cams.stream().map(c->new VectorSensorView(c, this).withControls()).collect(toList())),
                400, 900);


        float width = g.getWidth();
        float height = g.getHeight();
        int gpsDigits = 8;
        float gpsRes = 0.1f;
        senseNumber(level -> $.inh($.the(level), $.p(id, $.the("y"))),
                () -> g.player[OBJ_Y] / height,
                gpsDigits, DigitizedScalar.FuzzyNeedle
        ).resolution(gpsRes);
        senseNumber(level -> $.inh($.the(level), $.p(id, $.the("x"))),
                () -> g.player[OBJ_X] / width,
                gpsDigits, DigitizedScalar.FuzzyNeedle
        ).resolution(gpsRes);


        actionToggle($.inh("fire", id), b -> g.keys[VK_SHOOT] = b);

        if (canPause) {
            actionToggle($.inh("pause", id),
                    b -> g.paused = b);
        }


        initToggle();
        //initBipolar();

        rewardNormalized("alive", -1, +1, ()->{
            if (g.playerDead > 1)
                return -1f;
            else if (g.paused)
                return Float.NaN;
            else
                return +1;
        });

        rewardNormalized("destroy",0, 1, ()->{

            if (g.paused)
                return 0;

            int nextScore = g.score;

            float r = nextScore - lastScore;

            lastScore = nextScore;

            return Math.max(0, r);
       });

    }



    void initToggle() {
        actionPushButtonMutex($.inh("left", id), $.inh("right", id),
                b -> g.keys[VK_LEFT] = b,
                b -> g.keys[VK_RIGHT] = b);
        actionPushButtonMutex($.inh("up", id), $.inh("down", id),
                b -> g.keys[VK_UP] = b,
                b -> g.keys[VK_DOWN] = b);
    }

    void initBipolar() {

        float thresh = 0.1f;
        actionBipolar($.inh("y", id), dy -> {
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
        actionBipolar($.inh("x", id), dx -> {
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

package nars.experiment;

import java4k.gradius4k.Gradius4K;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.ScalarConcepts;
import nars.gui.Vis;
import nars.util.signal.Bitmap2DSensor;
import nars.video.Scale;

import static java4k.gradius4k.Gradius4K.*;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Gridding.col;

/**
 * Created by me on 4/30/17.
 */
public class Gradius extends NAgentX {

    static {
        //Param.STRONG_COMPOSITION = true;
    }

    private final Gradius4K g;

    public Gradius(NAR nar) {
        //super("g", nar, HaiQAgent::new);
        super("g", nar);

        this.g = new Gradius4K();

        g.updateMS = 20;

        //senseCameraReduced($.p(id,$.the("ae")), (Supplier)()->g.image, 32,32,2,2).resolution(0.5f);



        Bitmap2DSensor c1 = senseCamera($.p(id, $.the("global")), new Scale(() -> g.image, 50, 50)).resolution(0.05f);

        senseCameraRetina($.p(id,$.the("local")), () -> g.image, 16, 16).resolution(0.04f);


        //BufferedImageBitmap2D cc = new Scale(() -> g.image, 48, 48).blur();
//        senseCamera($.p(id, $.the("r")), new Scale(() -> g.image, 4, 4).mode(BufferedImageBitmap2D.ColorMode.R)).resolution(0.1f);
//        senseCamera($.p(id, $.the("g")), new Scale(() -> g.image, 4, 4).mode(BufferedImageBitmap2D.ColorMode.G)).resolution(0.1f);
//        senseCamera($.p(id, $.the("b")), new Scale(() -> g.image, 4, 4).mode(BufferedImageBitmap2D.ColorMode.B)).resolution(0.1f);

        //new ShapeSensor($.p(id, $.the("shape")), new BufferedImageBitmap2D(()->g.image),this);

        float width = g.getWidth();
        float height = g.getHeight();
        ScalarConcepts yPos = senseNumber((level)->$.inh($.p($.the("Y"), $.the(level)), id),
                ()->g.player[OBJ_Y] / height,
                4, ScalarConcepts.FuzzyNeedle
        ).resolution(0.1f);
        ScalarConcepts xPos = senseNumber((level)->$.inh($.p($.the("X"), $.the(level)), id),
                ()->g.player[OBJ_X] / width,
                4, ScalarConcepts.FuzzyNeedle
        ).resolution(0.1f);
        window(
                col(
                        Vis.conceptBeliefPlots(this, xPos, 8),
                        Vis.conceptBeliefPlots(this, yPos, 8)
                ),
                500, 500);


//        PixelBag cc = PixelBag.of(() -> g.image, 64, 64);
//        cc.setClarity(0.5f, 0.9f);


//        //TODO fix the panning/zooming
//        onFrame((z) -> {
//
//            float x = (g.player[Gradius4K.OBJ_X] - g.cameraX) / g.getWidth();
//            float y = (g.player[OBJ_Y]) / g.getHeight();
//
//            cc.setXRelative(x + 0.5f);
//            cc.setYRelative(y + 0.5f);
//            cc.setZoom(0.5f);
//
//            //cc.setXRelative( mario.)
//        });

        //CameraSensor<PixelBag> camScale = senseCamera("(G,cam)" /*"(nario,local)"*/, cc);
        //camScale.resolution(0.1f);



        //nar.truthResolution.setValue(0.05f);

//        BufferedImageBitmap2D camScaleLow = new Scale(() -> g.image, 16, 16);
//        for (BufferedImageBitmap2D.ColorMode cm : new BufferedImageBitmap2D.ColorMode[]{
//                R,
//                BufferedImageBitmap2D.ColorMode.B,
//                BufferedImageBitmap2D.ColorMode.G
//        }) {
//            senseCamera("Gc" + cm.name(), /*"(G,c" + cm.name() + ")"*/
//                    //(cm == R ? camScale : camScaleLow).filter(cm)
//                    camScaleLow.filter(cm).blur()
//            ).resolution(0.05f);
//        }

        /*nar.believe($.inh(*///$.sete(

        actionPushButton($.inh("fire", id),
                (b) -> g.keys[VK_SHOOT] = b);

        //initBipolar();
        initToggle();

//        actionTriState($.p(Atomic.the("dx")), (dh) -> {
//            g.keys[Gradius4K.VK_LEFT] = false;
//            g.keys[Gradius4K.VK_RIGHT] = false;
//            switch (dh) {
//                case +1:
//                    g.keys[Gradius4K.VK_RIGHT] = true;
//                    break;
//                case -1:
//                    g.keys[Gradius4K.VK_LEFT] = true;
//                    break;
//            }
//        }).term(),
//
//        actionTriState($.p(Atomic.the("dy")), (dh) -> {
//            g.keys[Gradius4K.VK_UP] = false;
//            g.keys[Gradius4K.VK_DOWN] = false;
//            switch (dh) {
//                case +1:
//                    g.keys[Gradius4K.VK_DOWN] = true;
//                    break;
//                case -1:
//                    g.keys[Gradius4K.VK_UP] = true;
//                    break;
//            }
//        }).term())/*, id))*/;


    }

    void initToggle() {
        actionToggle($.inh("up", id),
                (b) -> g.keys[VK_UP] = b);
        actionToggle($.inh("down", id),
                (b) -> g.keys[VK_DOWN] = b);
        actionToggle($.inh("left", id),
                (b) -> g.keys[VK_LEFT] = b);
        actionToggle($.inh("right", id),
                (b) -> g.keys[VK_RIGHT] = b);
    }

    void initBipolar() {
        actionBipolar($.inh("y", id), (dy) -> {
            float thresh = 0.33f;
            if (dy < -thresh) {
                g.keys[VK_UP] = false; g.keys[VK_DOWN] = true;
            } else if (dy > +thresh) {
                g.keys[VK_UP] = true; g.keys[VK_DOWN] = false;
            } else {
                g.keys[VK_UP] = false; g.keys[VK_DOWN] = false;
            }
            return dy;
        });
        actionBipolar($.inh("x", id), (dx) -> {
           float thresh = 0.33f;
           if (dx < -thresh) {
               g.keys[VK_LEFT] = false; g.keys[VK_RIGHT] = true;
           } else if (dx > +thresh) {
               g.keys[VK_LEFT] = true; g.keys[VK_RIGHT] = false;
           } else {
               g.keys[VK_LEFT] = false; g.keys[VK_RIGHT] = false;
           }
           return dx;
       });
    }


    int lastScore;

    @Override
    protected float act() {


        int nextScore = g.score;

        float r = (nextScore - lastScore);
//        if (r > 0)
//            System.out.println(r);
        lastScore = nextScore;

        if (g.playerDead > 1)
            return -1f;
        else
            return r;
    }

    public static void main(String[] args) {

        NAgentX.runRT(Gradius::new, 20f);

    }

}

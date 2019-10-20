package nars.experiment.minicraft;

import jcog.Util;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.Narsese;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.video.AutoclassifiedBitmap;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.SpaceGraph;

import java.awt.image.BufferedImage;

import static nars.$.$;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends GameX {

    private final SideScrollMinicraft craft;

    public static void main(String[] args) {
        Companion.initFn(30, nar1 -> {
            try {
                return new SideCraft(nar1);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public SideCraft(NAR nar) throws Narsese.NarseseException {
        super("cra");

        this.craft = new SideScrollMinicraft();


        var camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;

        var cam = new PixelBag(new MonoBufImgBitmap2D(() -> camBuffer), 48, 32)
                .addActions($.$("cra"), this);


        var camAE = new AutoclassifiedBitmap("cra", cam.pixels, 8, 8, 32, this);
        camAE.alpha.set(0.1f);
        SpaceGraph.window(camAE.newChart(), 500, 500);


        var pixels = senseCamera("cra", cam);


        /* slow */


        actionPushButton($("cra(key,left)"), (b4) -> {
            if (b4) craft.player.startLeft(false /* slow */);
            else craft.player.stopLeft();
        });

        /* slow */


        actionPushButton($("cra(key,right)"), (b3) -> {
            if (b3) craft.player.startRight(false /* slow */);
            else craft.player.stopRight();
        });


        actionPushButton($("cra(key,up)"), (b2) -> {
            if (b2) craft.player.startClimb();
            else craft.player.stopClimb();
        });


        actionPushButton($("cra(key,mouseL)"), (BooleanProcedure) (b1) -> craft.leftClick = b1);


        actionPushButton($("cra(key,mouseR)"), (BooleanProcedure) (b) -> craft.rightClick = b);

        var mSpeed = 45f;
        actionBipolar($("cra(mouse,X)"), (v) -> {
            var x = craft.screenMousePos.x;
            var xx = Util.clamp(Math.round(x + v * mSpeed), 0, camBuffer.getWidth() - 1);

            craft.screenMousePos.x = xx;
            return v;

        });
        actionBipolar($("cra(mouse,Y)"), (v) -> {
            var y = craft.screenMousePos.y;
            var yy = Util.clamp(Math.round(y + v * mSpeed), 0, camBuffer.getHeight() - 1);
            craft.screenMousePos.y = yy;
            return v;
        });


        reward(() -> {

            float nextScore = 0;
            for (var i = 0; i < gameFramesPerCycle; i++)
                nextScore += craft.frame();
            var ds = nextScore - prevScore;
            this.prevScore = nextScore;
            return ds;
        });

        craft.startGame(false, 512);
    }


    float prevScore;
    static final int gameFramesPerCycle = 1;


}

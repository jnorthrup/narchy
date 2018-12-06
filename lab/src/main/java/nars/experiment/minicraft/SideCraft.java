package nars.experiment.minicraft;

import jcog.Util;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.sensor.Bitmap2DSensor;
import nars.video.AutoclassifiedBitmap;
import nars.video.PixelBag;
import spacegraph.SpaceGraph;

import java.awt.image.BufferedImage;

import static nars.$.$;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends NAgentX {

    private final SideScrollMinicraft craft;
    private final Bitmap2DSensor<PixelBag> pixels;
    private final AutoclassifiedBitmap camAE;

    public static void main(String[] args) {
        runRT(nar1 -> {
            try {
                return new SideCraft(nar1);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }
        }, 30);
    }

    public SideCraft(NAR nar) throws Narsese.NarseseException {
        super("cra", nar);

        this.craft = new SideScrollMinicraft();


        BufferedImage camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;

        PixelBag cam = new PixelBag(new MonoBufImgBitmap2D(() -> camBuffer), 48, 32).addActions($.$("cra"), this);


        camAE = new AutoclassifiedBitmap("cra", cam.pixels, 8, 8, 32, this);
        SpaceGraph.window(camAE.newChart(), 500, 500);


        pixels = senseCamera("cra", cam);


        actionToggle($("cra(key,left)"), (b) -> {
            if (b) craft.player.startLeft(false /* slow */);
            else craft.player.stopLeft();
        });
        actionToggle($("cra(key,right)"), (b) -> {
            if (b) craft.player.startRight(false /* slow */);
            else craft.player.stopRight();
        });
        actionToggle($("cra(key,up)"), (b) -> {
            if (b) craft.player.startClimb();
            else craft.player.stopClimb();
        });
        actionToggle($("cra(key,mouseL)"), (b) -> craft.leftClick = b);
        actionToggle($("cra(key,mouseR)"), (b) -> craft.rightClick = b);
        float mSpeed = 45f;
        actionBipolar($("cra(mouse,X)"), (v) -> {
            int x = craft.screenMousePos.x;
            int xx = Util.clampI(x + v * mSpeed, 0, camBuffer.getWidth() - 1);

            craft.screenMousePos.x = xx;
            return v;

        });
        actionBipolar($("cra(mouse,Y)"), (v) -> {
            int y = craft.screenMousePos.y;
            int yy = Util.clampI(y + v * mSpeed, 0, camBuffer.getHeight() - 1);
            craft.screenMousePos.y = yy;
            return v;
        });


        reward(() -> {

            float nextScore = 0;
            for (int i = 0; i < gameFramesPerCycle; i++)
                nextScore = craft.frame();
            float ds = nextScore - prevScore;
            this.prevScore = nextScore;
            return ds;
        });

        craft.startGame(false, 512);
    }


    float prevScore;
    final int gameFramesPerCycle = 1;


}

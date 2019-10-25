package nars.experiment.minicraft;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.Narsese;
import nars.experiment.minicraft.side.SideScrollMinicraft;
import nars.experiment.minicraft.side.awtgraphics.AwtGraphicsHandler;
import nars.game.Game;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.video.AutoclassifiedBitmap;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.SpaceGraph;

import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.*;

/**
 * Created by me on 9/19/16.
 */
public class SideCraft extends GameX {

    private final SideScrollMinicraft craft;

    public static void main(String[] args) {
        Companion.initFn(30.0F, new Function<NAR, Game>() {
            @Override
            public Game apply(NAR nar1) {
                try {
                    return new SideCraft(nar1);
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    public SideCraft(NAR nar) throws Narsese.NarseseException {
        super("cra");

        this.craft = new SideScrollMinicraft();


        BufferedImage camBuffer = ((AwtGraphicsHandler) craft.gfx).buffer;

        PixelBag cam = new PixelBag(new MonoBufImgBitmap2D(new Supplier<BufferedImage>() {
            @Override
            public BufferedImage get() {
                return camBuffer;
            }
        }), 48, 32)
                .addActions($.INSTANCE.$("cra"), this);


        AutoclassifiedBitmap camAE = new AutoclassifiedBitmap("cra", cam.pixels, 8, 8, 32, this);
        camAE.alpha.set(0.1f);
        SpaceGraph.window(camAE.newChart(), 500, 500);


        Bitmap2DSensor<PixelBag> pixels = senseCamera("cra", cam);


        /* slow */


        actionPushButton(INSTANCE.$("cra(key,left)"), new BooleanProcedure() {
            @Override
            public void value(boolean b4) {
                if (b4) craft.player.startLeft(false /* slow */);
                else craft.player.stopLeft();
            }
        });

        /* slow */


        actionPushButton(INSTANCE.$("cra(key,right)"), new BooleanProcedure() {
            @Override
            public void value(boolean b3) {
                if (b3) craft.player.startRight(false /* slow */);
                else craft.player.stopRight();
            }
        });


        actionPushButton(INSTANCE.$("cra(key,up)"), new BooleanProcedure() {
            @Override
            public void value(boolean b2) {
                if (b2) craft.player.startClimb();
                else craft.player.stopClimb();
            }
        });


        actionPushButton(INSTANCE.$("cra(key,mouseL)"), new BooleanProcedure() {
            @Override
            public void value(boolean b1) {
                craft.leftClick = b1;
            }
        });


        actionPushButton(INSTANCE.$("cra(key,mouseR)"), new BooleanProcedure() {
            @Override
            public void value(boolean b) {
                craft.rightClick = b;
            }
        });

        float mSpeed = 45f;
        actionBipolar(INSTANCE.$("cra(mouse,X)"), new FloatToFloatFunction() {
            @Override
            public float valueOf(float v) {
                int x = craft.screenMousePos.x;
                int xx = Util.clamp(Math.round((float) x + v * mSpeed), 0, camBuffer.getWidth() - 1);

                craft.screenMousePos.x = xx;
                return v;

            }
        });
        actionBipolar(INSTANCE.$("cra(mouse,Y)"), new FloatToFloatFunction() {
            @Override
            public float valueOf(float v) {
                int y = craft.screenMousePos.y;
                int yy = Util.clamp(Math.round((float) y + v * mSpeed), 0, camBuffer.getHeight() - 1);
                craft.screenMousePos.y = yy;
                return v;
            }
        });


        reward(new FloatSupplier() {
            @Override
            public float asFloat() {

                float nextScore = (float) 0;
                for (int i = 0; i < gameFramesPerCycle; i++)
                    nextScore += craft.frame();
                float ds = nextScore - prevScore;
                SideCraft.this.prevScore = nextScore;
                return ds;
            }
        });

        craft.startGame(false, 512);
    }


    float prevScore;
    static final int gameFramesPerCycle = 1;


}

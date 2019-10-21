package nars.experiment.minicraft;

import jcog.math.FloatSupplier;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.Narsese;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.game.Game;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.video.AutoclassifiedBitmap;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.SpaceGraph;

import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;


/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends GameX {

    private final TopDownMinicraft craft;
    private float prevHealth;
    float prevScore;
    private Bitmap2DSensor<PixelBag> pixels;

    public TopCraft(NAR nar) throws Narsese.NarseseException {
        super("cra");

        this.craft = new TopDownMinicraft();
        TopDownMinicraft.start(craft);
        //craft.changeLevel(1);

        PixelBag p = new PixelBag(new MonoBufImgBitmap2D(new Supplier<BufferedImage>() {
            @Override
            public BufferedImage get() {
                return craft.image;
            }
        }), 64, 64).addActions(id, this);
        int nx = 8;
        //return new float[]{p.X, p.Y, p.Z};
        AutoclassifiedBitmap camAE = new AutoclassifiedBitmap("cae", p.pixels, nx, nx, new AutoclassifiedBitmap.MetaBits() {
            @Override
            public float[] get(int subX, int subY) {
                return new float[1]; //return new float[]{p.X, p.Y, p.Z};
            }
        }, 8, this);
        camAE.alpha.set(0.04f);
        camAE.noise.set(0.02f);
        SpaceGraph.window(camAE.newChart(), 500, 500);
        onFrame(p::updateBitmap);


        senseSwitch($.func("dir", id), new IntSupplier() {
            @Override
            public int getAsInt() {
                return craft.player.dir;
            }
        }, 0, 4);
        sense($.func("stamina", id), new FloatSupplier() {
            @Override
            public float asFloat() {
                return (float) (craft.player.stamina) / ((float) craft.player.maxStamina);
            }
        });
        sense($.func("health", id), new FloatSupplier() {
            @Override
            public float asFloat() {
                return (float) (craft.player.health) / ((float) craft.player.maxHealth);
            }
        });

        int tileMax = 13;
        senseSwitch("tile:here", () -> craft.player.tile().id, 0, tileMax);
        senseSwitch("tile:up", () -> craft.player.tile(0, 1).id, 0, tileMax);
        senseSwitch("tile:down", () -> craft.player.tile(0, -1).id, 0, tileMax);
        senseSwitch("tile:right", () -> craft.player.tile(1, 0).id, 0, tileMax);
        senseSwitch("tile:left", () -> craft.player.tile(-1, 0).id, 0, tileMax);

        InputHandler input = craft.input;
        actionPushButton($.func("fire", id), input.attack::pressed/*, 16*/);
        actionPushButtonMutex($.func("l", id), $.func("r", id),
                input.left::pressed, input.right::pressed);
        actionPushButtonMutex($.func("u", id), $.func("d", id),
                input.up::pressed, input.down::pressed);
        actionPushButton($.func("next", id), new BooleanProcedure() {
            @Override
            public void value(boolean i) {
                if (craft.menu != null) {
                    input.up.pressed(false);
                    input.down.pressIfUnpressed();
                }
            }
        });
        actionPushButton($.func("menu", id), new FloatSupplier() {
            @Override
            public float asFloat() {
                return 0.9f;
            }
        }, input.menu::pressIfUnpressed);

        rewardNormalized("score", -1.0F, (float) +1, new FloatSupplier() {
            @Override
            public float asFloat() {
                float nextScore = (float) craft.frameImmediate();
                float ds = nextScore - prevScore;
                if (ds == (float) 0)
                    return Float.NaN;
                TopCraft.this.prevScore = nextScore;
                //System.out.println("score delta:" + ds);
                return ds;
            }
        });
        rewardNormalized("health", -1.0F, (float) +1, new FloatSupplier() {
            @Override
            public float asFloat() {
                float nextHealth = (float) craft.player.health;
                float dh = nextHealth - prevHealth;
                if (dh == (float) 0)
                    return Float.NaN;
                TopCraft.this.prevHealth = nextHealth;
                //System.out.println("health delta: " + dh);
                return dh;
                //return (craft.player.health / ((float) craft.player.maxHealth));
            }
        });
    }

    public static void main(String[] args) {
        Companion.initFn(20.0F, new Function<NAR, Game>() {
            @Override
            public Game apply(NAR n) {
                try {
                    TopCraft tc = new TopCraft(n);


                    return tc;
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }


}

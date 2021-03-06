package jake2;

import com.jogamp.opengl.GL;
import jake2.client.CL_input;
import jake2.client.Key;
import jake2.game.EntHurtAdapter;
import jake2.game.PlayerView;
import jake2.game.edict_t;
import jake2.sound.jsound.SND_JAVA;
import jake2.sys.IN;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave2d.BrightnessNormalize;
import jcog.signal.wave2d.ImageFlip;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.Narsese;
import nars.game.Game;
import nars.game.sensor.FreqVectorSensor;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.term.Term;
import nars.video.AutoclassifiedBitmap;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.WaveBitmap;
import spacegraph.video.GLScreenShot;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import static jake2.Globals.STAT_FRAGS;
import static jake2.Globals.cl;
import static nars.$.*;
import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * Created by me on 9/22/16.
 */
public class Jake2Agent extends GameX implements Runnable {

    static final int FPS = 24;
    static float timeScale = 2.5f;

    static float yawSpeed = 10.0F;

    private static final boolean lookPitch = false;
    static float pitchSpeed = 5.0F;

    private final GLScreenShot rgb;
    private final GLScreenShot depth;
    private final FreqVectorSensor hear;




    public class PlayerData {
        public float health;
        public float velX;
        public float velY;
        public float velZ;
        public float speed;
        public int weaponState;
        public short frags;
        public float angle;
        edict_t player = null;

        final AtomicInteger damageInflicted = new AtomicInteger();

        protected synchronized void update() {

            edict_t p = PlayerView.current_player;
            if (player == null || player!=p) {
                if (p==null)
                    return;
                player = p;
                p.hurt = new EntHurtAdapter() {
                    @Override
                    public void hurt(edict_t self, edict_t other, int damage) {
                        if (other!=self) {
                            System.err.println("hurt " + other + " (" + damage + ")");
                            damageInflicted.addAndGet(damage);
                        }
                    }

                    @Override
                    public String getID() {
                        return "player_hurt";
                    }
                };
            }


            if (p.deadflag > 0) {


                Key.Event(Key.K_ENTER, true, 0);
                return;
            }

            health = (float) p.health;


            weaponState = p.client.weaponstate;

            frags = p.client.ps.stats[STAT_FRAGS];


            angle = p.angle;

            float[] v = p.velocity;
            velX = v[0];
            velY = v[1];
            velZ = v[2];
            speed = (float) Math.sqrt((double) (velX * velX + velY * velY + velZ * velZ));

        }


    }

    protected static String nextMap() {
        return "demo1";
    }

    final PlayerData player = new PlayerData();

    public Jake2Agent(NAR nar) throws Narsese.NarseseException {
        super("q");


//        Bitmap2DSensor<PixelBag> vision = senseCameraRetina(
//                "q", screenshotter, 32, 24);


        //int px = 64, py = 48, nx = 4, aeStates = 8;

        rgb = new GLScreenShot(true);
        depth = new GLScreenShot(false);

        int py = 48;
        int px = 64;
        PixelBag rgbVision = new PixelBag(
                new BrightnessNormalize(
                        new ImageFlip(false, true, new ScaledBitmap2D(rgb, px, py))
                ), px, py);
        rgbVision.setZoom((float) 0);

        Bitmap2DSensor depthVision = senseCamera("depth", new BrightnessNormalize(
            new ImageFlip(false, true, new ScaledBitmap2D(depth, px / 4, py / 4)
        )));


        int aeStates = 16;
        int nx = 8;
        AutoclassifiedBitmap rgbAE = new AutoclassifiedBitmap($.INSTANCE.the("gray"), rgbVision, nx, nx, aeStates, this);
        rgbAE.alpha(0.03f);
        rgbAE.noise.set(0.05f);



        //SpaceGraph.(column(visionView, camAE.newChart()), 500, 500);
//        }

        BitmapMatrixView rgbView = new BitmapMatrixView(rgbVision);
        onFrame(rgbView::updateIfShowing);

        BitmapMatrixView depthView = new BitmapMatrixView(depthVision.src);
        onFrame(depthView::updateIfShowing);

        SpaceGraph.window(grid(rgbView, rgbAE.newChart(), depthView ), 500, 500);

        hear = new FreqVectorSensor(nar, new CircularFloatBuffer(8*1024),
                512, 16, new IntFunction<Term>() {
            @Override
            public Term apply(int f) {
                return $.INSTANCE.inh($.INSTANCE.the(f), "hear");
            }
        });
        addSensor(hear);
        WaveBitmap hearView = new WaveBitmap(hear.buf, 300, 64);
        onFrame(hearView::update);
        SpaceGraph.window(grid(new VectorSensorChart(hear, this).withControls(),
                //spectrogram(hear.buf, 0.1f,512, 16),
                new ObjectSurface(hear), hearView), 400, 400);


        actionPushButtonMutex(
                $.INSTANCE.the("fore"), $.INSTANCE.the("back"),
                new BooleanProcedure() {
                    @Override
                    public void value(boolean x) {
                        CL_input.in_forward.state = x ? 1 : 0;
                    }
                },
                new BooleanProcedure() {
                    @Override
                    public void value(boolean x) {
                        CL_input.in_back.state = x ? 1 : 0;
                    }
                }
        );

//        actionPushButtonMutex(
//                $.the("strafeLeft"), $.the("strafeRight"),
//                x -> CL_input.in_moveleft.state = x ? 1 : 0,
//                x -> CL_input.in_moveright.state = x ? 1 : 0
//        );

        actionPushButtonMutex(
                $.INSTANCE.the("left"), $.INSTANCE.the("right"),
                new Runnable() {
                    @Override
                    public void run() {
                        cl.viewangles[Defines.YAW] += yawSpeed;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        cl.viewangles[Defines.YAW] -= yawSpeed;
                    }
                }
        );


        actionPushButton(INSTANCE.$("jump"), new BooleanProcedure() {
            @Override
            public void value(boolean x1) {
                CL_input.in_up.state = x1 ? 1 : 0;
            }
        });


        actionPushButton(INSTANCE.$("fire"), new BooleanProcedure() {
            @Override
            public void value(boolean x) {
                CL_input.in_attack.state = x ? 1 : 0;
            }
        });

        if (lookPitch) {
            actionPushButtonMutex(
                    $.INSTANCE.the("lookUp"), $.INSTANCE.the("lookDown"),
                    new Runnable() {
                        @Override
                        public void run() {
                            cl.viewangles[Defines.PITCH] = Math.min((float) +30, cl.viewangles[Defines.PITCH] + pitchSpeed);
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            cl.viewangles[Defines.PITCH] = Math.max(-30.0F, cl.viewangles[Defines.PITCH] - pitchSpeed);
                        }
                    }
            );
        }





        onFrame(player::update);

        rewardNormalized("health", -1.0F, (float) +1, new FloatFirstOrderDifference(nar::time, new FloatSupplier() {
            @Override
            public float asFloat() {
                return player.health;
            }
        }).nanIfZero());

        rewardNormalized("speed", (float) 0, (float) +1, new FloatNormalized(new FloatSupplier() {
            @Override
            public float asFloat() {
                return player.speed;
            }
        }));
        rewardNormalized("frags", (float) 0, (float) +1,
                new FloatFirstOrderDifference(nar::time, player.damageInflicted::getOpaque).nanIfZero());


//        ()->{
//            player.update();
//
//            float nextState = player.health * 4f + player.speed / 2f + player.frags * 2f;
//
//            float delta = nextState - state;
//
//            state = nextState;
//
//            return delta;
//        });


        new Thread(this).start();
    }


    @Override
    public void run() {


        IN.mouse_avail = false;
        Jake2.run(new String[]{
                //"+god 1",
                //"+deathmatch 1",
                "vid_width 800",
                "vid_height 600",
                "+dmflags 1024",
                "+cl_gun 0",
                "lookspring 1",
                "+timescale " + timeScale,


                "+map " + nextMap()

        }, new Consumer<>() {

            int lastSamplePos = -1;

            @Override
            public void accept(GL g) {
                rgb.update(g);
                depth.update(g);

                byte[] b = SND_JAVA.dma.buffer;
                if (b == null)
                    return;
                int sample = SND_JAVA.SNDDMA_GetDMAPos();
                if (sample != lastSamplePos) {


                    //System.out.println(sample + " " + SND_MIX.paintedtime);

                    int samples = 1024;
                    if (hear.buf.available() < samples * 2)
                        hear.buf.skip(samples * 2);

                    int from = (sample - samples * 4) % b.length;
                    int to = (sample - samples * 2) % b.length;

                    while (from < 0) from += b.length;
                    while (to < 0) to += b.length;

                    if (from <= to) {
                        hear.buf.writeS16(b, from, to, (float) 1);
                    } else {
                        hear.buf.writeS16(b, from, b.length, (float) 1);
                        hear.buf.writeS16(b, 0, to, (float) 1);
                    }


                    lastSamplePos = sample;
                }


            }
        });


        /*
        Outer Base		base1.bsp
        Installation		base2.bsp
        Comm Center	base3.bsp
        Lost Station		train.bsp
        Ammo Depot	bunk1.bsp
        Supply Station	ware1.bsp
        Warehouse		ware2.bsp
        Main Gate		jail1.bsp
        Destination Center	jail2.bsp
        Security Complex	jail3.bsp
        Torture Chambers	jail4.bsp
        Guard House	jail5.asp
        Grid Control		security.bsp
        Mine Entrance	mintro.asp
        Upper Mines		mine1.bsp
        Borehole		mine2.bsp
        Drilling Area		mine3.bsp
        Lower Mines		mine4.bsp
        Receiving Center	fact1.bsp
        Sudden Death	fact3.bsp
        Processing Plant	fact2.bsp
        Power Plant		power1.bsp
        The Reactor		power2.bsp
        Cooling Facility	cool1.bsp
        Toxic Waste Dump	waste1.bsp
        Pumping Station 1	waste2.bsp
        Pumping Station 2	waste3.bsp
        Big Gun		biggun.bsp
        Outer Hangar	hangar1.bsp
        Comm Satelite	space.bsp
        Research Lab	lab.bsp
        Inner Hangar	hangar2.bsp
        Launch Command	command.bsp
        Outlands		strike.bsp
        Outer Courts	city1.bsp
        Lower Palace	city2.bsp
        Upper Palace	city3.bsp
        Inner Chamber	boss1.bsp
        Final Showdown	boss2.bsp


        Read more: http:
        Under Creative Commons License: Attribution Non-Commercial No Derivatives
        Follow us: @CheatCodes on Twitter | CheatCodes on Facebook

         */

    }


    public static void main(String[] args) {

        Companion.initFn((float) FPS, new Function<NAR, Game>() {
            @Override
            public Game apply(NAR nar1) {
                try {
                    return new Jake2Agent(nar1);
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

}



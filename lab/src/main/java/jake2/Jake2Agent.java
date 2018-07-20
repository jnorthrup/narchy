package jake2;

import jake2.client.CL_input;
import jake2.client.Key;
import jake2.client.refexport_t;
import jake2.game.PlayerView;
import jake2.game.edict_t;
import jake2.render.Base;
import jake2.render.JoglGL2Renderer;
import jake2.sys.IN;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.sensor.Bitmap2DSensor;
import nars.video.PixelBag;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static jake2.Globals.*;
import static jake2.render.Base.vid;
import static nars.$.$;

/**
 * Created by me on 9/22/16.
 */
public class Jake2Agent extends NAgentX implements Runnable {

    ByteBuffer seen;
    int width, height;
    boolean see = true;

    final int[] nBits = {8, 8, 8};
    final int[] bOffs1 = {2, 1, 0};

    final ColorSpace raster = ColorSpace.getInstance(1000);
    final ComponentColorModel colorModel = new ComponentColorModel(raster, nBits, false, false, 1, 0);

    final Supplier<BufferedImage> screenshotter = () -> {
        if (seen == null || width == 0 || height == 0)
            return null;

        byte[] bb = seen.array();

        WritableRaster raster1 = Raster.createInterleavedRaster(
                new DataBufferByte(bb, bb.length), width, height, width * 3, 3, bOffs1, new Point(0, 0));

        BufferedImage b = new BufferedImage(colorModel, raster1, false, null);
        return b;
    };

    public class PlayerData {
        public float health;
        public float velX;
        public float velY;
        public float velZ;
        public float speed;
        public int weaponState;
        public short frags;
        public float angle;

        protected void update() {
            edict_t p = PlayerView.current_player;
            if (p == null) return;
            if (p.deadflag > 0) {


                Key.Event(Key.K_ENTER, true, 0);
                return;
            }

            health = p.health;


            weaponState = p.client.weaponstate;

            frags = p.client.ps.stats[STAT_FRAGS];


            angle = p.angle;

            float[] v = p.velocity;
            velX = v[0];
            velY = v[1];
            velZ = v[2];
            speed = (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);

        }


    }

    protected String nextMap() {
        return "demo2";
    }

    final PlayerData player = new PlayerData();

    public Jake2Agent(NAR nar) throws Narsese.NarseseException {
        super("q", nar);


        Bitmap2DSensor<PixelBag> qcam = senseCameraRetina(
                "q", screenshotter, 32, 24);
        qcam.src.setMinZoomOut(0.5f);
        qcam.src.setMaxZoomOut(1f);
        qcam.src.vflip = true;
        qcam.resolution(0.01f);


//        senseFields("q", player);

        actionToggle($("q(move,fore)"), (x) -> CL_input.in_forward.state = x ? 1 : 0);
        actionToggle($("q(move,back)"), (x) -> CL_input.in_back.state = x ? 1 : 0);


        actionToggle($("q(move,left)"), (x) -> CL_input.in_moveleft.state = x ? 1 : 0);
        actionToggle($("q(move,right)"), (x) -> CL_input.in_moveright.state = x ? 1 : 0);
        actionToggle($("q(jump)"), (x) -> CL_input.in_up.state = x ? 1 : 0);


        float yawSpeed = 20;
        float pitchSpeed = 5;
        actionToggle($("q(look,left)"), (x) ->
                cl.viewangles[Defines.YAW] += yawSpeed);
        actionToggle($("q(look,right)"), (x) ->
                cl.viewangles[Defines.YAW] -= yawSpeed);
        actionToggle($("q(look,up)"), (x) ->
                cl.viewangles[Defines.PITCH] = Math.min(30, cl.viewangles[Defines.PITCH] + pitchSpeed));
        actionToggle($("q(look,down)"), (x) ->
                cl.viewangles[Defines.PITCH] = Math.max(-30, cl.viewangles[Defines.PITCH] - pitchSpeed));

        actionToggle($("q(attak)"), (x) -> CL_input.in_attack.state = x ? 1 : 0);


        reward(()->{
            player.update();

            float nextState = player.health * 4f + player.speed / 2f + player.frags * 2f;

            float delta = nextState - state;

            state = nextState;

            return delta;
        });

        new Thread(this).start();
    }

    float state;

    @Override
    public void run() {


        IN.mouse_avail = false;
        Jake2.run(new String[]{
                "+god",


                "+dmflags 1024",
                "+cl_gun 0",
                "+timescale 0.5",


                "+map " + nextMap()

        }, this::onDraw);


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

    protected synchronized void onDraw() {

        refexport_t r = re;
        JoglGL2Renderer renderer = (JoglGL2Renderer) r;

        if (see) {
            if (seen != null) {
                seen.rewind();
            }

            width = vid.getWidth();
            height = vid.getHeight();
            seen = ((Base) renderer.impl).see(seen);
        }

    }


    public static void main(String[] args) {
        runRT(nar1 -> {
            try {
                return new Jake2Agent(nar1);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }
        }, 8);
    }
}



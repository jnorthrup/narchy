package nars.experiment.minicraft;

import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.util.signal.Bitmap2DSensor;
import nars.video.AutoclassifiedBitmap;
import nars.video.PixelBag;
import spacegraph.SpaceGraph;
















/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends NAgentX {

    private final TopDownMinicraft craft;
    private Bitmap2DSensor<PixelBag> pixels;
    private final AutoclassifiedBitmap camAE;

    public static void main(String[] args) {
        runRT(n -> {
            try {
                TopCraft tc = new TopCraft(n);



                return tc;
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }
        }, 20);
    }

    public TopCraft(NAR nar) throws Narsese.NarseseException {
        super("cra", nar);

        this.craft = new TopDownMinicraft();





























        
        

        
                

        PixelBag p = PixelBag.of(() -> craft.image, 64, 64).addActions(id, this);
        int nx = 8;
        camAE = new AutoclassifiedBitmap("cae", p.pixels, nx, nx,   (subX, subY) -> {
            
            
            return new float[]{ p.X, p.Y, p.Z };
        }, 4, this) {
            @Override
            public void accept(NAR n) {
                p.update();
                super.accept(n);
            }
        };
        SpaceGraph.window(camAE.newChart(), 500, 500);


        senseSwitch($.func("dir",id), ()->craft.player.dir, 0, 4);
        sense($.func("stamina", id), ()->(craft.player.stamina)/((float)craft.player.maxStamina));
        sense($.func("health", id), ()->(craft.player.health)/((float)craft.player.maxHealth));

        int tileMax = 13;
        senseSwitch("tile:here", ()->craft.player.tile().id, 0, tileMax);
        senseSwitch("tile:up", ()->craft.player.tile(0,1).id, 0, tileMax);
        senseSwitch("tile:down", ()->craft.player.tile(0,-1).id, 0, tileMax);
        senseSwitch("tile:right", ()->craft.player.tile(1,0).id, 0, tileMax);
        senseSwitch("tile:left", ()->craft.player.tile(-1,0).id, 0, tileMax);

        InputHandler input = craft.input;
        actionPushButton($.func("fire",id), input.attack::pressed/*, 16*/ );
        actionTriState($.func("x", id), (i)->{
           boolean l = false, r = false;
           switch (i) {
               case -1: l = true;  break;
               case +1: r = true;  break;
           }
           input.left.pressed(l);
           input.right.pressed(r);
        });
        actionTriState($.func("y",id), (i)->{
            if (craft.menu==null) {
                boolean u = false, d = false;
                switch (i) {
                    case -1:
                        u = true;
                        break;
                    case +1:
                        d = true;
                        break;
                }
                input.up.pressed(u);
                input.down.pressed(d);
            }
        });
        actionPushButton($.func("next",id), (i)->{
           if (craft.menu!=null) {
               input.up.pressed(false);
               input.down.pressIfUnpressed();
           }
        });
        actionToggle($.func("menu", id), input.menu::pressIfUnpressed);









        TopDownMinicraft.start(craft);
    }



    float prevScore;
    @Override protected float act() {

        float nextScore = craft.frameImmediate();
        float ds = nextScore - prevScore;
        this.prevScore = nextScore;
        float r = ((ds/2f)) + 2f * (craft.player.health/((float)craft.player.maxHealth)-0.5f);
        return r;
    }












































































}

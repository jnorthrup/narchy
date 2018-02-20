package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.ql.HaiQAgent;
import jcog.signal.ArrayBitmap2D;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.NAgent;
import nars.index.term.HijackConceptIndex;
import nars.op.RLBooster;
import nars.term.Term;
import nars.util.signal.Bitmap2DSensor;
import nars.video.CameraSensorView;
import spacegraph.render.Draw;

import static spacegraph.render.JoglSpace.window;

/* 1D and 2D grid tracking */
public class TrackXY extends NAgent {

    final ArrayBitmap2D view;

    //target position, to reach
    float tx, ty;

    //source position, to control
    float sx, sy;
    public Bitmap2DSensor cam;


    private float controlSpeed = 1f, targetSpeed = 0.5f;
    private float sharpness = 0.9f;

    public static void main(String[] args) {

        float fps = 10;

        boolean nars = true;
        boolean rl = false;

        NARS nb = NARS
                .realtime(fps)
                .index(new HijackConceptIndex(8 * 1024, 4));

        if (nars) {
            nb.deriverAdd(1, 8);
        }

        NAR n = nb.get();


        TrackXY t = new TrackXY(4, 4);
        n.on(t);

        n.time.synch(n);



        window(new CameraSensorView(t.cam, n) {
            @Override
            protected void paint(GL2 gl, int dtMS) {
                super.paint(gl, dtMS);
                RectFloat2D at = cellRect(t.sx, t.sy, 0.5f, 0.5f);
                gl.glColor4f(1, 0, 0, 0.9f);
                Draw.rect(gl, at);
            }
        }, 800, 800);

        n.startFPS(fps);
        t.runFPS(fps);

        if (rl) {
            new RLBooster(t,
                    HaiQAgent::new,
                    //RandomAgent::new,
                    1);
        }

        n.log();

    }

    protected TrackXY(int x, int y) {
        super("trackXY", null);
        this.view = new ArrayBitmap2D(x, y);
    }

    @Override
    protected void start(NAR nar) {
        super.start(nar);

        if (view.height() > 1) {
            actionToggle($.the("up"), () -> {
                sy = Util.clamp(sy + controlSpeed, 0, view.height()-1);

            });
            actionToggle($.the("down"), () -> {
                sy = Util.clamp(sy - controlSpeed, 0, view.height()-1);
            });
        }

        actionToggle($.the("right"), ()->{
            sx = Util.clamp(sx + controlSpeed, 0, view.width()-1);
        });
        actionToggle($.the("left"), ()->{
            sx = Util.clamp(sx - controlSpeed, 0, view.width()-1);
        });

        this.cam = new Bitmap2DSensor((Term)null, view, nar);


        randomize();
    }

    protected void randomize() {
        this.tx = random().nextInt(view.width());
        this.sx = random().nextInt(view.width());
        if (view.height() > 1) {
            this.ty = random().nextInt(view.height());
            this.sy = random().nextInt(view.height());
        } else {
            this.ty = this.sy = 0;
        }
    }

    protected void update() {
        synchronized (view) {
            this.tx = Util.clamp(tx + 2 * targetSpeed * (random().nextFloat()-0.5f), 0, view.width() - 1);
            if (view.height() > 1)
                this.ty = Util.clamp(ty + 2 * targetSpeed * (random().nextFloat()-0.5f), 0, view.height() - 1);
        }
    }

    @Override
    protected synchronized float act() {

        synchronized (view) {
            view.set((x,y)->{
                float dist = (float) Math.sqrt(Util.sqr(tx-x) + Util.sqr(ty-y));
                return Math.max(0, 1-dist*sharpness);
            });
            update();
        }

        cam.input();



        float dist = (float) Math.sqrt(Util.sqr(tx-sx) + Util.sqr(ty-sy));

        return 1f/(1f+dist);
    }
}

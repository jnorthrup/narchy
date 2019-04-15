package spacegraph.video;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import jcog.event.Off;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMoveWindow;
import spacegraph.input.finger.FingerResizeWindow;
import spacegraph.input.finger.Fingering;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.input.finger.impl.NewtMouseFinger;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceGraph;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.Animating;
import spacegraph.space2d.hud.Zoomed;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.util.animate.Animated;

import java.util.function.BiConsumer;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static spacegraph.SpaceGraph.surfaceWindow;

public class OrthoSurfaceGraph extends JoglDisplay implements SurfaceGraph {


    private final static short MOVE_AND_RESIZE_BUTTON = 1;


    /* render context */
    public final ReSurface rendering = new ReSurface();

    public final Finger finger;
    private final NewtKeyboard keyboard;


    private final Fingering windowResize = new FingerResizeWindow(this, MOVE_AND_RESIZE_BUTTON) {
        @Override
        public Surface touchNext(Surface prev, Surface next) {
            return layers;
        }
    };

    private final Fingering windowMove = new FingerMoveWindow(MOVE_AND_RESIZE_BUTTON) {

        @Override
        protected JoglDisplay window() {
            return OrthoSurfaceGraph.this;
        }

        @Override
        public void move(float dx, float dy) {
            int nx = Math.round(windowStartX + dx);
            int ny = Math.round(windowStartY - dy);
            if (nx != windowStartX || ny != windowStartY)
                video.setPosition(nx, ny);
        }

        @Override
        public Surface touchNext(Surface prev, Surface next) {
            return layers;
        }
    };

    private final Stacking layers = new Stacking() {

        private transient int _w, _h;

        private final BiConsumer<GL2, ReSurface> reset = (g, rr) -> {
            rr.pw = _w;
            rr.ph = _h;
            rr.x1 = rr.y1 = 0;
            rr.x2 = w();
            rr.y2 = h();
        };

        @Override
        public Surface finger(Finger finger) {

            //check windowResize first since it is a more exclusive condition than windowMove
            if (finger.pressed(MOVE_AND_RESIZE_BUTTON)) {
                if (/*finger.tryFingering(windowResize) || */finger.tryFingering(windowMove))
                    return null;
            }

            return super.finger(finger);
        }

        @Override
        protected void renderContent(ReSurface r) {
            _w = video.getWidth();
            _h = video.getHeight();
            r.on(reset);
            super.renderContent(r);
        }
    };


    /**
     *
     * @param content content to interact zoomably
     * @param pw pixel width of window, or 0 to start invisibly
     * @param ph pixel height of window, or 0 to start invisibly
     */
    public OrthoSurfaceGraph(Surface content, int pw, int ph) {
        super();

        if (pw > 0 && ph > 0)
            video.show(pw, ph);

        video.window.addWindowListener(new com.jogamp.newt.event.WindowAdapter() {

            @Override
            public void windowResized(WindowEvent e) {
                JoglWindow w = video;
                int W = w.getWidth();
                int H = w.getHeight();
                layers.resize(W, H);
            }

            @Override
            public void windowDestroyNotify(WindowEvent e) {
                layers.delete();
            }
        });

        video.window.setPointerVisible(false);

        finger = new NewtMouseFinger(this, layers);

        keyboard = new NewtKeyboard(/*TODO this */);

        layers.start(this);


        Zoomed z = new Zoomed(this, keyboard, content);
        layers.add(z);
        layers.add(z.overlayZoomBounds(finger));
        layers.add(finger.overlayCursor());



//        //addOverlay(this.keyboard.keyFocusSurface(cam));
//        layers.add((Surface) hud);

//            {
//                layers.add(new Menu());
//            }

    }

    @Override
    protected void updateCamera(float dtS) {
        //null
    }

    @Override
    protected void renderOrthos(float dtS) {

        int n = layers.size();
        if (n <= 0)
            return;

        GL2 g = video.gl;

        int w = video.getWidth(), h = video.getHeight();
        rendering.restart(w, h, dtS, video.renderFPS);

        g.glViewport(0, 0, w, h);
        g.glMatrixMode(GL_PROJECTION);
        g.glLoadIdentity();

        g.glOrtho(0, w, 0, h, -1.5, 1.5);
        g.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        rendering.render(g);
        rendering.clear();
//        g.glDisable(GL.GL_DEPTH_TEST);
//
//        g.glEnable(GL.GL_DEPTH_TEST);
    }

    @Override
    protected void update() {
        rendering.restart(video.getWidth(), video.getHeight());
        layers.tryRender(rendering);
    }



    public Off animate(Animated c) {
        return onUpdate(c);
    }

    private Off animate(Runnable c) {
        return onUpdate(c);
    }

    @Override
    public boolean keyFocus(Surface s) {
        return keyboard.focus(s);
    }


    @Override
    public final SurfaceGraph root() {
        return this;
    }

    /** spawns a developer windonw
     * @return*/
    public OrthoSurfaceGraph dev(/** boolean hover mode (undecorated), other options */ ) {
        Gridding g = new Gridding();

        TextEdit fingerInfo = new TextEdit(60, 12);

        g.add(fingerInfo);

        return surfaceWindow(new Animating<>(g, ()->{
            Surface t = finger.touching();
            fingerInfo.text(
                "buttn: " + finger.buttonSummary() + '\n' +
                "state: " +  finger.fingering() + '\n' +
                "posPx: " + finger.posPixel + '\n' +
                //"posGl: " + finger.posGlobal(layers.first(Zoomed.class)) + '\n' +
                "touch: " + t + '\n' +
                "posRl: " + (t!=null ? finger.posRelative(t.bounds) : "?") + '\n' +
                ""
            );
        }, 0.25f),500,300);
    }

    static class Menu extends Bordering {

        //TODO different modes, etc
        public Menu() {
            super(new EmptySurface());
            //animate(new DelayedHover(finger));
            set(SW, Gridding.row(
                    PushButton.awesome("mouse-pointer"), //click, wire, etc
                    PushButton.awesome("i-cursor"), //text edit
                    PushButton.awesome("question-circle") //inspect
            ));
            set(NE, new Gridding(
                    PushButton.awesome("bolt") //popup with configuration, options, tasks
            ));
//            set(NE, new Gridding(
//                    new PushButton("X"),
//                    new PushButton("Y"),
//                    new PushButton("Z")
//            ));
//            set(SE, new Gridding(
//                    //new Timeline2D<>()
//            ));

        }

    }


}

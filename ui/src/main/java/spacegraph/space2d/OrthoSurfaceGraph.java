package spacegraph.space2d;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import jcog.event.Off;
import jcog.exe.Exe;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerMoveWindow;
import spacegraph.input.finger.FingerResizeWindow;
import spacegraph.input.finger.Fingering;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.input.finger.impl.NewtMouseFinger;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.hud.Zoomed;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.util.animate.Animated;
import spacegraph.video.JoglDisplay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class OrthoSurfaceGraph extends JoglDisplay implements SurfaceGraph {


    private final static short MOVE_WINDOW_BUTTON = 1;
    private final static short RESIZE_WINDOW_BUTTON = MOVE_WINDOW_BUTTON;
    //    private final Ortho<MutableListContainer> hud;
    private final Map<String, Pair<Object, Runnable>> singletons = new ConcurrentHashMap();
    private final Finger finger;
    private final NewtKeyboard keyboard;
    private final Fingering windowResize = new FingerResizeWindow(this, RESIZE_WINDOW_BUTTON);

    private final Fingering windowMove = new FingerMoveWindow(MOVE_WINDOW_BUTTON) {

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
            //return prev; //dont change
            return null;
        }
    };

    public final Stacking layers = new Stacking() {

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
            if (finger.tryFingering(windowResize)) {
                //..
                return this;
            }
            if (finger.tryFingering(windowMove)) {
                //..
                return this;
            }

            Surface s = super.finger(finger);



            return s;
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


        layers.clipBounds = false; //HACK

        finger = new NewtMouseFinger(this);

        keyboard = new NewtKeyboard(/*TODO this */);

        video.window.setPointerVisible(false);

        video.window.addWindowListener(new com.jogamp.newt.event.WindowAdapter() {

            @Override
            public void windowResized(WindowEvent e) {
                Exe.invokeLater(OrthoSurfaceGraph.this::resize);
            }

            @Override
            public void windowDestroyNotify(WindowEvent e) {
                Exe.invokeLater(layers::stop);
            }
        });

        layers.start(this);

        if (pw > 0 && ph > 0) {
            video.show(pw, ph);
        }


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

    protected void resize() {
//        RectFloat bounds = RectFloat.X0Y0WH(0, 0, display.getWidth(), display.getHeight());
//        layers.pos(bounds);


        GLWindow w = video.window;
        int W = w.getWidth();
        int H = w.getHeight();
        layers.resize(W, H);


    }

    @Override
    protected void renderOrthos(float dtS) {

        int n = layers.size();
        if (n <= 0) {
            return;
        }

        GL2 g = video.gl;

        int w = video.window.getWidth(), h = video.window.getHeight();
        rendering.restart(w, h, dtS, video.renderFPS);

        g.glDisable(GL.GL_DEPTH_TEST);

        g.glViewport(0, 0, w, h);
        g.glMatrixMode(GL_PROJECTION);
        g.glLoadIdentity();

        g.glOrtho(0, w, 0, h, -1.5, 1.5);
        g.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        rendering.render(g);
        rendering.clear();

        g.glEnable(GL.GL_DEPTH_TEST);
    }

    @Override
    protected void update(ReSurface rendering) {
        layers.tryRender(rendering);
    }

    @Override
    public Object the(String key) {
        synchronized (singletons) {
            Pair<Object, Runnable> x = singletons.get(key);
            return x == null ? null : x.getOne();
        }
    }

    public Off animate(Animated c) {
        return onUpdate(c);
    }

    private Off animate(Runnable c) {
        return onUpdate(c);
    }

    @Override
    public void the(String key, Object added, Runnable onRemove) {
        synchronized (singletons) {

            Pair<Object, Runnable> removed = null;
            if (added == null) {
                assert (onRemove == null);
                removed = singletons.remove(key);
            } else {
                removed = singletons.put(key, pair(added, onRemove));
            }

            if (removed != null) {
                if (removed.getOne() == added) {

                } else {
                    removed.getTwo().run();
                }
            }
        }
    }


    @Override
    public boolean keyFocus(Surface s) {
        return keyboard.focus(s);
    }


    @Override
    public final SurfaceGraph root() {
        return this;
    }

    class Menu extends Bordering {

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

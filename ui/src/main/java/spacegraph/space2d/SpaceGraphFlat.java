package spacegraph.space2d;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import jcog.event.Off;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.input.finger.impl.NewtMouseFinger;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.util.animate.Animated;
import spacegraph.video.JoglSpace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class SpaceGraphFlat extends JoglSpace implements SurfaceRoot {


//    private final Ortho<MutableListContainer> hud;
    private final Map<String, Pair<Object, Runnable>> singletons = new ConcurrentHashMap();

    private final Finger finger;
    private final NewtKeyboard keyboard;


    public final Stacking layers = new Stacking() {

        private transient int _w, _h;

        private final BiConsumer<GL2, ReSurface> reset = (g, rr) -> {
            rr.pw = _w;
            rr.ph = _h;
            rr.x1 = rr.y1 = 0;
            rr.x2 = _w; rr.y2 = _h;
        };

        @Override protected void compileChildren(ReSurface r) {
            _w = display.getWidth(); _h = display.getHeight();
            forEach(c -> {
                r.on(reset);
                c.rerender(r);
            });
        }
    };

    public SpaceGraphFlat(Surface _content) {
        super();

        layers.showing = true; layers.clipBounds = false; //HACK

        finger = new NewtMouseFinger(this);

        keyboard = new NewtKeyboard(/*TODO this */);


        later(() -> {
            display.window.addWindowListener(new com.jogamp.newt.event.WindowAdapter(){
                @Override
                public void windowResized(WindowEvent e) {
                    resize();
                }
            });
            display.window.setPointerVisible(false); //HACK
            layers.start(this);

            Ortho content = new ZoomOrtho(this, keyboard); content.set(_content);
            layers.add(content);

            layers.add(finger.zoomBoundsSurface(content.cam));
            layers.add(finger.cursor());

//        //addOverlay(this.keyboard.keyFocusSurface(cam));
//        layers.add((Surface) hud);

//            {
//                layers.add(new Menu());
//            }

            resize();
        });
    }

    protected void resize() {
        RectFloat bounds = RectFloat.X0Y0WH(0, 0, display.getWidth(), display.getHeight());
        layers.pos(bounds);
    }

    @Override
    protected void renderOrthos(int dtMS) {

        int n = layers.size();
        if (n <= 0) {
            return;
        }

        GL2 g = display.gl;

        int w = display.window.getWidth(), h = display.window.getHeight();
        rendering.restart(w, h, dtMS, display.renderFPS);

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
        layers.rerender(rendering);
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
    public final SurfaceRoot root() {
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

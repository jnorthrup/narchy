package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import jcog.event.Off;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.input.finger.impl.NewtMouseFinger;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.util.animate.Animated;
import spacegraph.video.JoglSpace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class SpaceGraphFlat extends JoglSpace implements SurfaceRoot {


//    private final Ortho<MutableListContainer> hud;
    private final Map<String, Pair<Object, Runnable>> singletons = new ConcurrentHashMap();

    private final Finger finger;
    private final NewtKeyboard keyboard;

    public final Stacking layers = new Stacking();
    private final ZoomOrtho contentZoomed;

    public SpaceGraphFlat(Surface content) {
        super();

        layers.showing = true; //HACK
        layers.clipBounds = false;

        finger = new NewtMouseFinger(this);

        keyboard = new NewtKeyboard(/*TODO this */);



        contentZoomed = new ZoomOrtho(this, content, finger, keyboard);

//        MutableListContainer hud = new MutableListContainer() {
//
//            @Override
//            protected void paintIt(GL2 gl, SurfaceRender r) {
//                gl.glPushMatrix();
//                gl.glLoadIdentity();
//            }
//
//
//            @Override
//            protected void compileAbove(SurfaceRender r) {
//                r.on(GLMatrixFunc::glPopMatrix);
//            }
//
//        };

        layers.add(contentZoomed);
        layers.add(finger.zoomBoundsSurface(contentZoomed.cam));
        layers.add(finger.cursorSurface());
//        //addOverlay(this.keyboard.keyFocusSurface(cam));
//        layers.add((Surface) hud);

//        {
//            Menu menu = new Menu();
//            menu.pos(0, 0, 200, 200);
//            hud.add(menu);
//        }


        later(() -> {
            display.window.setPointerVisible(false); //HACK
            layers.pos(0, 0, display.getWidth(), display.getHeight());
            layers.start(this);
        });
    }


    @Override
    protected void renderOrthos(int dtMS) {

        int n = layers.size();
        if (n <= 0) {
            return;
        }

        GL2 gl = display.gl;

        int w = display.window.getWidth(), h = display.window.getHeight();
        rendering.restart(w, h, dtMS);

        gl.glDisable(GL2.GL_DEPTH_TEST);

        rendering.render(w, h, gl);
        rendering.clear();

        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    @Override
    protected void update(SurfaceRender rendering) {
        layers.recompile(rendering);
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

    class Menu extends Widget {

        //TODO different modes, etc
        public Menu() {
            //animate(new DelayedHover(finger));

            set(new Gridding(
                    new PushButton("X"),
                    new PushButton("Y"),
                    new PushButton("Z")
            ));
        }
    }

}

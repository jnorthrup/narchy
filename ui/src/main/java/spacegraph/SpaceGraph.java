package spacegraph;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.list.FasterList;
import spacegraph.input.FPSLook;
import spacegraph.input.KeyXYZ;
import spacegraph.input.OrbMouse;
import spacegraph.math.v3;
import spacegraph.phys.constraint.BroadConstraint;
import spacegraph.render.JoglPhysics;
import spacegraph.render.JoglSpace;
import spacegraph.render.SpaceGraphFlat;
import spacegraph.space.DynamicListSpace;
import spacegraph.widget.meta.AutoSurface;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<X> extends JoglPhysics<X> {


    final List<Ortho> orthos = new FasterList<>(1);

    final List<AbstractSpace<X>> inputs = new FasterList<>(1);


    final List<Ortho> preAdd = new FasterList();
    public final Topic<SpaceGraph> onUpdate = new ListTopic<>();
    public int windowX, windowY;

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        super.windowDestroyed(windowEvent);
        orthos.clear();
        inputs.clear();
        onUpdate.clear();
        preAdd.clear();
    }


    /**
     * number of items that will remain cached, some (ideally most)
     * will not be visible but once were and may become visible again
     */
    public SpaceGraph() {

    }


    public SpaceGraph(AbstractSpace<X>... cc) {
        this();

        for (AbstractSpace c : cc)
            add(c);
    }

    public SpaceGraph(Spatial<X>... cc) {
        this();

        add(cc);
    }




//    public Ortho ortho(Surface ortho) {
//        Ortho o = new Ortho(ortho);
//        add(o);
//        return o;
//    }

    public SpaceGraph add(Ortho c) {
        if (window == null) {
            preAdd.add(c);
        } else {
            _add(c);
        }
        return this;
    }

    private void _add(Ortho c) {
        this.orthos.add(c);
        c.start(this);
    }

    public SpaceGraph add(AbstractSpace<X> c) {
        if (inputs.add(c))
            c.start(this);
        return this;
    }

    public void removeSpace(AbstractSpace<X> c) {
        if (inputs.remove(c)) {
            c.stop();
        }
    }

    final Queue<Spatial> toRemove = new ArrayBlockingQueue(1024);

    public void remove(Spatial<X> y) {
        toRemove.add(y);
    }


//    public @Nullable Spatial getIfActive(X t) {
//        Spatial v = atoms.getIfPresent(t);
//        return v != null && v.preactive() ? v : null;
//    }


    public SpaceGraph setGravity(v3 v) {
        dyn.setGravity(v);
        return this;
    }


    public static float r(float range) {
        return (-0.5f + (float) Math.random()) * 2f * range;
    }


    @Override
    public void init(GL2 gl) {
        super.init(gl);

        for (Ortho f : preAdd) {
            _add(f);
        }
        preAdd.clear();


        initInput();
        updateWindowInfo();

    }


    protected void initInput() {

        //default 3D input controls
        addMouseListener(new FPSLook(this));
        addMouseListener(new OrbMouse(this));
        addKeyListener(new KeyXYZ(this));

    }

    @Override
    public Iterator<Spatial<X>> iterator() {
        throw new UnsupportedOperationException("use forEach()");
    }

    @Override
    final public void forEach(Consumer<? super Spatial<X>> each) {

        for (int i = 0, inputsSize = inputs.size(); i < inputsSize; i++) {
            inputs.get(i).forEach(each);
        }
    }

    @Override
    protected void render(int dtMS) {
        super.render(dtMS);
        int facialsSize = orthos.size();
        if (facialsSize > 0) {

            ortho();

            gl.glDisable(GL2.GL_DEPTH_TEST);

            GL2 gl = this.gl;
            for (int i = 0; i < facialsSize; i++) {
                orthos.get(i).render(gl, dtMS);
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }

    @Override
    protected void update(long dtMS) {

        toRemove.forEach(x -> x.delete(dyn));
        toRemove.clear();


        List<AbstractSpace<X>> ii = this.inputs;
        for (int i = 0, inputs1Size = ii.size(); i < inputs1Size; i++) {
            AbstractSpace<X> s = ii.get(i);

            s.update(this, dtMS);

        }

        super.update(dtMS);

        onUpdate.emit(this);
    }


    public String summary() {
        return dyn.summary();
    }

//    void print(AbstractSpace s) {
//        System.out.println();
//        //+ active.size() + " active, "
//        System.out.println(s + ": " + this.atoms.estimatedSize() + " cached; " + "\t" + dyn.summary());
//        /*s.forEach(System.out::println);
//        dyn.objects().forEach(x -> {
//            System.out.println("\t" + x.getUserPointer());
//        });*/
//        System.out.println();
//    }

    public DynamicListSpace<X> add(Spatial<X>... s) {
        DynamicListSpace<X> l = new DynamicListSpace<X>() {

            final List<Spatial<X>> ls = new FasterList().with(s);

            @Override
            protected List<? extends Spatial<X>> get() {
                return ls;
            }
        };
        add(l);
        return l;
    }

//    @Override
//    public void windowGainedFocus(WindowEvent windowEvent) {
//        updateWindowInfo();
//    }

    @Override
    public void windowResized(WindowEvent windowEvent) {
        updateWindowInfo();
    }

    @Override
    public void windowMoved(WindowEvent windowEvent) {
        updateWindowInfo();
    }

    private final AtomicBoolean gettingScreenPointer = new AtomicBoolean(false);

    private void updateWindowInfo() {
        GLWindow rww = window;
        if (rww == null)
            return;
        if (!rww.isRealized() || !rww.isVisible() || !rww.isNativeValid()) {
            return;
        }

        if (gettingScreenPointer.compareAndSet(false, true)) {

            window.getScreen().getDisplay().getEDTUtil().invoke(false, () -> {
                try {
                    Point p = rww.getLocationOnScreen(new Point());
                    windowX = p.getX();
                    windowY = p.getY();
                } finally {
                    gettingScreenPointer.set(false);
                }
            });
        }
    }

    public static SpaceGraph window(Surface s, int w, int h) {
        SpaceGraph win = new SpaceGraphFlat(
                new ZoomOrtho(s)
        );
        if (w > 0 && h > 0) {

            win.show(w, h);
        }
        return win;
    }

    public static SpaceGraph window(Object o, int w, int h) {
        if (o instanceof SpaceGraph) {
            SpaceGraph s = (SpaceGraph) o;
            s.show(w, h);
            return s;
        } else if (o instanceof Spatial) {
            return window(((Spatial) o), w, h);
        } else if (o instanceof Surface) {
            return window(((Surface) o), w, h);
        } else {
            return window(new AutoSurface(o), w, h);
        }
    }

    public static SpaceGraph window(Spatial s, int w, int h) {
        return window(w, h, s);
    }

    public static SpaceGraph window(int w, int h, Spatial... s) {
        SpaceGraph win = new SpaceGraph(s);
        win.show(w, h);
        return win;
    }

    @Deprecated
    public SpaceGraph with(BroadConstraint b) {
        dyn.addBroadConstraint(b);
        return this;
    }

    public JoglSpace camPos(float x, float y, float z) {
        camPos.set(x, y, z);
        return this;
    }



    //    public static class PickDragMouse extends SpaceMouse {
//
//        public PickDragMouse(JoglPhysics g) {
//            super(g);
//        }
//    }
//    public static class PickZoom extends SpaceMouse {
//
//        public PickZoom(JoglPhysics g) {
//            super(g);
//        }
//    }

}

package spacegraph.video;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.AnimatorBase;
import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.exe.Exe;
import jcog.exe.InstrumentedLoop;
import jcog.exe.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.animate.Animated;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;


public abstract class JoglWindow implements GLEventListener, WindowListener {


    /**
     * JOGL default is 10ms; we dont need/want it that often
     */
    private static final long syncConstructionDelay = 10;


    private static final Collection<JoglWindow> windows = new ConcurrentFastIteratingHashSet<>(new JoglWindow[0]);
    final Topic<JoglWindow> onUpdate = new ListTopic<>();
    private final Logger logger;


    /**
     * update loop
     */
    private final InstrumentedLoop updater;
    private final ConcurrentLinkedQueue<Consumer<JoglWindow>> preRenderTasks = new ConcurrentLinkedQueue();
    public float renderFPS = 30f;
    public volatile GLWindow window;
    public GL2 gl;
    /**
     * update time since last cycle (S)
     */
    public float dtS = 0;
    private float updateFPS = 30f;
    /**
     * render loop
     */
    private GameAnimatorControl renderer;
    /**
     * update time since last cycle (ms)
     */
    protected long dtMS = 0;
    private long lastRenderMS = System.currentTimeMillis();
    private volatile int nx, ny, nw, nh;

    private final Consumer<JoglWindow> windowUpdater = (s) -> {
        GLWindow w = window;
        if (w == null)
            return;

        int nw = this.nw;
        int nh = this.nh;

        if (nw == 0 || nh == 0) {
            if (w.isVisible()) {
                w.setVisible(false);
            }
        } else {
            if (!w.isVisible())
                w.setVisible(true);

            if (nw != getWidth() || nh != getHeight()) {
                w.setSize(nw, nh);
            }

            int nx = this.nx;
            int ny = this.ny;
            if (nx != getX() || ny != getY())
                w.setPosition(nx, ny);

        }


    };

    JoglWindow() {
        logger = LoggerFactory.getLogger(toString());


        renderer = new GameAnimatorControl();

        updater = new InstrumentedLoop() {
            @Override
            public boolean next() {
                return JoglWindow.this.next();
            }
        };
    }

    private static GLWindow window() {
        return window(config());
    }


    private static GLWindow window(GLCapabilitiesImmutable config) {

        GLWindow w = GLWindow.create(config);


        return w;
    }

    private static GLCapabilitiesImmutable config() {


        GLCapabilities config = new GLCapabilities(


                GLProfile.get(GLProfile.GL2)


        );


        config.setStencilBits(1);


        return config;
    }

    public void off() {
        GLWindow w = this.window;
        if (w != null)
            Exe.invoke(w::destroy);
    }

    public final void pre(Consumer<JoglWindow> beforeNextRender) {
        preRenderTasks.add(beforeNextRender);
    }

    abstract protected void init(GL2 gl);

    public void printHardware() {


        System.err.print("GL:");
        System.err.println(gl);
        System.err.print("GL_VERSION=");
        System.err.println(gl.glGetString(GL.GL_VERSION));
        System.err.print("GL_EXTENSIONS: ");
        System.err.println(gl.glGetString(GL.GL_EXTENSIONS));
    }

    public final int getWidth() {
        return window.getSurfaceWidth();
    }

    public final int getWidthNext() {
        return nw;
    }

    public final int getHeight() {
        return window.getSurfaceHeight();
    }

    public final int getHeightNext() {
        return nh;
    }

//    public final int getXNext() {
//        return nx;
//    }
//
//    public final int getYNext() {
//        return ny;
//    }

    @Override
    public void dispose(GLAutoDrawable arg0) {
    }

    @Override
    public void windowResized(WindowEvent windowEvent) {
        //if (!preRenderTasks.contains(windowUpdater)) {
        this.nw = getWidth();
        this.nh = getHeight();
        //}
    }

    @Override
    public void windowMoved(WindowEvent windowEvent) {
        //if (!preRenderTasks.contains(windowUpdater)) {
        this.nx = getX();
        this.ny = getY();
        //}
    }

    @Override
    public void windowDestroyNotify(WindowEvent windowEvent) {
        renderer.stop();
        updater.stop();
    }

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        windows.remove(this);

    }

    @Override
    public void windowGainedFocus(WindowEvent windowEvent) {

    }

    @Override
    public void windowLostFocus(WindowEvent windowEvent) {

    }

    @Override
    public void windowRepaint(WindowUpdateEvent windowUpdateEvent) {

    }

    /**
     * dtMS - time transpired since last call (millisecons)
     *
     * @param dtMS
     */
    abstract protected void render(int dtMS);

    private boolean next() {
        if (window.isVisible()) {
            long cycleTimeNS = updater.cycleTimeNS;
            this.dtMS = cycleTimeNS / 1_000_000;
            this.dtS = cycleTimeNS / 1E9f;
            onUpdate.emit(this);
        }
        return true;
    }

    /**
     * dt in milliseconds since last update
     */
    public long dtMS() {
        return dtMS;
    }

    @Override
    public final void display(GLAutoDrawable drawable) {

//            if (gl == null)
//                gl = drawable.getGL().getGL2();

        long nowMS = System.currentTimeMillis(), renderDTMS = nowMS - lastRenderMS;
        if (renderDTMS > Integer.MAX_VALUE) renderDTMS = Integer.MAX_VALUE;
        this.lastRenderMS = nowMS;

        render((int) renderDTMS);

    }


    public void show(int w, int h, boolean async) {
        show("", w, h, async);
    }

    private void show(String title, int w, int h, int x, int y, boolean async) {

        //Exe.invokeLater(() -> {

        if (window != null) {

            return;
        }

        GLWindow W = window();
        this.window = W;

//        EDTUtil edt = window.getScreen().getDisplay().getEDTUtil();
//        if (!edt.isRunning()) {
//            edt.start();
//            edt.setPollPeriod(EDT_POLL_PERIOD_MS);
//        }

        window.addGLEventListener(this);
        window.addWindowListener(this);

        windows.add(this);

        W.setTitle(title);
        if (x != Integer.MIN_VALUE) {
            setPositionAndSize(x, y, w, h);
        } else {
            setSize(w, h);
        }
        //});

        if (!async) {
            Thread.yield();
            while (gl == null) {
                Util.sleepMS(syncConstructionDelay);
            }
        }


    }

    public void setVisible(boolean b) {
        if (!b) {
            setSize(0, 0);
        } else {
            setSize(Math.max(1, getWidth()), Math.max(1, getHeight()));
        }
    }


    public void setPosition(int x, int y) {
        setPositionAndSize(x, y, nw, nh);
    }

    private void setSize(int w, int h) {
        setPositionAndSize(nx, ny, w, h);
    }

    public void setPositionAndSize(int x, int y, int w, int h) {

        if (window == null) return;

        boolean change = false;
        if ((nx != x)) {
            nx = x;
            change = true;
        }
        if ((ny != y)) {
            ny = y;
            change = true;
        }
        if ((nw != w)) {
            nw = w;
            change = true;
        }
        if ((nh != h)) {
            nh = h;
            change = true;
        }

        if (change) {
            if (!preRenderTasks.contains(windowUpdater))
                pre(windowUpdater);
        }

    }

    @Override
    public final void init(GLAutoDrawable drawable) {

        GL2 gl = drawable.getGL().getGL2();
        this.gl = gl;

        if (gl.getGLProfile().isHardwareRasterizer()) {

            gl.setSwapInterval(1);
        } else {
            gl.setSwapInterval(4);
        }


        renderer.add(window);

        Draw.init(gl);

        init(gl);


        updater.setFPS(updateFPS);

    }

    public void setFPS(float render, float update) {

        logger.info("fps render={} update={}", render, update);
        renderFPS = render;
        updateFPS = update;
        if (updater.isRunning()) {
            renderer.loop.setFPS(renderFPS);
            updater.setFPS(updateFPS);
        }

    }

    private void show(String title, int w, int h, boolean async) {
        show(title, w, h, Integer.MIN_VALUE, Integer.MIN_VALUE, async);
    }

    public void addMouseListenerPost(MouseListener m) {
        window.addMouseListener(m);
    }

    public void addMouseListenerPre(MouseListener m) {
        window.addMouseListener(0, m);
    }

    public void addWindowListener(WindowListener m) {
        window.addWindowListener(m);
    }

    public void addKeyListener(KeyListener m) {
        window.addKeyListener(m);
    }


    public On onUpdate(Consumer<JoglWindow> c) {
        return onUpdate.on(c);
    }

    /**
     * adapter
     */
    public On onUpdate(Animated c) {
        return onUpdate.on((JoglWindow s) -> c.animate(dtS));
    }

    public On onUpdate(Runnable c) {
        return onUpdate.on((JoglWindow s) -> c.run());
    }

    public int getX() {
        return window.getX();
    }

    public int getY() {
        return window.getY();
    }

    public float getScreenY() {
        return window.getScreen().getHeight();
    }


/* from: Jake2's */
class GameAnimatorControl extends AnimatorBase {

    final Loop loop;
    private volatile boolean paused = true;


    GameAnimatorControl() {
        super();

        setIgnoreExceptions(false);
        setPrintExceptions(true);


        this.loop = new Loop() {


            @Override
            public String toString() {
                return JoglWindow.this + ".render";
            }

            @Override
            protected void starting() {
                paused = false;
            }


            @Override
            public boolean next() {

                if (window != null && !paused) {

                    preRenderTasks.removeIf(r -> {
                        r.accept(JoglWindow.this);
                        return true;
                    });

                    if (!drawables.isEmpty()) {
                        GLAutoDrawable d = drawables.get(0);
                        if (d == null)
                            return false;

                        d.display();
                        return true; //async
                    }
                }

                return true;

            }
        };


        loop.setFPS(renderFPS);


    }

    @Override
    protected String getBaseName(String prefix) {
        return prefix;
    }

    @Override
    public final boolean start() {
        return false;
    }


    @Override
    public final boolean stop() {

        pause();
        loop.stop();
        return true;
    }


    @Override
    public final boolean pause() {


        paused = true;
        return true;
    }

    @Override
    public final boolean resume() {
        paused = false;
        return true;
    }

    @Override
    public synchronized final boolean isStarted() {
        return loop.isRunning();
    }

    @Override
    public final boolean isAnimating() {
        return !paused;
    }

    @Override
    public final boolean isPaused() {
        return paused;
    }


}


}





















































































package spacegraph.video;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.AnimatorBase;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.event.ListTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.exe.Exe;
import jcog.exe.InstrumentedLoop;
import jcog.util.ArrayUtils;
import spacegraph.UI;
import spacegraph.util.animate.Animated;
import spacegraph.video.font.HersheyFont;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;


public abstract class JoglWindow implements GLEventListener, WindowListener {

    private static final Collection<JoglWindow> windows = new ConcurrentFastIteratingHashSet<>(new JoglWindow[0]);

//    static final Executor renderThread = Executors.newSingleThreadExecutor();

//    static {
//        Threading.disableSingleThreading();
//    }
    private static GLCapabilitiesImmutable config = null;

    static {
////        synchronized(JoglWindow.class) {
////            NEWTJNILibLoader.loadNEWT();
////        }
//        NewtFactory.setUseEDT(false);
    }

    //    private static final Logger logger = LoggerFactory.getLogger(JoglWindow.class);
    //    /**
//     * update loop
//     */
//    private final InstrumentedLoop updater;
    public final GLWindow window;
    public final Topic<JoglWindow> eventClosed = new ListTopic<>();
    final Topic<JoglWindow> onUpdate = new ListTopic<>();
    /**
     * render loop
     */
    private final GameAnimatorControl renderer;
    private final AtomicBoolean updateWindow = new AtomicBoolean(true);
    public GL2 gl;
//    public float renderFPSInvisibleRate = 0;
    /**
     * update time since last cycle (S)
     */
    public float dtS = 0;
    public float renderFPS = UI.FPS_default;
    /**
     * factor to decrease FPS of unfocused windows
     */
    private float renderFPSUnfocusedRate = 0.5f;
    private long lastRenderNS = System.nanoTime();
    private volatile int nx, ny, nw, nh;

    JoglWindow() {

        windows.add(this);

        synchronized (JoglWindow.class) {
            if (JoglWindow.config == null) {
                //lazy instantiate
                JoglWindow.config = config();
            }
        }

        this.window = GLWindow.create(config);
        window.addWindowListener(this);
        window.addGLEventListener(this);

        renderer = new GameAnimatorControl();


    }

    private static GLCapabilitiesImmutable config() {


        GLCapabilities config = new GLCapabilities(


                //GLProfile.getGL2GL3()
                GLProfile.getDefault()
                //GLProfile.get(new String[] { GLProfile.GL2ES2 }, true)
                //GLProfile.getMinimum(true)


        );


        config.setStencilBits(1);


        return config;
    }

    private void updateWindow() {
        GLWindow w = window;
        if (w == null)
            return;

        if (!updateWindow.compareAndSet(true, false))
            return;

        int nw = this.nw, nh = this.nh;

        if (nw == 0 || nh == 0) {
//            if (w.isVisible())
//                w.setVisible(false);

        } else {
//            if (!w.isVisible())
//                w.setVisible(true);

            if (nw != getWidth() || nh != getHeight()) {
                w.setSurfaceSize(nw, nh);
            }

            int nx = this.nx, ny = this.ny;
            if (nx != getX() || ny != getY())
                w.setPosition(nx, ny);

        }

    }

    public void off() {
        GLWindow w = this.window;
        if (w != null)
            w.destroy();
        //Exe.invokeLater(w::destroy);
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

    public final int getHeight() {
        return window.getSurfaceHeight();
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
        renderer.loop.stop();
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
//        updater.stop();
        eventClosed.emit(this);
    }

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        windows.remove(this);
    }

    @Override
    public void windowGainedFocus(WindowEvent windowEvent) {
        renderer.loop.setFPS(renderFPS);
    }

    @Override
    public void windowLostFocus(WindowEvent windowEvent) {
        renderer.loop.setFPS(renderFPSUnfocusedRate * renderFPS);
    }

    @Override
    public void windowRepaint(WindowUpdateEvent windowUpdateEvent) {
        //if (!updater.isRunning()) {
//        updater.setFPS(updateFPS /* window.hasFocus() ? updateFPS : updateFPS * updateFPSUnfocusedMultiplier */);
        //}
    }

    /**
     * dtMS - time transpired since last call (millisecons)
     *
     * @param dtMS
     */
    abstract protected void render(float dtS);


    @Override
    public final /*synchronized*/ void display(GLAutoDrawable drawable) {

        long nowNS = System.nanoTime();
        long renderDtNS = nowNS - lastRenderNS;
        this.lastRenderNS = nowNS;

        /* ns -> ms */
        //render((int) Math.min(Integer.MAX_VALUE, Math.round(renderDtNS / 1_000_000.0)));
        render((float) (renderDtNS / 1.0E9));

        //gl.glFlush();
        gl.glFinish();
    }


    public void show(int w, int h) {
        show("", w, h);
    }


    private void show(String title, int w, int h, int x, int y) {

        //when called from a main thread, unless this is executed as queued then the main thread can exit before the GL threads activate and this race condition results in a dead, empty window.
        //solution is to queue this to the global timer which starts the self-invoking loop

        //GLWorkerThread.invokeLater(()-> {
        Exe.invokeLater(() -> {

            GLWindow W = this.window;
            if (x != Integer.MIN_VALUE) {
                setPositionAndSize(x, y, w, h);
                W.setPosition(x, y); //needs lock
                W.setSize(w, h);  //needs lock
            } else {
                setSize(w, h);
                W.setSize(w, h); //needs lock
            }

            W.setTitle(title); //needs lock
            W.setVisible(true); //needs lock

        });

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
            updateWindow.set(true);
        }

    }

    @Override
    public final void init(GLAutoDrawable drawable) {

        GL2 gl = drawable.getGL().getGL2();
        this.gl = gl;

        if (gl.getGLProfile().isHardwareRasterizer()) {
            gl.setSwapInterval(0);
        } else {
            gl.setSwapInterval(2); //lower framerate
        }

        HersheyFont.load(gl);

        init(gl);

        renderer.add(window);

        //ready

        renderer.loop.setFPS(renderFPS);
    }

    public void setFPS(float render) {
        renderer.loop.setFPS(renderFPS = render);
//        updateFPS = update;
//        if (updater.isRunning()) {
//            updater.setFPS(updateFPS);
//        }

    }

    private void show(String title, int w, int h) {
        show(title, w, h, Integer.MIN_VALUE, Integer.MIN_VALUE);
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
        if (ArrayUtils.indexOf(window.getKeyListeners(), m) != -1)
            return;
        window.addKeyListener(m);
    }


    public Off onUpdate(Consumer<JoglWindow> c) {
        return onUpdate.on(c);
    }

    /**
     * adapter
     */
    public Off onUpdate(Animated c) {
        return onUpdate.on((JoglWindow s) -> c.animate(dtS));
    }

    public Off onUpdate(Runnable c) {
        return onUpdate.on((JoglWindow s) -> c.run());
    }

    /**
     * x-pixel coordinate of window left edge
     */
    public int getX() {
        return window.getX();
    }

    /**
     * y-pixel coordinate of window top edge.
     * note: this is the reverse direction of the generally-expected cartesian upward-pointing y-axis
     */
    public int getY() {
        return window.getY();
    }

    public float getScreenW() {
        return window.getScreen().getWidth();
    }

    public float getScreenH() {
        return window.getScreen().getHeight();
    }

    protected void clearComplete() {
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void clearMotionBlur(float rate /* TODO */) {


        gl.glAccum(GL2.GL_LOAD, 0.5f);

        gl.glAccum(GL2.GL_ACCUM, 0.5f);


        gl.glAccum(GL2.GL_RETURN, rate);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);


    }

    protected void update() {

    }

    /* from: Jake2's */
    private final class GameAnimatorControl extends AnimatorBase {

        final InstrumentedLoop loop;

        GameAnimatorControl() {
            super();

            setIgnoreExceptions(false);
            setPrintExceptions(true);

            this.loop = new DisplayLoop();

            //loop.setFPS(1);  //HACK initially trigger slowly
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
            return true;
        }


        @Override
        public final boolean pause() {
            loop.stop();
            return true;
        }

        @Override
        public final boolean resume() {
            return true;
        }

        @Override
        public final boolean isStarted() {
            return loop.isRunning();
        }

        @Override
        public final boolean isAnimating() {
            return loop.isRunning();
        }

        @Override
        public final boolean isPaused() {
            return !loop.isRunning();
        }

        private final class DisplayLoop extends InstrumentedLoop {

            @Override
            public String toString() {
                return JoglWindow.this + ".render";
            }

            @Override
            public boolean next() {

                if (window == null)
                    return false;

                if (!window.isVisible())
                    return false;

                if (drawables.isEmpty())
                    return true;

                GLAutoDrawable d = drawables.get(0);
                if (d != null) {

                    dtS = (float) renderer.loop.cycleTimeS;



                    onUpdate.emit(JoglWindow.this);

                    update();

                    updateWindow();

                    try {
                        d.flushGLRunnables();
                        d.display();
                    } catch (GLException e) {
                        e.printStackTrace();
                        stop();
                        return false;
                    }

                }

                return true;
            }
        }
    }


}





















































































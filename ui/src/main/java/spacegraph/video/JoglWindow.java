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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.UI;
import spacegraph.util.animate.Animated;
import spacegraph.video.font.HersheyFont;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;


public abstract class JoglWindow implements GLEventListener, WindowListener {


//    static final Executor renderThread = Executors.newSingleThreadExecutor();

//    static {
//        Threading.disableSingleThreading();
//    }

    private static final Collection<JoglWindow> windows = new ConcurrentFastIteratingHashSet<>(new JoglWindow[0]);
    final Topic<JoglWindow> onUpdate = new ListTopic<>();
    private static final Logger logger = LoggerFactory.getLogger(JoglWindow.class);


    //    /**
//     * update loop
//     */
//    private final InstrumentedLoop updater;
    public volatile GLWindow window;
    public GL2 gl;
    /**
     * update time since last cycle (S)
     */
    public float dtS = 0;
    public final Topic<JoglWindow> eventClosed = new ListTopic<>();

    //    private float updateFPS = 32f;
    public float renderFPS = UI.FPS_default;
    public float renderFPSUnfocused = renderFPS / 2;
    public float renderFPSInvisible = 0;

//    /** reduction throttle for update loop when unfoused */
//    private float updateFPSUnfocusedMultiplier = 0.25f;

    /**
     * render loop
     */
    private GameAnimatorControl renderer;

    private long lastRenderNS = System.nanoTime();
    private volatile int nx, ny, nw, nh;

    private final AtomicBoolean updateWindow = new AtomicBoolean(true);

    private void updateWindow() {
        if (!updateWindow.weakCompareAndSetVolatile(true, false))
            return;

        GLWindow w = window;

        int nw = this.nw, nh = this.nh;

        if (nw == 0 || nh == 0) {
//            if (w.isVisible())
//                w.setVisible(false);

        } else {
//            if (!w.isVisible())
//                w.setVisible(true);

            if (nw != getWidth() || nh != getHeight()) {
                w.setSize(nw, nh);
            }

            int nx = this.nx, ny = this.ny;
            if (nx != getX() || ny != getY())
                w.setPosition(nx, ny);

        }

    }

    JoglWindow() {


//        updater = new InstrumentedLoop() {
//            @Override
//            public boolean next() {
//                return JoglWindow.this.next();
//            }
//        };

        renderer = new GameAnimatorControl();


    }

    private static final GLCapabilitiesImmutable config = config();

    private static GLWindow window() {
        return window(config);
    }


    private static GLWindow window(GLCapabilitiesImmutable config) {

        GLWindow w = GLWindow.create(config);


        return w;
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

    public void off() {
        GLWindow w = this.window;
        if (w != null)
            Exe.invoke(w::destroy);
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
        renderer.loop.setFPS(renderFPSUnfocused);
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
    abstract protected void render(int dtMS);


    @Override
    public final void display(GLAutoDrawable drawable) {

        long nowNS = System.nanoTime(), renderDtNS = nowNS - lastRenderNS;
        this.lastRenderNS = nowNS;

        this.dtS = (float) (renderDtNS / 1.0E9);

        /* ns -> ms */
        render((int) Math.min(Integer.MAX_VALUE, Math.round(renderDtNS / 1_000_000.0)));

    }


    public GLWindow show(int w, int h) {
        return show("", w, h);
    }


    private GLWindow show(String title, int w, int h, int x, int y) {

        //Threading.invokeOnOpenGLThread(false, () -> {
        //GLWorkerThread.invokeLater(()->{
        //Exe.invokeLater(() -> {

        if (window != null) {

            return window;
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
            W.setPosition(x, y);
            W.setSize(w, h);
        } else {
            setSize(w, h);
            W.setSize(w, h);
        }
        W.setVisible(true);

        return W;
        //});


        //});

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
            gl.setSwapInterval(1);
        } else {
            gl.setSwapInterval(2); //lower framerate
        }


        renderer.add(window);

        HersheyFont.load(gl);

        init(gl);

        renderer.loop.setFPS(renderFPS);
    }

    public void setFPS(float render) {

        logger.info("fps render={}", render);
        renderFPS = render;
//        updateFPS = update;
//        if (updater.isRunning()) {
//            renderer.loop.setFPS(renderFPS);
//            updater.setFPS(updateFPS);
//        }

    }

    private GLWindow show(String title, int w, int h) {
        //Threading.invokeOnOpenGLThread(false, ()->{
        return show(title, w, h, Integer.MIN_VALUE, Integer.MIN_VALUE);
        //});
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

    public int getX() {
        return window.getX();
    }

    public int getY() {
        return window.getY();
    }

    public float getScreenY() {
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

    /* from: Jake2's */
    class GameAnimatorControl extends AnimatorBase {

        final InstrumentedLoop loop;



        GameAnimatorControl() {
            super();

            setIgnoreExceptions(false);
            setPrintExceptions(true);


            this.loop = new InstrumentedLoop() {


                @Override
                public String toString() {
                    return JoglWindow.this + ".render";
                }

                @Override
                protected void starting() {

                }


                /** waiting to be rendered */
                final AtomicBoolean waiting = new AtomicBoolean();

                private void render() {
                    try {
                        updateWindow();

                        if (!drawables.isEmpty()) {
                            GLAutoDrawable d = drawables.get(0);
                            if (d != null)
                                d.display();
                        }
                    } finally {
                        waiting.set(false);
                    }
                }

                @Override
                public boolean next() {

                    if (window == null)
                        return false;

                    //System.out.println(window + " " +window.isVisible());
                    if (window.isVisible()) {

                        if (waiting.compareAndSet(false, true)) {

                            onUpdate.emit(JoglWindow.this);

                            Threading.invokeOnOpenGLThread(false, this::render);
                        }

                    } else {
                        renderer.loop.stop();
                        renderer.stop();
                    }




                    return true;


                }
            };


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
            loop.stop();
            return true;
        }


        @Override
        public final boolean pause() {


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


    }


}





















































































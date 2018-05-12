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
import jcog.exe.InstrumentedLoop;
import jcog.exe.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.animate.Animated;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public abstract class JoglWindow implements GLEventListener, WindowListener {


    /**
     * JOGL default is 10ms; we dont need/want it that often
     */
    private static final long EDT_POLL_PERIOD_MS = 20;


    private static final Collection<JoglWindow> windows = new ConcurrentFastIteratingHashSet<>(new JoglWindow[0]);
    public final Topic<JoglWindow> onUpdate = new ListTopic<>();
    public final Logger logger;
    final AtomicBoolean rendering = new AtomicBoolean(false);

    /**
     * update loop
     */
    final InstrumentedLoop updater;
    final ConcurrentLinkedQueue<Consumer<JoglWindow>> preRenderTasks = new ConcurrentLinkedQueue();
    public float renderFPS = 30f;
    public volatile GLWindow window;
    public GL2 gl;
    /**
     * update time since last cycle (S)
     */
    public float dtS = 0;
    protected float updateFPS = 30f;
    /**
     * render loop
     */
    protected GameAnimatorControl renderer;
    /**
     * update time since last cycle (ms)
     */
    protected long dtMS = 0;
    private long lastRenderMS = System.currentTimeMillis();

    protected JoglWindow() {
        logger = LoggerFactory.getLogger(toString());

        renderer = new GameAnimatorControl();
        updater = new InstrumentedLoop() {
            @Override
            public boolean next() {
                return JoglWindow.this.next();
            }
        };
    }

    static GLWindow window() {
        return window(config());
    }

    static GLWindow window(GLCapabilitiesImmutable config) {
        GLWindow w = GLWindow.create(config);

        //w.setSharedContext(sharedDrawable.getContext());

        return w;
    }

    static GLCapabilitiesImmutable config() {


        GLCapabilities config = new GLCapabilities(

                //GLProfile.getMinimum(true)
                GLProfile.get(GLProfile.GL2)
                //GLProfile.getMaximum(true)

        );


//        config.setBackgroundOpaque(false);
//        config.setTransparentRedValue(-1);
//        config.setTransparentGreenValue(-1);
//        config.setTransparentBlueValue(-1);
//        config.setTransparentAlphaValue(-1);


//        config.setHardwareAccelerated(true);


        config.setStencilBits(1);
//        config.setAlphaBits(8);
//        config.setAccumAlphaBits(8);
//        config.setAccumRedBits(8);
//        config.setAccumGreenBits(8);
//        config.setAccumBlueBits(8);
        return config;
    }


//    protected World2D getWorld() {
//        return model != null ? model.getCurrTest().getWorld() : world;
//    }

    public void off() {
        GLWindow w = this.window;
        if (w != null)
            Loop.invokeLater(w::destroy);
    }


    public final void pre(Consumer<JoglWindow> beforeNextRender) {
        preRenderTasks.add(beforeNextRender);
    }

    abstract protected void init(GL2 gl);

    public void printHardware() {
        //System.err.print("GL Profile: ");
        //System.err.println(GLProfile.getProfile());
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

    @Override
    public void dispose(GLAutoDrawable arg0) {
    }

    @Override
    public void windowResized(WindowEvent windowEvent) {
    }

    @Override
    public void windowMoved(WindowEvent windowEvent) {
    }

    @Override
    public void windowDestroyNotify(WindowEvent windowEvent) {
        renderer.stop();
        updater.stop();
    }

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        windows.remove(this);
        //window = null;
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

    public boolean next() {
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
        rendering.set(true);
        try {
            long nowMS = System.currentTimeMillis(), renderDTMS = nowMS - lastRenderMS;
            if (renderDTMS > Integer.MAX_VALUE) renderDTMS = Integer.MAX_VALUE;
            this.lastRenderMS = nowMS;

            render((int) renderDTMS);
        } finally {
            rendering.set(false);
        }
    }

    public void show(int w, int h) {
        show("", w, h);
    }

    public void show(String title, int w, int h, int x, int y) {

        Loop.timer().execute(() -> {

            if (window != null) {
                //TODO apply w,h,x,y to the existing window
                return;
            }

            GLWindow W = window();
            this.window = W;

            window.getScreen().getDisplay().getEDTUtil().setPollPeriod(EDT_POLL_PERIOD_MS);


            windows.add(this);

            window.addGLEventListener(this);
            window.addWindowListener(this);


            //W.getScreen().getDisplay().getEDTUtil().invoke(false, ()->{
            W.setTitle(title);
            setSize(w, h);
            if (x != Integer.MIN_VALUE) {
                setPosition(x, y);
            }

            setVisible(true);

        });

        Thread.yield();

        //HACK block until GL initialized
        while (gl == null) {
            Util.sleep(10);
        }

    }

    private void setVisible(boolean b) {
        if (window == null || window.isVisible() != b)
            pre((s) -> s.window.setVisible(b));
    }

    public void setPosition(int x, int y) {

        //TODO buffer subsequent setPositions only applying the final one on the EDT before frame render
        pre((s) -> {
            if (s.window.getX() != x || s.window.getY() != y) {
                s.window.setPosition(x, y);
            }
        });

    }

    public void setSize(int w, int h) {

        //TODO buffer subsequent setSize only applying the final one on the EDT before frame render
        if (window == null || window.getWidth() != w || window.getHeight() != h) {
            pre((s) -> s.window.setSize(w, h));
        }
    }

    @Override
    public final void init(GLAutoDrawable drawable) {

        GL2 gl = drawable.getGL().getGL2();

        if (gl.getGLProfile().isHardwareRasterizer()) {
            //gl.setSwapInterval(0); //0=disable vsync
            gl.setSwapInterval(1); //enabled vsync
        } else {
            gl.setSwapInterval(4); //reduce CPU strain
        }

        //printHardware();

        renderer.add(window);

        Draw.init(gl);

        init(gl);

        this.gl = window.getGL().getGL2();

        updater.runFPS(updateFPS);

    }

    public void setFPS(float render, float update) {
        //synchronized (this) {
        logger.info("fps render={} update={}", render, update);
        renderFPS = render;
        updateFPS = update;
        if (updater.isRunning()) {
            renderer.loop.runFPS(renderFPS);
            updater.runFPS(updateFPS);
        }
        //}
    }

    public void show(String title, int w, int h) {
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
        window.addKeyListener(m);
    }

//    public GL2 gl() {
//        return gl;
//    }

    public On onUpdate(Consumer<JoglWindow> c) {
        return onUpdate.on(c);
    }

    /**
     * adapter
     */
    public On onUpdate(Animated c) {
        return onUpdate.on((JoglWindow s) -> {
            c.animate(dtS);
        });
    }


    /* from: Jake2's */
    class GameAnimatorControl extends AnimatorBase {
        //        final FPSCounterImpl fpsCounter;
        public final Loop loop;
        private volatile boolean paused = true;

        GameAnimatorControl() {
            super();

            setIgnoreExceptions(false);
            setPrintExceptions(false);

            //setExclusiveContext(true);

//            fpsCounter = new FPSCounterImpl();
//            final boolean isARM = Platform.CPUFamily.ARM == Platform.getCPUFamily();
//            fpsCounter.setUpdateFPSFrames(isARM ? 60 : 4 * 60, System.err);
            this.loop = new Loop() {

                @Override
                protected void onStart() {
                    paused = false;
                }

                @Override
                public boolean next() {

                    if (window == null)
                        return true; //not ready yet

                    preRenderTasks.removeIf(r -> {
                        r.accept(JoglWindow.this);
                        return true;
                    });

                    if (!drawablesEmpty && !paused) { // RUN

                        try {
//                            //for debug
//                            if (window.isSurfaceLockedByOtherThread()) {
//                                Thread surfaceLockOwner = window.getSurfaceLockOwner();
//                                System.err.println(
//                                   "surface locked by: " + surfaceLockOwner + "\n" +
//                                   Joiner.on('\n').join(surfaceLockOwner.getStackTrace())
//                                );
//                            }

                            //if (!window.isSurfaceLockedByOtherThread())
                            impl.display(drawables, ignoreExceptions, printExceptions);
                        } catch (final UncaughtAnimatorException dre) {
                            //quitIssued = true;
                            dre.printStackTrace();
                        }
                    }
                    /*else if (pauseIssued && !quitIssued) { // PAUSE
//                        if (DEBUG) {
//                            System.err.println("FPSAnimator pausing: " + alreadyPaused + ", " + Thread.currentThread() + ": " + toString());
//                        }
                        //this.cancel();

//                        if (!alreadyPaused) { // PAUSE
//                            alreadyPaused = true;
                        if (exclusiveContext && !drawablesEmpty) {
                            setDrawablesExclCtxState(false);
                            try {
                                display(); // propagate exclusive context -> off!
                            } catch (final UncaughtAnimatorException dre) {
                                dre.printStackTrace();
                                //quitIssued = true;
//                                    stopIssued = true;
                            }
                        }
//                        if (null == caughtException) {
//                            synchronized (GameAnimatorControl.this) {
//                                if (DEBUG) {
//                                    System.err.println("FPSAnimator pause " + Thread.currentThread() + ": " + toString());
//                                }
//                                isAnimating = false;
//                                GameAnimatorControl.this.notifyAll();
//                            }
//                        }
                    }*/
                    return true;

                }
            };


//                    if (stopIssued) { // STOP incl. immediate exception handling of 'displayCaught'
//                        if (DEBUG) {
//                            System.err.println("FPSAnimator stopping: " + alreadyStopped + ", " + Thread.currentThread() + ": " + toString());
//                        }
//                        this.cancel();
//
//                        if (!alreadyStopped) {
//                            alreadyStopped = true;
//                            if (exclusiveContext && !drawablesEmpty) {
//                                setDrawablesExclCtxState(false);
//                                try {
//                                    display(); // propagate exclusive context -> off!
//                                } catch (final UncaughtAnimatorException dre) {
//                                    if (null == caughtException) {
//                                        caughtException = dre;
//                                    } else {
//                                        System.err.println("FPSAnimator.setExclusiveContextThread: caught: " + dre.getMessage());
//                                        dre.printStackTrace();
//                                    }
//                                }
//                            }
//                            boolean flushGLRunnables = false;
//                            boolean throwCaughtException = false;
//                            synchronized (FPSAnimator.this) {
//                                if (DEBUG) {
//                                    System.err.println("FPSAnimator stop " + Thread.currentThread() + ": " + toString());
//                                    if (null != caughtException) {
//                                        System.err.println("Animator caught: " + caughtException.getMessage());
//                                        caughtException.printStackTrace();
//                                    }
//                                }
//                                isAnimating = false;
//                                if (null != caughtException) {
//                                    flushGLRunnables = true;
//                                    throwCaughtException = !handleUncaughtException(caughtException);
//                                }
//                                animThread = null;
//                                GameAnimatorControl.this.notifyAll();
//                            }
//                            if (flushGLRunnables) {
//                                flushGLRunnables();
//                            }
//                            if (throwCaughtException) {
//                                throw caughtException;
//                            }
//                        }
//
//                        //if (impl!=null && !drawablesEmpty)
//                        //  display();
//                        return true;

            loop.runFPS(renderFPS);

            //setDrawablesExclCtxState(exclusiveContext); // may re-enable exclusive context
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
            //quitIssued = true;
            pause();
            loop.stop();
            return true;
        }


        @Override
        public final boolean pause() {
//            if( DEBUG ) {
//                System.err.println("GLCtx Pause Anim: "+Thread.currentThread().getName());
//                Thread.dumpStack();
//            }
            paused = true;
            return true;
        }

        @Override
        public final boolean resume() {
            paused = false;
            return true;
        }

        @Override
        public final boolean isStarted() {
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

//
//    public void reshape2D(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
//        float width = getWidth();
//        float height = getHeight();
//
//        GL2 gl2 = arg0.getGL().getGL2();
//
//        gl2.glMatrixMode(GL_PROJECTION);
//        gl2.glLoadIdentity();
//
//        // coordinate system origin at lower left with width and height same as the window
//        GLU glu = new GLU();
//        glu.gluOrtho2D(0.0f, width, 0.0f, height);
//
//
//        gl2.glMatrixMode(GL_MODELVIEW);
//        gl2.glLoadIdentity();
//
//        gl2.glViewport(0, 0, getWidth(), getHeight());
//
//    }


}
//    private static class MyFPSAnimator extends FPSAnimator {
//
//        int idealFPS, minFPS;
//        float lagTolerancePercentFPS = 0.05f;
//
//        public MyFPSAnimator(int idealFPS, int minFPS, int updateEveryNFrames) {
//            super(idealFPS);
//
//            setIgnoreExceptions(true);
//            setPrintExceptions(false);
//
//            this.idealFPS = idealFPS;
//            this.minFPS = minFPS;
//
//            setUpdateFPSFrames(updateEveryNFrames, new PrintStream(new OutputStream() {
//
//                @Override
//                public void write(int b) {
//                }
//
//                long lastUpdate;
//
//                @Override
//                public void flush() {
//                    long l = getLastFPSUpdateTime();
//                    if (lastUpdate == l)
//                        return;
//                    updateFPS();
//                    lastUpdate = l;
//                }
//
//            }, true));
//
//        }
//
//
//        protected void updateFPS() {
//            //logger.info("{}", MyFPSAnimator.this);
//
//            int currentFPS = getFPS();
//            float lastFPS = getLastFPS();
//            float lag = currentFPS - lastFPS;
//
//            float error = lag / currentFPS;
//
//            float nextFPS = Float.NaN;
//
//            if (error > lagTolerancePercentFPS) {
//                if (currentFPS > minFPS) {
//                    //decrease fps
//                    nextFPS = Util.lerp(0.1f, currentFPS, minFPS);
//                }
//            } else {
//                if (currentFPS < idealFPS) {
//                    //increase fps
//                    nextFPS = Util.lerp(0.1f, currentFPS, idealFPS);
//                }
//            }
//
//            int inextFPS = Math.max(1, Math.round(nextFPS));
//            if (nextFPS == nextFPS && inextFPS != currentFPS) {
//                //stop();
//                logger.debug("animator rate change from {} to {} fps because currentFPS={} and lastFPS={} ", currentFPS, inextFPS, currentFPS, lastFPS);
//
//                Thread x = animThread; //HACK to make it think it's stopped when we just want to change the FPS value ffs!
//                animThread = null;
//
//                setFPS(inextFPS);
//                animThread = x;
//
//                //start();
//            }
//
////            if (logger.isDebugEnabled()) {
////                if (!meters.isEmpty()) {
////                    meters.forEach((m, x) -> {
////                        logger.info("{} {}ms", m, ((JoglPhysics) m).frameTimeMS.mean());
////                    });
////                }
////            }
//        }
//
//
//    }

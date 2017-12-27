package spacegraph.render;

import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import jcog.Util;
import jcog.exe.Loop;
import jcog.list.FastCoWList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;


public abstract class JoglSpace implements GLEventListener, WindowListener {


    final static int RENDER_FPS_IDEAL = 30;
    final static int UPDATE_FPS_IDEAL = 15;

    //protected static final MyFPSAnimator a = new MyFPSAnimator(JoglSpace.FPS_IDEAL, FPS_MIN, FPS_IDEAL);
    protected static final GameAnimatorControl a;

    private static final Loop u;
    private volatile boolean ready = true;

    static {
//        GLCapabilitiesImmutable cfg = newDefaultConfig();
//        sharedDrawable = GLDrawableFactory.getFactory(cfg.getGLProfile()).createDummyAutoDrawable(null, true, cfg, null);
//        sharedDrawable.display(); // triggers GLContext object creation and native realization.
//        Draw.init(sharedDrawable.getGL().getGL2());
        a = new GameAnimatorControl(RENDER_FPS_IDEAL);
        u = new Loop(UPDATE_FPS_IDEAL) {
            @Override public boolean next() {
                windows.forEach(JoglSpace::updateIfReady);
//                windows.forEach(w->{
//                    w.window.getScreen().getDisplay().getEDTUtil().invoke(true, w::update);
//                });
                return true;
            }
        };
    }

    public static final GLU glu = new GLU();
    public final static GLSRT glsrt = new GLSRT(JoglSpace.glu);
    public static final GLUT glut = new GLUT();

    public GLWindow window;
    protected GL2 gl;

    boolean visible;

    protected JoglSpace() {
        super();
        //frameTimeMS = new PeriodMeter(toString(), 8);
    }

    static GLWindow window(JoglSpace j) {
        return window(config(), j);
    }

    static GLWindow window(GLCapabilitiesImmutable config, JoglSpace j) {



        GLWindow w = GLWindow.create(config);
        w.addGLEventListener(j);
        w.addWindowListener(j);

        //w.setSharedContext(sharedDrawable.getContext());


        return w;
    }

    static final Logger logger = LoggerFactory.getLogger(JoglSpace.class);

    private static final FastCoWList<JoglSpace> windows = new FastCoWList<>(16, JoglSpace[]::new);

    private static void start(JoglSpace j) {

//        if (!windows.isEmpty()) {
//        } else {
//            //a.start();
//
//        }
        GLWindow w = j.window;
        windows.add(j);

        w.addWindowListener(new WindowAdapter() {

            @Override
            public void windowDestroyed(WindowEvent e) {
                if (windows.remove(j)) {
                    //synchronized (a) {
                    a.remove(w);
                }
//                        boolean nowEmpty = windows.isEmpty();
//
//                        if (nowEmpty) {
//                            a.pause();
//                            logger.info("PAUSE {}", a);
//                        }
//                    }
//                }
            }
        });

        a.add(w);
    }



    @Override
    public final void init(GLAutoDrawable drawable) {
        this.window = ((GLWindow) drawable);

        this.gl = drawable.getGL().getGL2();


        if (gl.getGLProfile().isHardwareRasterizer()) {
            gl.setSwapInterval(0); //0=disable vsync
        } else {
            gl.setSwapInterval(4); //reduce CPU strain
        }

        //printHardware();

        Draw.init(gl);

        init(gl);
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

    static GLCapabilitiesImmutable config() {


        GLCapabilities config = new GLCapabilities(

                //GLProfile.getMinimum(true)
                GLProfile.getDefault()
                //GLProfile.getMaximum(true)


        );



//        config.setBackgroundOpaque(false);
//        config.setTransparentRedValue(-1);
//        config.setTransparentGreenValue(-1);
//        config.setTransparentBlueValue(-1);
//        config.setTransparentAlphaValue(-1);


//        config.setHardwareAccelerated(true);


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

    }

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {

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

    abstract protected void update();

    /** dtMS - time transpired since last call (millisecons)
     * @param dtMS*/
    abstract protected void render(int dtMS);

    protected final void updateIfReady() {
        if (ready) {
            ready = false;
            update();
        }
    }

    long lastFrameStartMS = System.currentTimeMillis();

    @Override
    public final void display(GLAutoDrawable drawable) {

        long nowMS = System.currentTimeMillis();
        long dtMS = nowMS - lastFrameStartMS;
        if (dtMS > Integer.MAX_VALUE) dtMS = Integer.MAX_VALUE;
        this.lastFrameStartMS = nowMS;

        render((int)dtMS);
        ready = true;

        //long now = System.currentTimeMillis();
        //frameTimeMS.hit(now - start);

    }


    public GLWindow show(int w, int h) {
        return show("", w, h);
    }

    public GLWindow show(String title, int w, int h, int x, int y) {

        if (window != null) {
            //TODO apply w,h,x,y to the existing window
            return window;
        }

        GLWindow g = window(this);
        g.setTitle(title);
        g.setDefaultCloseOperation(WindowClosingProtocol.WindowClosingMode.DISPOSE_ON_CLOSE);
        g.preserveGLStateAtDestroy(false);
        g.setSize(w, h);
        if (x != Integer.MIN_VALUE) {
            g.setPosition(x, y);
        }
        g.setVisible(true);

        start(this);

        return this.window = g;
    }

    public GLWindow show(String title, int w, int h) {
        return show(title, w, h, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public void addMouseListener(MouseListener m) {
        window.addMouseListener(m);
    }

    public void addWindowListener(WindowListener m) {
        window.addWindowListener(m);
    }

    public void addKeyListener(KeyListener m) {
        window.addKeyListener(m);
    }

    public GL2 gl() {
        return gl;
    }


    /* from: Jake2's */
    public static class GameAnimatorControl extends AnimatorBase {
//        final FPSCounterImpl fpsCounter;
        private final Loop loop;

        //private boolean pauseIssued;
        //private boolean quitIssued;
        public boolean isAnimating;
        private boolean paused = false;

        GameAnimatorControl(float initialFPS) {
            super();

            setIgnoreExceptions(false);
            setPrintExceptions(false);

//            fpsCounter = new FPSCounterImpl();
//            final boolean isARM = Platform.CPUFamily.ARM == Platform.getCPUFamily();
//            fpsCounter.setUpdateFPSFrames(isARM ? 60 : 4 * 60, System.err);
            this.loop = new Loop(-1) {


                {
                    setExclusiveContext(animThread);
                }

                @Override
                protected void onStart() {
                    isAnimating = true;
                }

                @Override
                public boolean next() {

                    if (!drawablesEmpty && !paused) { // RUN
                        try {
                            display();
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




            loop.runFPS(initialFPS);
            setDrawablesExclCtxState(exclusiveContext); // may re-enable exclusive context

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
        public final synchronized boolean isStarted() {
            //return null != window;
            return true;
        }

        @Override
        public final boolean isAnimating() {
            return isAnimating;
        }

        @Override
        public final boolean isPaused() {
            return paused;
        }


    }

    private static class MyFPSAnimator extends FPSAnimator {

        int idealFPS, minFPS;
        float lagTolerancePercentFPS = 0.05f;

        public MyFPSAnimator(int idealFPS, int minFPS, int updateEveryNFrames) {
            super(idealFPS);

            setIgnoreExceptions(true);
            setPrintExceptions(false);

            this.idealFPS = idealFPS;
            this.minFPS = minFPS;

            setUpdateFPSFrames(updateEveryNFrames, new PrintStream(new OutputStream() {

                @Override
                public void write(int b) {
                }

                long lastUpdate;

                @Override
                public void flush() {
                    long l = getLastFPSUpdateTime();
                    if (lastUpdate == l)
                        return;
                    updateFPS();
                    lastUpdate = l;
                }

            }, true));

        }


        protected void updateFPS() {
            //logger.info("{}", MyFPSAnimator.this);

            int currentFPS = getFPS();
            float lastFPS = getLastFPS();
            float lag = currentFPS - lastFPS;

            float error = lag / currentFPS;

            float nextFPS = Float.NaN;

            if (error > lagTolerancePercentFPS) {
                if (currentFPS > minFPS) {
                    //decrease fps
                    nextFPS = Util.lerp(0.1f, currentFPS, minFPS);
                }
            } else {
                if (currentFPS < idealFPS) {
                    //increase fps
                    nextFPS = Util.lerp(0.1f, currentFPS, idealFPS);
                }
            }

            int inextFPS = Math.max(1, Math.round(nextFPS));
            if (nextFPS == nextFPS && inextFPS != currentFPS) {
                //stop();
                logger.debug("animator rate change from {} to {} fps because currentFPS={} and lastFPS={} ", currentFPS, inextFPS, currentFPS, lastFPS);

                Thread x = animThread; //HACK to make it think it's stopped when we just want to change the FPS value ffs!
                animThread = null;

                setFPS(inextFPS);
                animThread = x;

                //start();
            }

//            if (logger.isDebugEnabled()) {
//                if (!meters.isEmpty()) {
//                    meters.forEach((m, x) -> {
//                        logger.info("{} {}ms", m, ((JoglPhysics) m).frameTimeMS.mean());
//                    });
//                }
//            }
        }


    }

    // See http://www.lighthouse3d.com/opengl/glut/index.php?bmpfontortho
    protected void ortho() {
        int w = getWidth();
        int h = getHeight();
        gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();

        //gl.glOrtho(-2.0, 2.0, -2.0, 2.0, -1.5, 1.5);
        gl.glOrtho(0, w, 0, h, -1.5, 1.5);

//        // switch to projection mode
//        gl.glMatrixMode(gl.GL_PROJECTION);
//        // save previous matrix which contains the
//        //settings for the perspective projection
//        // gl.glPushMatrix();
//        // reset matrix
//        gl.glLoadIdentity();
//        // set a 2D orthographic projection
//        glu.gluOrtho2D(0f, screenWidth, 0f, screenHeight);
//        // invert the y axis, down is positive
//        //gl.glScalef(1f, -1f, 1f);
//        // mover the origin from the bottom left corner
//        // to the upper left corner
//        //gl.glTranslatef(0f, -screenHeight, 0f);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        //gl.glLoadIdentity();


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

/*
 * NEWTWin.java
 * Copyright (C) 2004
 * 
 */
package jake2.render.opengl;

import com.jogamp.common.os.Platform;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.SurfaceSize;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.*;
import jake2.Defines;
import jake2.client.VID;
import jake2.game.cvar_t;
import jake2.qcommon.Cbuf;
import jake2.qcommon.Cvar;
import jake2.render.Base;
import jake2.sys.NEWTKBD;
import jogamp.opengl.FPSCounterImpl;

import java.io.PrintStream;
import java.util.List;

public class NEWTWin {
    final static boolean DEBUG = false;
    /** Required due to AWT lock of surface, if Applet! */
    final static boolean FORCE_RELEASE_CTX_VAL = true; 
    
    MonitorMode oldDisplayMode;
    volatile Screen screen;
    volatile GLWindow window;
    volatile GameAnimatorControl animCtrl;
    /** Encapsulateed AWT dependency */
    public volatile Object canvasObj;
    boolean forceReleaseCtx;
    volatile boolean shouldQuit;
    volatile boolean shouldPause;
    volatile boolean shouldReparent;
    volatile boolean isAnimating;

    public List<MonitorMode> getModeList() {
        if( null != window ) {
            final MonitorDevice mainMonitor = window.getMainMonitor();
            return mainMonitor.getSupportedModes();
        } else {        
            return screen.getMonitorModes();
        }
    }

    public MonitorMode findDisplayMode(DimensionImmutable dim) {
        final List<MonitorMode> sml = MonitorModeUtil.filterByResolution(getModeList(), dim);
        if(sml.size() == 0) {
            return oldDisplayMode;
        }
        return sml.get(0);
    }

    public static String getModeString(MonitorMode mm) {
        final SurfaceSize ss = mm.getSurfaceSize();
        final DimensionImmutable m = ss.getResolution();
        final StringBuilder sb = new StringBuilder();
        sb.append(m.getWidth());
        sb.append('x');
        sb.append(m.getHeight());
        sb.append('x');
        sb.append(ss.getBitsPerPixel());
        sb.append('@');
        sb.append(mm.getRefreshRate());
        sb.append("Hz");
        return sb.toString();
    }

    /**
     * @param dim
     * @param mode
     * @param fullscreen
     * @param driverName
     * @return enum Base.rserr_t
     */
    public int setMode(GLProfile glp, Dimension dim, int mode, boolean fullscreen, String driverName) {
        final Dimension newDim = new Dimension();

        VID.Printf(Defines.PRINT_ALL, "Initializing OpenGL display for profile "+glp+ '\n');

        if(null == screen) {
            screen = NewtFactory.createScreen(NewtFactory.createDisplay(null), 0);
            screen.addReference(); 
        } else if( !screen.isNativeValid() ) {
            screen.addReference(); 
        }
        
        if (!VID.GetModeInfo(newDim, mode)) {
            VID.Printf(Defines.PRINT_ALL, " invalid mode\n");
            return Base.rserr_invalid_mode;
        }

        VID.Printf(Defines.PRINT_ALL, "...setting mode " + mode + ", " + newDim.getWidth() + " x " + newDim.getHeight() + ", fs " + fullscreen + ", driver " + driverName + '\n');

        
        shutdownImpl(false);
        
        if(null != window) {
            throw new InternalError("XXX");            
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        CapabilitiesChooser chooser = null; 
        final cvar_t v = Cvar.Get("jogl_rgb565", "0", 0);
        if( v.value != 0f ) {
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            chooser = new GenericGLCapabilitiesChooser(); 
        }

        window = GLWindow.create(screen, caps);
        window.setAutoSwapBufferMode(false);
        window.setDefaultCloseOperation(WindowClosingProtocol.WindowClosingMode.DO_NOTHING_ON_CLOSE); 
        window.setCapabilitiesChooser(chooser);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                shouldQuit = null != window; 
            }

            @Override
            public void windowResized(WindowEvent e) {
                propagateNewSize();
            }
        });
        window.setTitle("Jake2 ("+driverName+"-newt-"+glp.getName().toLowerCase()+ ')');
        
        animCtrl = new GameAnimatorControl();
        window.setAnimator(animCtrl);

        final MonitorDevice mainMonitor = window.getMainMonitor();
        
        if (oldDisplayMode == null) {
            oldDisplayMode = mainMonitor.getCurrentMode();
        }

        
        NEWTKBD.Init(window);
        
        window.addWindowListener(NEWTKBD.listener);
        window.addKeyListener(NEWTKBD.listener);
        window.addMouseListener(NEWTKBD.listener);
        window.setSize(newDim.getWidth(), newDim.getHeight());
        
        isAnimating = true; 

        














































        forceReleaseCtx = false;
        canvasObj = null;

        if (fullscreen) {
            MonitorMode mm = findDisplayMode(newDim);
            final DimensionImmutable smDim = mm.getSurfaceSize().getResolution();
            newDim.setWidth( smDim.getWidth() );
            newDim.setHeight( smDim.getHeight() );
            mainMonitor.setCurrentMode(mm);
            VID.Printf(Defines.PRINT_ALL, "...MonitorMode "+mm+'\n');
            window.setFullscreen(true);
        }

        window.setVisible(true);
        window.requestFocus();
        if( !window.isNativeValid()|| !window.isRealized() ) {
            throw new RuntimeException("NEWT window didn't not realize: "+window);
        }
        window.display(); 
        final GLContext ctx = window.getContext();
        if( !ctx.isCreated() ) {
            System.err.println("Warning: GL context not created: "+ctx);
        }
        if( ctx.isCurrent() ) {
            throw new RuntimeException("NEWT GL context still current: "+ctx);
        }

        VID.Printf(Defines.PRINT_ALL, "...reques GLCaps "+window.getRequestedCapabilities()+'\n');
        VID.Printf(Defines.PRINT_ALL, "...chosen GLCaps "+window.getChosenGLCapabilities()+'\n');
        VID.Printf(Defines.PRINT_ALL, "...size "+window.getWidth()+" x "+window.getHeight()+'\n');

        
        activateGLContext(true);
        
        return Base.rserr_ok;
    }
    
    private void propagateNewSize() {
        if( null != window ) {
            final int width = window.getWidth();
            final int height = window.getHeight();
            final int _width;
            final int mask = ~0x03;
            if ((width & 0x03) != 0) {
                _width = ( width & mask ) + 4;
            } else {
                _width = width;
            }
            VID.Printf(Defines.PRINT_ALL, "Resize: " + width + " x " + height + ", masked " + _width + 'x' + height + '\n');
    
            Base.setVid(_width, height);
            
            VID.NewWindow(_width, height);
        }
    }

    protected final boolean activateGLContext(boolean force) {
        boolean ctxCurrent = false;
        if( force || !shouldPause ) {
            final GLContext ctx = window.getContext();
            if ( null != ctx && GLContext.getCurrent() != ctx ) {
                if( DEBUG ) {
                    System.err.println("GLCtx Current pause "+shouldPause+": "+Thread.currentThread().getName());
                }
                ctxCurrent = GLContext.CONTEXT_NOT_CURRENT < ctx.makeCurrent();
            } else {
                ctxCurrent = true;
            }
            isAnimating = ctxCurrent;
        }
        return ctxCurrent;
    }
    
    protected final void deactivateGLContext() {
        final GLContext ctx = window.getContext();
        if ( null != ctx && GLContext.getCurrent() == ctx) {
            if( DEBUG ) {
                System.err.println("GLCtx Release pause "+shouldPause+": "+Thread.currentThread().getName());
            }
            ctx.release();
        }        
    }
    
    /** 
     * Performs {@link GLWindow#swapBuffers()}, ticks the fps counter and performs <code>QUIT</code> if requested. 
     */
    public final void endFrame() {
        window.swapBuffers();

        animCtrl.fpsCounter.tickFPS();
        if( shouldQuit ) {
            deactivateGLContext();
            Cbuf.ExecuteText(Defines.EXEC_APPEND, "stop");
        } else if( shouldReparent  ) {
            shouldReparent  = false;
            deactivateGLContext();
            if( null != canvasObj && null != window ) {
                isAnimating = false; 

                final NativeWindow NativeSurface = (NativeWindow) canvasObj;
                if(null == window.getParent()) {
                    forceReleaseCtx = FORCE_RELEASE_CTX_VAL; 
                    window.reparentWindow( NativeSurface, 0, 0, 0 );
                } else {
                    window.reparentWindow(null, 0, 0, 0);
                    forceReleaseCtx = false;
                }
            }            
        } else if( forceReleaseCtx || shouldPause ) {
            deactivateGLContext();
        }
    }
    
    /** Performs <code>QUIT</code> if requested. */
    public final void checkQuit() {
        if( shouldQuit ) {
            deactivateGLContext();
            Cbuf.ExecuteText(Defines.EXEC_APPEND, "stop");
        }
    }
    
    void shutdown() {
        shutdownImpl(true);
    }
    
    private void shutdownImpl(boolean withScreen) {
        if ( null != window ) {
            deactivateGLContext();
            final GLWindow _window = window;
            window = null;
            _window.destroy();




















        }
        if( withScreen && null != screen ) {
            try {
                screen.destroy();
            } catch (Throwable e) {
                System.err.println("Catched "+e.getClass().getName()+": "+e.getMessage());
                e.printStackTrace();
            }
            screen = null;
        }
    }    
    
    class GameAnimatorControl implements GLAnimatorControl {
        final FPSCounterImpl fpsCounter;
        final Thread thread;
        
        GameAnimatorControl() {
            final boolean isARM = Platform.CPUFamily.ARM == Platform.getCPUFamily();
            fpsCounter = new FPSCounterImpl();
            fpsCounter.setUpdateFPSFrames(isARM ? 60 : 4*60, System.err);
            thread = Thread.currentThread();
        }
        
        @Override
        public final boolean start() {
            return false;
        }

        @Override
        public final boolean stop() {
            shouldQuit = true;
            return true;
        }

        @Override
        public final boolean pause() {
            if( DEBUG ) {
                System.err.println("GLCtx Pause Anim: "+Thread.currentThread().getName());
                Thread.dumpStack();
            }
            shouldPause = true;
            return true;
        }

        @Override
        public final boolean resume() {
            shouldPause = false;
            return true;
        }



        @Override
        public final boolean isStarted() {
            return null != window;
        }

        @Override
        public final boolean isAnimating() {
            return isAnimating; 
        }

        @Override
        public final boolean isPaused() {
            return null == window || shouldPause;
        }

        @Override
        public final Thread getThread() {
            return thread;
        }
        
        @Override
        public final void add(GLAutoDrawable drawable) {}

        @Override
        public final void remove(GLAutoDrawable drawable) {}

        @Override
        public UncaughtExceptionHandler getUncaughtExceptionHandler() {
            return null;
        }

        @Override
        public void setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {

        }

        @Override
        public final void setUpdateFPSFrames(int frames, PrintStream out) {
            fpsCounter.setUpdateFPSFrames(frames, out);
        }

        @Override
        public final void resetFPSCounter() {
            fpsCounter.resetFPSCounter();
        }

        @Override
        public final int getUpdateFPSFrames() {
            return fpsCounter.getUpdateFPSFrames();
        }

        @Override
        public final long getFPSStartTime() {
            return fpsCounter.getFPSStartTime();
        }

        @Override
        public final long getLastFPSUpdateTime() {
            return fpsCounter.getLastFPSUpdateTime();
        }

        @Override
        public final long getLastFPSPeriod() {
            return fpsCounter.getLastFPSPeriod();
        }

        @Override
        public final float getLastFPS() {
            return fpsCounter.getLastFPS();
        }

        @Override
        public final int getTotalFPSFrames() {
            return fpsCounter.getTotalFPSFrames();
        }

        @Override
        public final long getTotalFPSDuration() {
            return fpsCounter.getTotalFPSDuration();
        }

        @Override
        public final float getTotalFPS() {
            return fpsCounter.getTotalFPS();
        }
    }
    
    class ReparentKeyListener implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
           System.err.println(e);
           if( !e.isAutoRepeat() ) {
               int keyCode = e.getKeyCode();
               if( KeyEvent.VK_HOME == keyCode ) {
                   shouldReparent = true;
               }
           }
        }
        @Override
        public void keyReleased(KeyEvent e) { 
            System.err.println(e);
        }
    }
    
}

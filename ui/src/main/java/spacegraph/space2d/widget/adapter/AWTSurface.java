package spacegraph.space2d.widget.adapter;

import com.jogamp.newt.event.KeyEvent;
import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.Keyboard;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.util.AWTCamera;
import spacegraph.util.math.v2;
import spacegraph.video.Tex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.event.KeyEvent.VK_UNDEFINED;
import static java.awt.event.MouseEvent.NOBUTTON;

public class AWTSurface extends Widget {

    static Method processKeyEvent;

    static {
        try {
            processKeyEvent = Component.class.getDeclaredMethod("processKeyEvent", java.awt.event.KeyEvent.class);
            processKeyEvent.trySetAccessible();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

//    static {
//        System.setProperty("java.awt.headless", "true");
//    }

    Component component;
    private final Tex tex = new Tex();
    private final Dimension psize;
    BufferedImage buffer = null;
    int lpx = -1, lpy = -1;
    private On ons;
    private volatile Component myFocus;

    public AWTSurface(Component component) {
        this(component, component.getWidth(), component.getHeight());
    }

    public AWTSurface(Component component, int pw, int ph) {

        this.component = component;
        this.psize = new Dimension(pw, ph);

    }


    @Override
    public boolean start(@Nullable SurfaceBase parent) {
        if (super.start(parent)) {


            if (component instanceof JFrame) {
                component.setVisible(false);
                component = ((RootPaneContainer) component).getRootPane();
            }


            Window frame = new MyFrame();
//        frame.getContentPane().setLayout(new BorderLayout());
//        frame.getContentPane().add(component, BorderLayout.CENTER);
//        frame.add(component);
            //frame.setIgnoreRepaint(true);

//        frame.addNotify();
            component.addNotify();


            //frame.setIgnoreRepaint(true);
            component.setPreferredSize(psize);
            //component.setMaximumSize(psize);
            component.setSize(psize);
            frame.pack();

            component.setVisible(true);

            //component.requestFocus();

            component.validate();


            content(tex.view());


            AtomicBoolean busy = new AtomicBoolean(false);
            ons = root().onUpdate(w -> {

                if (!busy.compareAndSet(false, true))
                    return;

                //if (component.getIgnoreRepaint() && !component.is)
                SwingUtilities.invokeLater(()->{
                    try {
                        buffer = AWTCamera.get(component, buffer);
                        tex.update(buffer);
                    } finally {
                        busy.set(false);
                    }
                });

            });

            return true;
        }

        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            ons.off();
            ons = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean tryKey(KeyEvent e, boolean pressed) {
        int code = Keyboard.newtKeyCode2AWTKeyCode(e.getKeyCode());

        /*
         * @param source    The {@code Component} that originated the event
         * @param id              An integer indicating the type of event.
         *                  For information on allowable values, see
         *                  the class description for {@link KeyEvent}
         * @param when      A long integer that specifies the time the event
         *                  occurred.
         *                     Passing negative or zero value
         *                     is not recommended
         * @param modifiers The modifier keys down during event (shift, ctrl,
         *                  alt, meta).
         *                     Passing negative value
         *                     is not recommended.
         *                     Zero value means that no modifiers were passed.
         *                  Use either an extended _DOWN_MASK or old _MASK modifiers,
         *                  however do not mix models in the one event.
         *                  The extended modifiers are preferred for using
         * @param keyCode   The integer code for an actual key, or VK_UNDEFINED
         *                  (for a key-typed event)
         *  public KeyEvent(Component source, int id, long when, int modifiers, int keyCode) {
         */


        if (myFocus != null) {
            Component m = myFocus;
            int modifers = 0;
            Component src = component;
            if (pressed && e.isPrintableKey()) {

                try {
                    processKeyEvent.invoke(m, new java.awt.event.KeyEvent(src,
                            java.awt.event.KeyEvent.KEY_TYPED,
                            System.currentTimeMillis(),
                            modifers, VK_UNDEFINED, e.getKeyChar()
                    ));
                } catch (IllegalAccessException | InvocationTargetException e1) {
                    e1.printStackTrace();
                }
            }

            try {
                processKeyEvent.invoke(m, new java.awt.event.KeyEvent(src,
                        pressed ? java.awt.event.KeyEvent.KEY_PRESSED : java.awt.event.KeyEvent.KEY_RELEASED,
                        System.currentTimeMillis(),
                        modifers, code, e.getKeyChar()
                ));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

//        if (myFocus==null) {
//            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ee);
//        } else {
//            ((JTextComponent)myFocus).dispatchEvent(ee);
//            KeyboardFocusManager.getCurrentKeyboardFocusManager().
//                    processKeyEvent(myFocus, ee);
//        }

        return false;
    }


    @Override
    public void onFinger(@Nullable Finger finger) {
        boolean wasTouching = isTouched();


        long now = System.currentTimeMillis();
        if (finger == null) {
            if (wasTouching) {
                handle(new MouseEvent(component,
                        MouseEvent.MOUSE_EXITED,
                        now, 0,
                        lpx, lpy, lpx, lpy, 0, false, NOBUTTON
                ));
            }
            lpx = lpy = -1;
            return; //untouch //TODO mouseExited?
        }

        v2 rp = finger.relativePos(this);
        int px = Math.round(rp.x * component.getWidth());
        int py = Math.round((1f - rp.y) * component.getHeight());
        if (lpx == -1) {
            lpx = px;
            lpy = py;
        }
        if (!wasTouching) {
            handle(new MouseEvent(component,
                    MouseEvent.MOUSE_ENTERED,
                    now, 0,
                    px, py, px, py, 0, false, NOBUTTON)
            );
        }


        //SwingUtilities.convertPoint(this.frame, new Point(px, py), source);
        Component target = SwingUtilities.getDeepestComponentAt(this.component, px, py);
        if (target == null)
            target = this.component;
        else {
            px -= target.getX();
            py -= target.getY();
        }

        //        this(source, id, when, modifiers, x, y, clickCount, popupTrigger, NOBUTTON);

        if (finger.pressed(0) && !finger.prevButtonDown.get(0)) {
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_PRESSED,
                    now, InputEvent.BUTTON1_DOWN_MASK,
                    px, py, px, py, 0, false, MouseEvent.BUTTON1
            ));
        }
        if (!finger.pressed(0) && finger.prevButtonDown.get(0)) {
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_RELEASED,
                    now, InputEvent.BUTTON1_DOWN_MASK,
                    px, py, px, py, 0, false, MouseEvent.BUTTON1
            ));
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_CLICKED,
                    now, InputEvent.BUTTON1_DOWN_MASK,
                    px, py, px, py, 1, false, MouseEvent.BUTTON1
            ));
        }

        boolean moved = lpx != px || lpy != py;

        //TODO filter if no distance moved
        if (finger.pressed(0)) {
            //drag
            if (moved && finger.prevButtonDown.get(0)) {
                handle(new MouseEvent(target,
                        MouseEvent.MOUSE_DRAGGED,
                        now, 0,
                        px, py, px, py, 0, false, MouseEvent.BUTTON1
                ));
            }
            if (!finger.prevButtonDown.get(0) &&
                    target.isFocusable() && !target.hasFocus()) {


                myFocus = target;
                //target.requestFocus(FocusEvent.Cause.MOUSE_EVENT);
                //target.requestFocusInWindow(FocusEvent.Cause.MOUSE_EVENT);

            }
        } else {
            if (moved) {
                handle(new MouseEvent(target,
                        MouseEvent.MOUSE_MOVED,
                        now, 0,
                        px, py, px, py, 0, false, NOBUTTON
                ));
            }
        }

        //new MouseEvent(underCursor, id, when, modifiers, point.x, point.y, 0, 0, clickCount, false, button)
    }

    public void handle(AWTEvent e) {
//        component.dispatchEvent(e);
//        frame.dispatchEvent(e);
//        if (component instanceof Container) {
//            for (Component c : ((Container)component).getComponents()) {
//                c.dispatchEvent(e);
//            }
//        }
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
    }


    private static class MyFrame extends Window {
        public MyFrame() throws HeadlessException {
            super(null);
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public boolean isFocusable() {
            return true;
        }
        ////
////            @Override
////            public boolean isVisible() {
////                return true;
////            }
////
////            @Override
////            public boolean isDisplayable() {
////                return true;
////            }
//
//            @Override
//            protected void processEvent(AWTEvent e) {
//                super.processEvent(e);
//            }

//            Point zero = new Point(0, 0);
//
//            @Override
//            public Point getLocationOnScreen() {
//                return zero;
//            }

        @Override
        public boolean isShowing() {
            return true;
        }
    }

}


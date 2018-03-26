package spacegraph.widget.adapter;

import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.SurfaceBase;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.render.Tex;
import spacegraph.util.AWTCamera;
import spacegraph.widget.windo.Widget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AWTSurface extends Widget {

    final Component component;
    private final Tex tex;
    BufferedImage buffer = null;
    private On ons;

    public AWTSurface(Component component, int pw, int ph) {

        tex = new Tex();
        content(tex.view());

        JFrame frame = new JFrame() {
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

            Point zero = new Point(0,0);
            @Override
            public Point getLocationOnScreen() {
                return zero;
            }

            @Override
            public boolean isShowing() {
                return true;
            }
        };
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(component, BorderLayout.CENTER);


        this.component = component;


        component.requestFocus();

        component.setVisible(true);
        //component.setPreferredSize(new Dimension(pw, ph));
        Dimension psize = new Dimension(pw, ph);
        //frame.setIgnoreRepaint(true);
        component.setPreferredSize(psize);
        component.setMaximumSize(psize);
        component.setSize(psize);
        //frame.pack();


    }

    @Override
    public void start(@Nullable SurfaceBase parent) {
        synchronized(this) {
            super.start(parent);


            ons = root().onUpdate(w->{
                buffer = AWTCamera.get(component, buffer);
                tex.update(buffer);
            });

        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            ons.off();
            ons = null;
            super.stop();
        }
    }

    @Override
    public void touch(@Nullable Finger finger) {
        boolean wasTouching = isTouched();

        if (finger == null) {
            if (wasTouching) {
                handle(new MouseEvent(component,
                        MouseEvent.MOUSE_EXITED,
                        System.currentTimeMillis(), 0,
                        0, 0, 0, false
                ));
            }
            return; //untouch //TODO mouseExited?
        } else {
            if (!wasTouching) {
                handle(new MouseEvent(component,
                        MouseEvent.MOUSE_ENTERED,
                        System.currentTimeMillis(), 0,
                        0, 0, 0, false
                ));
            }
        }

        v2 rp = finger.relativePos(this);
        int px = Math.round(rp.x * component.getWidth());
        int py = Math.round((1f-rp.y) * component.getHeight());


        //SwingUtilities.convertPoint(this.frame, new Point(px, py), source);
        Component target = SwingUtilities.getDeepestComponentAt(this.component, px, py);
        if (target == null)
            target = this.component;
        else {
            px -= target.getX();
            py -= target.getY();
        }

        //        this(source, id, when, modifiers, x, y, clickCount, popupTrigger, NOBUTTON);

        if (finger.buttonDown[0] && !finger.prevButtonDown[0]) {
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                    px, py, 0,0,0, false, MouseEvent.BUTTON1
            ));
        }
        if (!finger.buttonDown[0] && finger.prevButtonDown[0]) {
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                    px, py, 0,0,0, false, MouseEvent.BUTTON1
            ));
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                    px, py, 0,0,1, false, MouseEvent.BUTTON1
            ));
        }

        //TODO filter if no distance moved
        if (finger.buttonDown[0]) {
            //drag
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(), 0,
                    px, py, 0, false, MouseEvent.BUTTON1
            ));
        } else {
            handle(new MouseEvent(target,
                    MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0,
                    px, py, 0, false
            ));
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
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                e);
    }

    public static void main(String[] args) throws IOException {
        //ui.setLayout(new BorderLayout());

        JPanel ui = new JPanel(new FlowLayout());
        ui.add(new JTextField("XYZ"));
        ui.add(new JSlider());
        ui.add(new JButton("XYZ"));
        ui.add(new JSlider());


//        JComboBox ui = new JComboBox();
//        ui.addItem("a");
//        ui.addItem("b");
//        ui.addItem("c");



        //Component ui = new JButton("XYZ");
//        Component ui = new JSlider(SwingConstants.VERTICAL);

        SpaceGraph.window(new AWTSurface(
                //new JColorChooser(),
                ui,
                        200, 200),
                400, 400);

        System.in.read();
    }
}


package spacegraph.input.finger.impl;

import com.jogamp.newt.event.*;
import jcog.math.v2;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.video.JoglDisplay;
import spacegraph.video.JoglWindow;

import java.util.function.Function;

/** ordinary desktop/laptop computer mouse, as perceived through jogamp NEWT's native interface */
public class NewtMouseFinger extends MouseFinger implements MouseListener, WindowListener {



    private final JoglDisplay space;


    public NewtMouseFinger(JoglDisplay s, Function<Finger,Surface> root) {
        super(MAX_BUTTONS);
        this.space = s;

        JoglWindow win = s.video;
        win.addMouseListenerPre(this);
        win.addWindowListener(this);
        if (win.window.hasFocus())
            focused.set(true);

        win.onUpdate(new Runnable() {
            @Override
            public void run() {
                NewtMouseFinger.this.finger(root);
            }
        });
    }


    private void updatePosition() {
        JoglWindow win = space.video;

        float pmx = posEvent.x, pmy = (float) win.getHeight() - posEvent.y;

        posPixel.set(pmx, pmy);

        posScreen.set((float) win.getX() + posEvent.x, win.getScreenH() - ((float) win.getY() + posEvent.y));
    }

    private void updateMoved(MouseEvent e) {
        posEvent.set((float) e.getX(), (float) e.getY());
        updatePosition();

        updateButtons(null);
    }

    private void updateOther(boolean moved) {
        updateButtons(null);
    }

    /** raw pixel coordinates from MouseEvent */
    private final v2 posEvent = new v2();



    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(@Nullable MouseEvent e) {
        if (focused.compareAndSet(false, true)) {
            enter();
            if (e != null)
                updateOther(false);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (focused.compareAndSet(true, false)) {
            updateOther(false);
            exit();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isConsumed())
            return;

        short[] bd = e.getButtonsDown();
        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) +(int) bd[i];

        updateButtons(e.getButtonsDown());

        if (touching() != null)
            e.setConsumed(true);

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isConsumed()) return;

        short[] bd = e.getButtonsDown();
        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) -(int) bd[i];

        updateButtons(bd);

        if (touching() != null)
            e.setConsumed(true);

    }


    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.isConsumed()) return;

        updateMoved( e);

        if (touching() != null)
            e.setConsumed(true);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //if (e.isConsumed()) return;

        updateMoved(e);
    }

    @Override
    public void windowResized(WindowEvent e) {

    }

    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {

    }

    @Override
    public void windowDestroyed(WindowEvent e) {

    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        if (focused.compareAndSet(false, true)) {
            enter();
            updateOther(false);
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        if (focused.compareAndSet(true, false)) {
            updateOther(false);
            exit();
        }
    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        if (e.isConsumed())
            return;
        rotationAdd(e.getRotation());
    }

    @Override
    protected void start(SpaceGraph x) {

    }

    @Override
    protected void stop(SpaceGraph x) {

    }
}

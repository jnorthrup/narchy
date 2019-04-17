package spacegraph.input.finger.impl;

import com.jogamp.newt.event.*;
import jcog.math.v2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.Surface;
import spacegraph.video.JoglDisplay;
import spacegraph.video.JoglWindow;

import java.util.function.Function;

/** ordinary desktop/laptop computer mouse, as perceived through jogamp NEWT's native interface */
public class NewtMouseFinger extends MouseFinger implements MouseListener, WindowListener {



    private final JoglDisplay space;
    private final Function<Finger,Surface> root;


    public NewtMouseFinger(JoglDisplay s, Function<Finger,Surface> root) {
        super(MAX_BUTTONS);
        this.space = s;
        this.root = root;

        JoglWindow win = s.video;
        win.addMouseListenerPre(this);
        win.addWindowListener(this);
        if (win.window.hasFocus())
            focused.set(true);

        win.onUpdate((Runnable) this::update);


    }

    /** called for each layer. returns true if continues down to next layer */
    public void finger(Function<Finger,Surface> s) {

        _posGlobal.set(posPixel); //HACK

        Fingering ff = this.fingering.get();

        Surface touchNext = s.apply(this);
        touching.accumulateAndGet(touchNext, ff::touchNext);

        commitButtons();
    }



    /** global position of the cursor center */
    @Override public v2 posGlobal() {
        //Function<v2,v2> z = this._screenToGlobal;
        //Function<v2,v2> z = _z !=null ? _z : (c instanceof Zoomed ? ((Zoomed)c) : c.parentOrSelf(Zoomed.class));
        return _posGlobal;
    }

    @Override protected void doUpdate() {

        finger(root);

        clearRotation();
    }

    private boolean update(boolean moved, MouseEvent e) {
        return update(moved, e, null);
    }

    private boolean update(boolean moved, MouseEvent e, @Nullable short[] buttonsDown) {

        JoglWindow win = space.video;

        if (moved) {

            int pmx = e.getX(), pmy = win.getHeight() - e.getY();

            posPixel.set(pmx, pmy);

            posScreen.set(win.getX() + pmx, win.getScreenH() - (win.getY() + e.getY()));
        }

        update(buttonsDown);

        return e != null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(@Nullable MouseEvent e) {
        if (focused.compareAndSet(false, true)) {
            enter();
            if (e != null)
                update(false, e);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (focused.compareAndSet(true, false)) {
            update(false, null);
            exit();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isConsumed())
            return;

        short[] bd = e.getButtonsDown();
        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) +bd[i];

        if (update(false, e, e.getButtonsDown())) {
            if (touching() != null)
                e.setConsumed(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isConsumed()) return;

        short[] bd = e.getButtonsDown();
        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) -bd[i];

        update(false, e, bd);

        if (touching() != null)
            e.setConsumed(true);

    }


    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.isConsumed()) return;

        if (update(true, e))
            if (touching() != null)
                e.setConsumed(true);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (e.isConsumed()) return;

        update(true, e);
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
            update(false, null);
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        if (focused.compareAndSet(true, false)) {
            update(false, null);
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
}

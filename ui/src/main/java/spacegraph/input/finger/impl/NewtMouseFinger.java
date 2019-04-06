package spacegraph.input.finger.impl;

import com.jogamp.newt.event.*;
import jcog.math.v2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Fingered;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.OrthoSurfaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.video.JoglDisplay;
import spacegraph.video.JoglWindow;

import java.util.function.Function;

/** ordinary desktop/laptop computer mouse, as perceived through jogamp NEWT's native interface */
public class NewtMouseFinger extends MouseFinger implements MouseListener, WindowListener {



    private final JoglDisplay space;


    public NewtMouseFinger(JoglDisplay s) {
        super(MAX_BUTTONS);
        this.space = s;

        s.later(()->{
            JoglWindow win = s.video;
            if (win.window.hasFocus())
                active.set(true);

            win.addMouseListenerPre(this);
            win.addWindowListener(this);
            win.onUpdate((Runnable) this::update);
        });
    }

    /** called for each layer. returns true if continues down to next layer */
    public void touch(Fingered s) {

        Fingering ff = this.fingering.get();

        if (ff == Fingering.Null || ff.escapes()) {
            Surface touchNext = s.finger(this);
            touching.accumulateAndGet(touchNext, ff::touchNext);
        }

    }

    public void setTransform(Function<v2, v2> transform) {
        pixelToGlobal = transform;
        updatePos();
    }

    private void updatePos() {
        _posGlobal.set(pixelToGlobal.apply(posPixel));
    }

    /** global position of the cursor center */
    @Override public v2 posGlobal() {
        //Function<v2,v2> z = this._screenToGlobal;
        //Function<v2,v2> z = _z !=null ? _z : (c instanceof Zoomed ? ((Zoomed)c) : c.parentOrSelf(Zoomed.class));
        return _posGlobal;
    }

    @Override protected void doUpdate() {

        touch(((OrthoSurfaceGraph) this.space).layers);

        Fingering ff = this.fingering.get();

        if (ff != Fingering.Null) {
            if (!ff.update(this)) {
                ff.stop(this);
                fingering.set(Fingering.Null);
            }
        }

        clearRotation();
    }

    private boolean update(boolean moved, MouseEvent e) {
        return update(moved, e, null);
    }

    private boolean update(boolean moved, MouseEvent e, short[] buttonsDown) {

        JoglWindow win = space.video;

        if (moved) {
            int pmx = e.getX(), pmy = win.window.getHeight() - e.getY();

            posPixel.set(pmx, pmy);

            posScreen.set(win.getX() + pmx, win.getScreenH() - (win.getY() + e.getY()));
        }

        if (buttonsDown != null) {
            update(buttonsDown);
        }

        return e != null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(@Nullable MouseEvent e) {
        if (active.compareAndSet(false, true)) {
            enter();
            if (e != null)
                update(false, e);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (active.compareAndSet(true, false)) {
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
        if (active.compareAndSet(false, true)) {
            enter();
            update(false, null);
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        if (active.compareAndSet(true, false)) {
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

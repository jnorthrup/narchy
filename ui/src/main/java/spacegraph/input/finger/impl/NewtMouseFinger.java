package spacegraph.input.finger.impl;

import com.jogamp.newt.event.*;
import jcog.math.v2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.SpaceGraphFlat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.hud.Ortho;
import spacegraph.video.JoglSpace;
import spacegraph.video.JoglWindow;

/** ordinary desktop/laptop computer mouse, as perceived through jogamp NEWT's native interface */
public class NewtMouseFinger extends MouseFinger implements MouseListener, WindowListener {



    private final JoglSpace space;


    public NewtMouseFinger(JoglSpace s) {
        super(MAX_BUTTONS);
        this.space = s;

        s.later(()->{
            JoglWindow win = s.display;
            if (win.window.hasFocus())
                focused.set(true);

            win.addMouseListenerPre(this);
            win.addWindowListener(this);
            win.onUpdate((Runnable) this::update);
        });
    }

    /** called for each layer. returns true if continues down to next layer */
    public boolean touch(Surface s) {

        Fingering ff = this.fingering.get();

        if (touchNext == null) {

            if (s instanceof Ortho) {
                _ortho = (Ortho)s; //HACK
                _posGlobal.setNaN(); //invalidate
            }

            Surface next = touchNext = (ff == Fingering.Null || ff.escapes()) ? s.finger(this) : touching.get();

            if (s instanceof Ortho) {
                _ortho = null; //HACK
            }

            return next == null;
        }

        return true;
    }

    @Override
    public v2 posGlobal(Surface c) {

        Ortho co = this._ortho;
        Ortho o = co !=null ? co : (c instanceof Ortho ? ((Ortho)c) : c.parentOrSelf(Ortho.class));
        if (o!=null) {
            if (_posGlobal.isNaN())
                _posGlobal.set(posGlobal(posPixel, o));
            return _posGlobal;
        } else
            return posPixel;//.clone(); //WTF?
    }

    private static v2 posGlobal(v2 x, Ortho o) {
        return o.cam.screenToWorld(x);
    }

    @Override protected void doUpdate() {
        touchNext = null;

        ((SpaceGraphFlat) this.space).layers.whileEachReverse(this::touch);

        Surface touchNext = this.touchNext;

        Fingering ff = this.fingering.get();

        @Nullable Surface touchPrev = touching(touchNext);
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

        JoglWindow win = space.display;

        if (moved) {
            int pmx = e.getX(), pmy = win.getHeight() - e.getY();

            posPixel.set(pmx, pmy);
            posScreen.set(win.getX() + pmx, win.getScreenY() - (e.getY() + win.getY()));
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

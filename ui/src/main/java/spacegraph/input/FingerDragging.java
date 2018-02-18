package spacegraph.input;

abstract public class FingerDragging extends Fingering {

    protected final int button;

    public FingerDragging(int button) {
        super();
        this.button = button;
    }

    @Override
    public boolean start(Finger f) {
        if (!f.prevButtonDown[button] && pressed(f)) {
            drag(f);
            return true;
        } else
            return false;
    }


    @Override
    public boolean update(Finger finger) {
        return pressed(finger) && drag(finger);
    }

    private boolean pressed(Finger finger) {
        return finger.buttonDown.length > 0 && finger.buttonDown[button];
    }

    /** return false to cancel the operation */
    abstract protected boolean drag(Finger f);
}

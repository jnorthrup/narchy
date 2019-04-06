package spacegraph.input.finger;

abstract public class Dragging extends Fingering {

    public final int button;
    private boolean stopped = false;

    public Dragging(int button) {
        super();
        this.button = button;
    }

    @Override
    final public boolean start(Finger f) {
        if (pressed(f) && startDrag(f)) {
            stopped = false;
            if (drag(f)) {
                return true;
            }
        }
        stopped = true;
        return false;
    }

    protected boolean startDrag(Finger f) {
        return true;
    }

    @Override
    public boolean escapes() {
        return true;
    }

    @Override
    public void stop(Finger finger) {
        super.stop(finger);
        stopped = true;
    }

    public final boolean isStopped() {
        return stopped;
    }

    @Override
    public final boolean update(Finger finger) {
        return pressed(finger) && drag(finger);
    }

    private boolean pressed(Finger f) {
        return f.pressed(button);
    }

    /** return false to cancel the operation */
    abstract protected boolean drag(Finger f);

}

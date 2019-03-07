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
        return pressing(f) && startDrag(f) && drag(f);
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

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public final boolean update(Finger finger) {
        return pressing(finger) && drag(finger);
    }

    private boolean pressing(Finger f) {
        return f.pressing(button);
    }

    /** return false to cancel the operation */
    abstract protected boolean drag(Finger f);
}

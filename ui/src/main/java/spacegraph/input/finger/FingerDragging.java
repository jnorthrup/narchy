package spacegraph.input.finger;

abstract public class FingerDragging extends Fingering {

    private final int button;
    private boolean stopped = false;

    public FingerDragging(int button) {
        super();
        this.button = button;
    }

    @Override
    final public boolean start(Finger f) {
        return f.pressing(button) && startDrag(f) && drag(f);
    }

    protected boolean startDrag(Finger f) {
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

    private boolean pressing(Finger finger) {
        return finger.pressing(button);
    }

    /** return false to cancel the operation */
    abstract protected boolean drag(Finger f);
}

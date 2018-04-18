package spacegraph.input.finger;

abstract public class FingerDragging extends Fingering {

    protected final int button;
    boolean stopped = false;

    public FingerDragging(int button) {
        super();
        this.button = button;
    }

    @Override
    public boolean start(Finger f) {
        return f.pressing(button) && drag(f);
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
    public boolean update(Finger finger) {
        return pressing(finger) && drag(finger);
    }

    public boolean pressing(Finger finger) {
        return finger.pressing(button);
    }

    /** return false to cancel the operation */
    abstract protected boolean drag(Finger f);
}

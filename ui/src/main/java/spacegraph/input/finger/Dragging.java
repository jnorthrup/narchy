package spacegraph.input.finger;

abstract public class Dragging extends Fingering {

    public final int button;
    private boolean active = false;

    public Dragging(int button) {
        super();
        this.button = button;
    }

    @Override
    final public boolean start(Finger f) {
        if (pressed(f) && ready(f)) {
            if (drag(f)) {
                active = true;
                return true;
            }
        }
        active = false;
        return false;
    }

    protected boolean ready(Finger f) {
        return true;
    }

    @Override
    public boolean escapes() {
        return true;
    }

    @Override
    public void stop(Finger finger) {
        active = false;
    }

    public final boolean active() {
        return active;
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

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

    private boolean pressed(Finger f) {
        return f.pressed(button);
    }

    @Override
    public final boolean updateGlobal(Finger finger) {
        return true;
    }

    @Override
    public boolean updateLocal(Finger finger) {
        return pressed(finger) && drag(finger);
    }

    abstract protected boolean drag(Finger f);

}

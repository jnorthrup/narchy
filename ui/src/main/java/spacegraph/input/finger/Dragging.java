package spacegraph.input.finger;

abstract public class Dragging extends Fingering {

    public final int button;
    private boolean active = false;

    public Dragging(int button) {
        super();
        this.button = button;
    }

    @Override
    public boolean start(Finger f) {
        if (pressedNow(f) && ready(f)) {
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
    public boolean defer(Finger finger) {
        return !finger.pressed(button);
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
    private boolean pressedNow(Finger f) {
        return f.pressedNow(button);
    }

    @Override
    public boolean update(Finger finger) {
        return pressed(finger) && drag(finger);
    }

    abstract protected boolean drag(Finger f);

}

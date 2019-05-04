package spacegraph.input.finger.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.Surface;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

abstract public class MouseFinger extends Finger {

    final AtomicBoolean updating = new AtomicBoolean(false);

    private final Function<Finger,Surface> root;


    protected MouseFinger(int buttons, Function<Finger, Surface> root) {
        super(buttons);
        this.root = root;
    }



    /** called for each layer. returns true if continues down to next layer */
    public void finger(Function<Finger, Surface> s) {

        posGlobal.set(posPixel); //HACK

        Fingering ff = this.fingering.get();

        Surface touchNext = s.apply(this);

        touching.accumulateAndGet(touchNext, ff::touchNext);

        commitButtons();
    }

    private void _update() {

        if (!focused.get())
            return;

        if (updating.compareAndSet(false, true)) {
            try {
                update();
            } finally {
                updating.set(false);
            }
        } else {
            /* busy */
            logger.warn("{} event update already in-progress", this);
        }
    }

    @Override
    public Surface cursorSurface() {
        return new CursorSurface(this);
    }

    protected void update() {
        finger(root);

        clearRotation();
    }

    final static Logger logger = LoggerFactory.getLogger(MouseFinger.class);
    final static int MAX_BUTTONS = 5;

}

package spacegraph.input.finger.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;

import java.util.concurrent.atomic.AtomicBoolean;

abstract public class MouseFinger extends Finger {

    final AtomicBoolean updating = new AtomicBoolean(false);


    protected Surface touchNext;

    protected MouseFinger(int buttons) {
        super(buttons);
    }

    abstract protected void doUpdate();

    protected void update() {

        if (!updating.compareAndSet(false, true)) {
            /* busy */
            logger.warn("{} event update already in-progress", this);
            return;
        }

        try {
            doUpdate();
        } finally {
            updating.set(false);
        }
    }

    final static Logger logger = LoggerFactory.getLogger(MouseFinger.class);
    final static int MAX_BUTTONS = 5;

}

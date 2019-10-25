package jcog;

import java.util.concurrent.TimeUnit;

/** when something takes too much time */
public class PatienceUnderflowException extends RuntimeException {
    public PatienceUnderflowException(String attemptedProcedure, long timeAllowed, TimeUnit timeUnit) {
        super(attemptedProcedure + " exceeded " + Texts.INSTANCE.timeStr((double) timeUnit.toNanos(timeAllowed)));
    }
}

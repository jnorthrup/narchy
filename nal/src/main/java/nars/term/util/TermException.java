package nars.term.util;

import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.util.SoftException;

import java.util.Arrays;

import static nars.time.Tense.DTERNAL;

/**
 * contains fields for storing target construction parameters
 */
public final class TermException extends SoftException {


    private final Op op;
    private final int dt;

    private final Term[] args;

    private final String reason;


    public TermException(String reason) {
        this(reason, null, Op.EmptyTermArray);
    }

    public TermException(String reason, Termlike t) {
        this(reason, null, DTERNAL, t);
    }

    public TermException(String reason, Op op, Term[] args) {
        this(reason, op, DTERNAL, args);
    }

    public TermException(String reason, Op op, int dt, Termlike args) {
        this(reason, op, dt, args.arrayShared());
    }

    public TermException(String reason, Op op, int dt, Term... args) {
        this.op = op;
        this.dt = dt;
        this.args = args;
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        return getClass().getSimpleName() + ": " + reason + " {" +
                op +
                ", dt=" + dt +
                ", args=" + Arrays.toString(args) +
                '}';
    }

}

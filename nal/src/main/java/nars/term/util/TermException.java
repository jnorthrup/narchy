package nars.term.util;

import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

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
        this(null, Op.EmptyTermArray, reason);
    }

    public TermException(Op op, Term[] args, String reason) {
        this(op, DTERNAL, reason, args);
    }

    public TermException(Op op, int dt, Term[] args, String reason) {
        this(op, dt, reason, args);
    }

    public TermException(Op op, int dt, Termlike args, String reason) {
        this(op, dt, reason, args.arrayShared());
    }

    public TermException(Op op, int dt, String reason, Term... args) {
        this.op = op;
        this.dt = dt;
        this.args = args;
        this.reason = reason;
    }

//        public InvalidTermException(String s, Compound c) {
//            this(c.op(), c.dt(), c.subterms(), s);
//        }

    @NotNull
    @Override
    public String getMessage() {
        return getClass().getSimpleName() + ": " + reason + " {" +
                op +
                ", dt=" + dt +
                ", args=" + Arrays.toString(args) +
                '}';
    }

}

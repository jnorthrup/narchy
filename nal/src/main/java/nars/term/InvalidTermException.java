package nars.term;

import nars.Op;
import nars.term.container.Subterms;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static nars.time.Tense.DTERNAL;

/**
 * Created by me on 2/26/16.
 */
public final class InvalidTermException extends SoftException {

    @NotNull private final Op op;
    private final int dt;
    @NotNull private final Term[] args;
    @NotNull private final String reason;


    public InvalidTermException(/*@NotNull*/ Op op, @NotNull Term[] args, @NotNull String reason) {
        this(op, DTERNAL, reason, args);
    }

    public InvalidTermException(/*@NotNull*/ Op op, int dt, @NotNull Term[] args, @NotNull String reason) {
        this(op, dt, reason, args);
    }

    public InvalidTermException(/*@NotNull*/ Op op, int dt, @NotNull Subterms args, @NotNull String reason) {
        this(op, dt, reason, args.arrayClone());
    }

    public InvalidTermException(/*@NotNull*/ Op op, int dt, @NotNull String reason, @NotNull Term... args) {
        this.op = op;
        this.dt = dt;
        this.args = args;
        this.reason = reason;
    }

    public InvalidTermException(String s, @NotNull Compound c) {
        this(c.op(), c.dt(), c.subterms(), s);
    }

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

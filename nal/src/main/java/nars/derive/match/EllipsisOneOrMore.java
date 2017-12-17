package nars.derive.match;

import nars.$;
import nars.term.Term;
import nars.term.var.NormalizedVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_PATTERN;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisOneOrMore extends Ellipsis {

    public EllipsisOneOrMore(@NotNull NormalizedVariable /*Variable*/ name) {
        super(name, 1); //TODO decide if EllipsisTransform, a subclass, needs its own uniqueness hashmask
    }

    @Override
    public @Nullable Variable normalize(byte vid) {
        if (vid == num) return this;
        return new EllipsisOneOrMore($.v(op(), vid));
    }

    final static int RANK = Term.opX(VAR_PATTERN, 2 /* different from normalized variables with a subOp of 0 */);
    @Override public int opX() { return RANK;    }


    @NotNull
    @Override
    public String toString() {
        return super.toString() + "..+";
    }


}

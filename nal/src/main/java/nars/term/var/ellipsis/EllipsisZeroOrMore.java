package nars.term.var.ellipsis;

import nars.$;
import nars.term.Term;
import nars.term.Variable;
import nars.term.var.NormalizedVariable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_PATTERN;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisZeroOrMore extends Ellipsis {

    public EllipsisZeroOrMore(NormalizedVariable /*Variable*/ name) {
        super(name, 0);
    }

    @Override
    public @Nullable Variable normalizedVariable(byte vid) {
        if (vid == num) return this;
        return new EllipsisZeroOrMore($.v(op(), vid));
    }

    private final static int RANK = Term.opX(VAR_PATTERN, 4 /* different from normalized variables with a subOp of 0 */);
    @Override public int opX() { return RANK;    }

}

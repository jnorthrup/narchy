package nars.term.var.ellipsis;

import nars.$;
import nars.term.Variable;
import nars.term.var.NormalizedVariable;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/5/15.
 */
public class EllipsisZeroOrMore extends Ellipsis {

    EllipsisZeroOrMore(NormalizedVariable /*Variable*/ name) {
        super(name, 0);
    }

    @Override
    public @Nullable Variable normalizedVariable(byte vid) {
        if (vid == num) return this;
        return new EllipsisZeroOrMore($.v(op(), vid));
    }

}

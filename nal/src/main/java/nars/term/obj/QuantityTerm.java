package nars.term.obj;

import nars.$;
import nars.term.ProxyTerm;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;
import tec.uom.se.AbstractQuantity;

import javax.measure.Quantity;

import static nars.Op.PROD;

public class QuantityTerm extends ProxyTerm {

    public final Quantity<?> quant;

    public QuantityTerm(Quantity<?> q) {
        super( $.p( $.the(q.getUnit().toString()), $.the( q.getValue() ) ) );
        this.quant = q;
    }

    public static @Nullable QuantityTerm the(String toParse) throws IllegalArgumentException {
        var q = AbstractQuantity.parse(toParse);
        if (q == null)
            return null;
        return new QuantityTerm(q);
    }

    /** interpret a product of the form (unit, time) */
    public static @Nullable QuantityTerm the(Term qTerm) throws IllegalArgumentException {
        if (qTerm.op()==PROD && qTerm.subs()==2) {
            var unit = qTerm.sub(0);
            var value = $.doubleValue(qTerm.sub(1));
            return QuantityTerm.the(value + " " + unit);
        }
        return null;
    }

}

package nars.term.obj;

import nars.$;
import nars.term.ProxyTerm;
import tec.uom.se.AbstractQuantity;

import javax.measure.Quantity;

public class QuantityTerm extends ProxyTerm {

    public final Quantity<?> quant;

    public QuantityTerm(Quantity<?> q) {
        super( $.p( $.the(q.getUnit()), $.the( q.getValue() ) ) );
        this.quant = q;
    }

    public static QuantityTerm the(String toParse) throws IllegalArgumentException {
        Quantity<?> q = AbstractQuantity.parse(toParse);
        return new QuantityTerm(q);
    }


}

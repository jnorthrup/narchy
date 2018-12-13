package nars.attention;

import jcog.data.atomic.AtomicFloat;
import jcog.pri.Pri;
import jcog.tree.atomic.AtomicTreeNode;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;

import java.util.Objects;
import java.util.stream.Stream;

public class AttNode extends AtomicTreeNode<AttNode> {

    private final Term id;

    /** demand, ideal amount to be in supply at next distribution. */
    public final AtomicFloat demand = new AtomicFloat();

    /** supply (provided by parent) */
    public final Pri supply = new Pri(0);

    public AttNode(Object id) {
        super();
        this.id = $.identity(id);
    }



    public void update(NAR nar) {

        float dAll = childDemand(nar);
        demand.set(dAll);

        float rate = Math.min(1, supply.priElseZero()/dAll);

        childrenStream().forEach(c -> {
            float cd = c.demand.floatValue() - c.supply.priElseZero();
            if (cd > 0) {
                float dSub = cd * rate;
                float taken = c.supply.take(supply, dSub, true, false);
            }
        });
    }

    protected float childDemand(NAR nar) {
        return Math.max(0, Math.max(0, (float)childrenStream().mapToDouble(c->{
            c.update( nar);
            return Math.max(c.demand.floatValue() - c.supply.priElseZero(), 0);
        }).sum()));
    }


//    /** take pri from parent supply */
//    public void get(float request, Pri target) {
//        target.take(parent().supply, request, true, false);
//    }


    @Override
    public String toString() {
        return id.toString() + " supply=" + supply + " demand=" + demand;
    }

    public Stream<Concept> concepts(NAR nar) {
        return childrenStream().map(x -> nar.concept(x.id)).filter(Objects::nonNull);
    }

}

package nars.attention;

import jcog.data.atomic.AtomicFloat;
import jcog.pri.Pri;
import jcog.pri.Prioritizable;
import jcog.pri.ScalarValue;
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



    protected float myDemand(NAR nar) {
        return 0;
    }

    public void update(NAR nar) {

        float myDemand = myDemand(nar);
        float demand = myDemand + childDemand(nar);
        float demandNet = Math.max(0, demand - supply.pri());
        this.demand.set(demandNet);


        float totalSupply = supply.pri();
        childrenStream().forEach(c -> {
            float cd = c.demand.floatValue();
            if (cd > 0) {
                float demandFraction = cd / demand;
                float dSub = Math.min(cd, demandFraction * totalSupply);
                if (dSub > ScalarValue.EPSILON) {
                    float givenToChild = c.supply.take(this.supply, dSub, true, false);
                }
            }
        });
    }

    protected float childDemand(NAR nar) {
        return Math.max(0, (float)childrenStream().mapToDouble(c->{
            c.update( nar );
            return Math.max(c.demand.floatValue(), 0);
        }).sum());
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

    public void take(Prioritizable c, float pri) {
        c.take(supply, pri, true, false);
    }

    public void taken(float p) {
        supply.priSub(p);
    }
}

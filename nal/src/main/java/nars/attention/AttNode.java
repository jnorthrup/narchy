package nars.attention;

import jcog.math.FloatRange;
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

    /** boost, relative among peers */
    public final FloatRange boost = new FloatRange(1f, 0.01f, 2f);

    /** advised priority value  */
    public final Pri pri = new Pri(0);

    public AttNode(Object id) {
        super();
        this.id = $.identity(id);
    }




//    public void update(NAR nar) {
//
//        float myDemand = myDemand(nar);
//        float childrenDemand = childDemand(nar);
//        float totalDemand = myDemand + childrenDemand;
//        //float demandNet = Math.max(0, totalDemand - supply.pri());
//        this.demand.setAt(totalDemand);
//
//    // AUTO SUPPLY
//        childrenStream().forEach(c -> c.pri.pri(c.demand.floatValue()));
//
//    // RESTRICT
////        if (childrenDemand > ScalarValue.EPSILON) {
////            float totalSupply = supply.pri();
////            float childrenSupply = totalSupply * (totalDemand > ScalarValue.EPSILON ? childrenDemand / totalDemand : 0);
////            childrenStream().forEach(c -> {
////                float cd = c.demand.floatValue() - c.supply.pri();
////                if (cd > ScalarValue.EPSILON) {
////                    float dFrac = cd / (childrenDemand);
////                    float dSub = Math.min(cd, dFrac * childrenSupply);
////                    if (dSub > ScalarValue.EPSILON) {
////                        float givenToChild = c.supply.take(this.supply, dSub, true, false);
////                    }
////                }
////            });
////        }
//    }
//
//    protected float childDemand(NAR nar) {
//        return Math.max(0, (float)childrenStream().mapToDouble(c->{
//            c.update( nar );
//            return Math.max(c.demand.floatValue() - c.pri.pri(), 0);
//        }).sum());
//    }

    @Override
    public String toString() {
        return id + " pri=" + pri + " boost=" + boost;
    }

    public Stream<Concept> concepts(NAR nar) {
        return childrenStream().map(x -> nar.concept(x.id)).filter(Objects::nonNull);
    }

//    static public void ensure(Prioritizable c, float pri) {
//        pri -= c.priElseZero();
//        if (pri > 0) {
//            //c.take(supply, pri, true, false); //RESTRICTED
//            c.priAdd(pri);
//        }
//    }

    public final float elementPri() {
        return pri.priElseZero();
        //return nar.priDefault(BELIEF);
    }
    protected float elementFraction(int n) {
        float i;
        if (n == 0)
            return 0;
        i = 1; //each component important as a top level concept
        //i = 1f / Util.sqrt(n); //shared by sqrt of components
        //i = 1f / n; //shared by all components
        return i;
    }

    public void update(float pri) {
        this.pri.pri(pri);
        float priEach = pri * elementFraction(size());
        //TODO local boost's
        childrenStream().forEach(c -> c.update(c.boost.floatValue() * priEach));
    }
}

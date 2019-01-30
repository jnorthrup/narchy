//package nars.op;
//
//import jcog.data.graph.AdjGraph;
//import jcog.pri.PriReference;
//import jcog.pri.bag.Bag;
//import nars.NAR;
//import nars.Param;
//import nars.concept.Concept;
//import nars.control.NARService;
//import nars.target.Term;
//import nars.target.Termed;
//import nars.unify.UnifySubst;
//import nars.util.graph.TermGraph;
//
///** TODO not finished */
//public class QuerySpider extends NARService {
//
//    public QuerySpider(NAR nar) {
//        super(nar);
//    }
//
//    @Override
//    protected void starting(NAR nar) {
//        nar.onTask(t -> {
//            if (t.isQuestionOrQuest() && t.target().hasVarQuery()) {
//                Term tt = t.target();
//                AdjGraph<Term, Float> g = spider(nar, t, 3);
//                g.nodes.keysView().takeWhile(r -> {
//                    new UnifySubst(null, nar, (z) -> true, Param.TTL_MIN).unify(tt, r.v, true);
//                    return true;
//                });
//            }
//        });
//    }
//
//    private AdjGraph<Term, Float> spider(NAR nar, Termed t, int recurse) {
//        return spider(nar, t, new AdjGraph(true), recurse);
//    }
//
//    /**
//     * resource-constrained breadth first search
//     */
//    private AdjGraph<Term, Float> spider(NAR nar, Termed t, AdjGraph<Term, Float> g, int recurse) {
//
//        Term tt = t.target();
//        if (tt.op().conceptualizable && g.addIfNew(tt) && recurse > 0) {
//
//
//            Concept c = nar.conceptualize(t);
//            if (c == null)
//                return g;
//
//            Bag<Term, PriReference<Term>> tl = c.termlinks();
//            if (!tl.isEmpty()) {
//                TermGraph.termlink(nar, tl.stream().map(PriReference::get), g);
//            } else {
//                TermGraph.termlink(nar, c.linker().targets(), g);
//            }
//
//
//            g.nodes.forEachKey(k -> spider(nar, k.v, g, recurse-1));
//        } else {
//            g.addNode(tt);
//        }
//
//        return g;
//    }
//}

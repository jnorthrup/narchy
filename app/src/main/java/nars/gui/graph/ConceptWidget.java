package nars.gui.graph;

import jcog.pri.PLink;
import jcog.pri.Prioritized;
import jcog.util.Flip;
import nars.concept.Concept;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.SphereShape;
import spacegraph.space3d.widget.EDraw;
import spacegraph.space3d.widget.SpaceWidget;
import spacegraph.video.Draw;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static nars.gui.graph.DynamicConceptSpace.ConceptVis2.TASKLINK;
import static nars.gui.graph.DynamicConceptSpace.ConceptVis2.TERMLINK;
import static org.eclipse.collections.impl.tuple.Tuples.twin;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static spacegraph.util.math.v3.v;


public class ConceptWidget extends SpaceWidget<Concept> {

    public float pri;

    public final Flip<Map<Concept, ConceptEdge>> edges = new Flip(LinkedHashMap::new);

    public ConceptWidget(Concept x) {
        super(x);


//        final PushButton icon = new PushButton(x.toString(), (z) -> {
//            setFront(new BeliefTableChart(nar, x).scale(4,4));
//        });
//        setFront(icon);

        //float edgeActivationRate = 1f;

//        edges = //new HijackBag<>(maxEdges * maxNodes, 4, BudgetMerge.plusBlend, nar.random);


//        for (int i = 0; i < edges; i++)
//            this.edges.add(new EDraw());

    }
    public static float r(float range) {
        return (-0.5f + (float) Math.random()) * 2f * range;
    }

    @Override
    protected CollisionShape newShape() {
        return id.op().atomic ? new SphereShape() : super.newShape() /* cube */;
    }

    @Override
    public Iterable<ConceptEdge> edges() {
        return edges.read().values();
    }

    @Override
    public Body3D newBody(boolean collidesWithOthersLikeThis) {
        Body3D x = super.newBody(collidesWithOthersLikeThis);

        final float initDistanceEpsilon = 50f;

        //place in a random direction
        x.transform.set(
                r(initDistanceEpsilon),
                r(initDistanceEpsilon),
                r(initDistanceEpsilon));

        //impulse in a random direction
        final float initImpulseEpsilon = 0.25f;
        x.impulse(v(
                r(initImpulseEpsilon),
                r(initImpulseEpsilon),
                r(initImpulseEpsilon)));

        return x;
    }

    //final RecycledSummaryStatistics edgeStats = new RecycledSummaryStatistics();



    //    @Override
//    protected void renderRelativeAspect(GL2 gl) {
//        renderLabel(gl, 0.05f);
//    }


//    @Nullable
//    public EDraw addEdge(@NotNull Budget l, @NotNull ConceptWidget target) {
//
//        EDraw z = new TermEdge(target, l);
//        edges.add(z);
//        return z;
//    }


//    public ConceptWidget setConcept(Concept concept, long when) {
//        this.concept = concept;
//        //icon.update(concept, when);
//        return this;
//    }

    public static class ConceptVis1 implements TermVis<ConceptWidget> {

        final float minSize = 0.1f;
        final float maxSize = 6f;


        @Override
        public void accept(List<ConceptWidget> pending) {
            pending.forEach(this::each);
        }

        public void each(ConceptWidget cw) {
            float p = cw.pri;
            p = (p == p) ? p : 0;// = 1; //pri = key.priIfFiniteElseZero();

            //sqrt because the area will be the sqr of this dimension
            float nodeScale = (float) (minSize + Math.sqrt(p) * maxSize);//1f + 2f * p;
            cw.scale(nodeScale, nodeScale, nodeScale);


            Draw.hsb(cw.shapeColor, (cw.id.op().ordinal() / 16f), 0.75f + 0.25f * p, 0.75f, 0.9f);
        }


    }

    static class EdgeComponent extends PLink<ObjectIntPair<Twin<ConceptWidget>>> {

        final int type;

        EdgeComponent(ConceptWidget src, ConceptWidget tgt, int type, float pri) {
            super(pair(twin(src, tgt), type), pri);
            this.type = type;
            this.pri = pri;
        }

        public ConceptWidget src() { return get().getOne().getOne(); }
        public ConceptWidget tgt() { return get().getOne().getTwo(); }

        @Override
        public boolean isDeleted() {
            if (!src().active() || !tgt().active()) {
                delete();
                return true;
            }
            return super.isDeleted();
        }

    }


    public static class ConceptEdge extends EDraw<ConceptWidget> {

        float termlinkPri, tasklinkPri;
        boolean inactive;
        final static float priTHRESH = Prioritized.EPSILON;

        public ConceptEdge(ConceptWidget src, ConceptWidget target, float pri) {
            super(src, target, pri);
            inactive = false;
        }

        protected void decay(float rate) {
            //termlinkPri = tasklinkPri = 0;

            //decay
            termlinkPri *= rate;
            tasklinkPri *= rate;
        }


        public void add(float p, boolean termOrTask) {
            if (termOrTask) {
                termlinkPri += p;
            } else {
                tasklinkPri += p;
            }
        }


        @Override
        public boolean isDeleted() {
            boolean inactive = !id.getOne().active() || !id.getTwo().active();
            if (inactive) {
                delete();
                return true;
            }
            return super.isDeleted();
        }

        public void merge(EdgeComponent e) {
            float p = e.priElseZero();
            if (p <= priTHRESH)
                return;

            switch (e.type) {
                case TERMLINK:
                    this.termlinkPri += p;
                    break;
                case TASKLINK:
                    this.tasklinkPri += p;
                    break;
            }
            priMax(p);
            inactive = false;
        }

    }

//        private class ConceptFilter implements Predicate<BLink<Concept>> {
//
//            int count;
//
//            public void reset() {
//                count = 0;
//            }
//
//            @Override
//            public boolean test(BLink<Concept> cc) {
//
//
//                float p = cc.pri();
//                if ((p < _minPri) || (p > _maxPri)) {
//                    return true;
//                }
//
//                Concept c = cc.get();
//
//                String keywordFilter1 = keywordFilter;
//                if (keywordFilter1 != null) {
//                    if (!c.toString().contains(keywordFilter1)) {
//                        return true;
//                    }
//                }
//
//                concepts.add(c);
//                return count++ <= maxNodes;
//
//            }
//        }


//    public static void updateConceptEdges(SpaceGrapher g, TermNode s, TLink link, DoubleSummaryReusableStatistics accumulator) {
//
//
//        Term t = link.getTerm();
//        TermNode target = g.getTermNode(t);
//        if ((target == null) || (s.equals(target))) return;
//
//        TermEdge ee = getConceptEdge(g, s, target);
//        if (ee != null) {
//            ee.linkFrom(s, link);
//            accumulator.accept(link.getPriority());
//        }
//    }


//    public final void updateNodeOLD(SpaceGrapher sg, BagBudget<Concept> cc, TermNode sn) {
//
//        sn.c = cc.get();
//        sn.priNorm = cc.getPriority();
//
//
//
//        //final Term t = tn.term;
//        //final DoubleSummaryReusableStatistics ta = tn.taskLinkStat;
//        //final DoubleSummaryReusableStatistics te = tn.termLinkStat;
//
//
////        System.out.println("refresh " + Thread.currentThread() + " " + termLinkMean.getResult() + " #" + termLinkMean.getN() );
//
//
////        Consumer<TLink> tLinkConsumer = t -> {
////            Term target = t.getTerm();
////            if (!source.equals(target.getTerm())) {
////                TermNode tn = getTermNode(graph, target);
////                //TermEdge edge = getConceptEdge(graph, sn, tn);
////
////            }
////        };
////
////        c.getTaskLinks().forEach(tLinkConsumer);
////        c.getTermLinks().forEach(tLinkConsumer);
//
//
//    }


}

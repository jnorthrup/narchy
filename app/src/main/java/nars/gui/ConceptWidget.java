package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.Deletes;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import nars.$;
import nars.Task;
import nars.budget.PLinkUntilDeleted;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.space.Cuboid;
import spacegraph.space.EDraw;

import java.util.HashMap;
import java.util.function.Consumer;

import static jcog.Util.or;
import static spacegraph.math.v3.v;


public class ConceptWidget extends Cuboid<Term> implements Consumer<PLink<? extends Termed>> {

    public final Bag<TermEdge, PLink<TermEdge>> edges;


    //caches a reference to the current concept
    public Concept concept;
    private final ConceptVis conceptVis = new ConceptVis2();
    private transient ConceptsSpace space;
    public float pri = 0;


    public ConceptWidget(Termed x) {
        super(x.term(), 1, 1);

        setFront(
//            /*col(
                //new Label(x.toString())
//                row(new FloatSlider( 0, 0, 4 ), new BeliefTableChart(nar, x))
//                    //new CheckBox("?")
//            )*/
                new ConceptIcon(x)
        );

//        final PushButton icon = new PushButton(x.toString(), (z) -> {
//            setFront(new BeliefTableChart(nar, x).scale(4,4));
//        });
//        setFront(icon);

        //float edgeActivationRate = 1f;

//        edges = //new HijackBag<>(maxEdges * maxNodes, 4, BudgetMerge.plusBlend, nar.random);
        this.edges =
                new PLinkHijackBag(0, 2);
                //new ArrayBag<>(0, PriMerge.avg, new HashMap());


//        for (int i = 0; i < edges; i++)
//            this.edges.add(new EDraw());

    }


    @Override
    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic x = super.newBody(collidesWithOthersLikeThis);


        final float initDistanceEpsilon = 50f;
        final float initImpulseEpsilon = 0.25f;

        //place in a random direction
        x.transform().set(SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon));

        //impulse in a random direction
        x.impulse(v(SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon)));

        return x;
    }

    @Override
    public void delete(Dynamics dyn) {
        concept = null;
        super.delete(dyn);
        //edges.setCapacity(0);
    }


    @Override
    public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        Surface s = super.onTouch(body, hitPoint, buttons, space);
        if (s != null) {

        }
        return s;
    }

    final static Truth zero = $.t(0.5f, 0.01f);

    public void commit(ConceptsSpace space) {

        this.space = space;


        Concept c = concept;

        if (c != null /* && !c.isDeleted() */) {


            int newCap = Util.lerp(pri, space.maxEdgesPerNodeMax, space.maxEdgesPerNodeMin);
            edges.setCapacity(newCap);
            if (newCap > 0) {

                edges.forEach(x -> x.get().clear());
                edges.commit(); //forget

                //phase 1: collect
                c.tasklinks().forEach(this);
                c.termlinks().forEach(this);

                float priSum = edges.priSum();

                conceptVis.apply(this, key);


                edges.commit(x -> {
                    x.get().update( ConceptWidget.this, priSum);
                });

            }


        }

//            float lastConceptForget = instance.getLastForgetTime();
//            if (lastConceptForget != lastConceptForget)
//                lastConceptForget = now;

//        @NotNull Bag<Term> termlinks = cc.termlinks();
//        @NotNull Bag<Task> tasklinks = cc.tasklinks();

//        if (!termlinks.isEmpty()) {
//            float lastTermlinkForget = ((BLink) (((ArrayBag) termlinks).get(0))).getLastForgetTime();
//            if (lastTermlinkForget != lastTermlinkForget)
//                lastTermlinkForget = lastConceptForget;
//        }

        //lag = now - Math.max(lastConceptForget, lastTermlinkForget);
        //lag = now - lastConceptForget;
        //float act = 1f / (1f + (timeSinceLastUpdate/3f));

//        Concept cc = nar.concept(tt);
//        if (cc != null) {
//            //remove? hide?
//    //        clearEdges();
//            int maxEdges = edges.length;
//    //
//            Consumer<BLink<? extends Termed>> linkAdder = l -> {
//                if (numEdges < maxEdges)
//                    addLink(s, l);
//            };
//            @NotNull Bag<Task> tasklinks = cc.tasklinks();
//            tasklinks.forEach(linkAdder);
//        }
        //tasklinks.topWhile(linkAdder, maxEdges - edgeCount()); //fill remaining edges
//        termlinks.topWhile(linkAdder, maxEdges - edgeCount()); //fill remaining edges


    }


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

    @Override
    public void renderAbsolute(GL2 gl) {
        renderEdges(gl);
    }


    void renderEdges(GL2 gl) {
        edges.forEachKey(f -> {
            if (f.a > 0)
                render(gl, f);
        });
    }

    public void render(@NotNull GL2 gl, @NotNull EDraw e) {

        gl.glColor4f(e.r, e.g, e.b, e.a);

        float width = e.width;// * radius();
        if (width <= 0.1f) {
            Draw.renderLineEdge(gl, this, e, width);
        } else {
            Draw.renderHalfTriEdge(gl, this, e, width / 9f, e.r * 2f /* hack */);
        }
    }

    @Override
    public void accept(PLink<? extends Termed> tgt) {
        float pri = tgt.priSafe(-1);
        if (pri < 0)
            return;

        Termed ttt = tgt.get();
        Term tt = ttt.term();
        if (!tt.equals(key)) {
            Concept c = space.nar.concept(tt);
            if (c != null) {
                ConceptWidget to = space.widgets.getIfPresent(c);
                if (to != null && to.active()) {
                    TermEdge ate = space.edges.apply(Tuples.pair(c, to));
                    ate.add(tgt, !(ttt instanceof Task));
                    edges.put(new PLinkUntilDeleted(ate, pri));
                }
            }
        }
    }


//    public ConceptWidget setConcept(Concept concept, long when) {
//        this.concept = concept;
//        //icon.update(concept, when);
//        return this;
//    }

    public static class TermEdge extends EDraw<Term, ConceptWidget> implements Termed, Deletes {

        float termlinkPri, tasklinkPri;

        private final int hash;

        public TermEdge(@NotNull ConceptWidget target) {
            super(target);
            this.hash = target.key.hashCode();
        }

        protected void clear() {
            termlinkPri = tasklinkPri = 0;
        }

        public void set(PLink b, boolean termOrTask) {
            clear();
            add(b, termOrTask);
        }

        public void add(PLink b, boolean termOrTask) {
            float p = b.priSafe(0);
            if (termOrTask) {
                termlinkPri += p;
            } else {
                tasklinkPri += p;
            }
        }

        @NotNull
        @Override
        public Term term() {
            return target.key;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || target.key.equals(o);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        public void update(ConceptWidget src, float conceptEdgePriSum) {

            float edgeSum = (termlinkPri + tasklinkPri);

            if (edgeSum >= 0) {

                //float priAvg = priSum/2f;

                float minLineWidth = 5f;
                float priToWidth = 5f;

                this.width = minLineWidth + priToWidth * Math.min(edgeSum, 5);

                //z.r = 0.25f + 0.7f * (pri * 1f / ((Term)target.key).volume());
//                float qEst = ff.qua();
//                if (qEst!=qEst)
//                    qEst = 0f;



                if (edgeSum > 0) {
                    this.b = 0.1f;
                    this.r = 0.1f + 0.7f * (tasklinkPri / edgeSum);
                    this.g = 0.1f + 0.7f * (termlinkPri / edgeSum);
                } else {
                    this.r = this.g = this.b = 0.1f;
                }

                //this.a = 0.1f + 0.5f * pri;
                float edgeProp = conceptEdgePriSum > 0  ? edgeSum / conceptEdgePriSum : 0;

                this.a = //0.1f + 0.5f * Math.max(tasklinkPri, termlinkPri);
                        //0.1f + 0.9f * ff.pri(); //0.9f;
                        0.1f + 0.8f * edgeProp;

                this.attraction = 0f + 2f * edgeProp;// + priSum * 0.75f;// * 0.5f + 0.5f;
                this.attractionDist = src.radius() + target.radius(); //target.radius() * 2f;// 0.25f; //1f + 2 * ( (1f - (qEst)));
            } else {
                this.a = -1;
                this.attraction = 0;
            }


        }

        @Override
        public boolean isDeleted() {
            return !target.active();
        }
    }

    public interface ConceptVis {
        void apply(ConceptWidget w, Term tt);
    }

    public static class ConceptVis1 implements ConceptVis {

        final float minSize = 0.1f;
        final float maxSize = 6f;


        @Override
        public void apply(ConceptWidget cw, Term tt) {
            float p = cw.pri;
            p = (p == p) ? p : 0;// = 1; //pri = key.priIfFiniteElseZero();

            //sqrt because the area will be the sqr of this dimension
            float nodeScale = (float) (minSize + Math.sqrt(p) * maxSize);//1f + 2f * p;
            cw.scale(nodeScale, nodeScale, nodeScale);


            Draw.hsb((tt.op().ordinal() / 16f), 0.75f + 0.25f * p, 0.75f, 0.9f, cw.shapeColor);
        }
    }

    public static class ConceptVis2 implements ConceptVis {

        final float minSize = 2f;
        final float maxSize = 10f;

        @Override
        public void apply(ConceptWidget cw, Term tt) {
            ConceptsSpace space = cw.space;
            float p = cw.pri;

            //long now = space.now();
//            float b = conceptWidget.concept.beliefs().eviSum(now);
//            float g = conceptWidget.concept.goals().eviSum(now);
//            float q = conceptWidget.concept.questions().priSum();
            float ec = p;// + w2c((b + g + q)/2f);

            //sqrt because the area will be the sqr of this dimension
            float nodeScale = (float) (minSize + Math.sqrt(ec) * maxSize);//1f + 2f * p;
            float l = nodeScale * 1.618f;
            float w = nodeScale;
            float h = nodeScale / (1.618f * 2);
            cw.scale(l, w, h);



            float density = 6f;
            if (cw.body != null) {
                cw.body.setMass(l * w * h * density);
                cw.body.setDamping(0.9f, 0.9f);
            }

//            Draw.hsb(
//                    (tt.op().ordinal() / 16f),
//                    0.5f + 0.5f / tt.volume(),
//                    0.3f + 0.2f * p,
//                    0.9f, conceptWidget.shapeColor);

            Concept c = cw.concept;
            if (c != null) {
                Truth belief = c.belief(space.now, space.dur);
                if (belief == null) belief = zero;
                Truth goal = c.goal(space.now, space.dur);
                if (goal == null) goal = zero;

                float angle = 45 + belief.freq() * 180f + (goal.freq() - 0.5f) * 90f;
                Draw.hsb(angle / 360f, 0.5f, 0.25f + 0.5f * or(belief.conf(), goal.conf()), 0.9f, cw.shapeColor);
            }

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

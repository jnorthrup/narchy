package nars.gui.graph;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentArrayBag;
import jcog.bag.util.Bagregate;
import jcog.list.FasterList;
import jcog.math.FloatParam;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import jcog.util.Flip;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.DurService;
import nars.gui.DynamicListSpace;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.phys.shape.SphereShape;
import spacegraph.render.Draw;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static jcog.Util.sqr;

public class DynamicConceptSpace extends DynamicListSpace<Concept, ConceptWidget> {

    public final NAR nar;
    final Bagregate<Activate> concepts;

    private final Flip<List<ConceptWidget>> next = new Flip(FasterList::new);
    final float bagUpdateRate = 0.25f;
    private final int maxNodes;
    private DurService on;

    volatile static int serial = 0;
    final String spaceID;

    //final StampedLock rw = new StampedLock();


    public TermWidget.TermVis vis;

    public DynamicConceptSpace(NAR nar, @Nullable Iterable<Activate> concepts, int maxNodes, int maxEdgesPerNodeMax) {
        super();

        this.spaceID = DynamicConceptSpace.class + "." + (serial++);

        vis = new ConceptVis2(maxNodes * maxEdgesPerNodeMax);
        this.nar = nar;
        this.maxNodes = maxNodes;

        if (concepts == null)
            concepts = (Iterable) this;

        this.concepts = new Bagregate<Activate>(concepts, maxNodes, bagUpdateRate) {

            @Override
            public void onRemove(PriReference<Activate> value) {
                //ConceptWidget cw = (ConceptWidget) space.get(value.get().id.term().);
                ConceptWidget cw = value.get().id.meta(spaceID);
                if (cw != null) {
                    cw.deactivate();
                }
            }
        };
    }


    final AtomicBoolean updated = new AtomicBoolean(false);

    @Override
    public void start(SpaceGraph<Concept> space) {
        super.start(space);
        on = DurService.on(nar, () -> {
            if (!updated.get() && concepts.update()) {



                updated.set(true);
            }

        }).durs(1);
    }

    @Override
    public synchronized void stop() {
        on.off();
        on = null;
        super.stop();
    }


    @Override
    protected List<ConceptWidget> get() {

        if (updated.get()) {

                    List<ConceptWidget> w;
                w = next.write();
                w.clear();

                concepts.forEach((clink) -> {
                    //ConceptWidget cw = space.getOrAdd(clink.get().id, ConceptWidget::new);
                    Concept cc = clink.get().id;
                    ConceptWidget cw = cc.meta(spaceID, (sid) -> new ConceptWidget(cc));
                    if (cw != null) {

                        cw.activate();

                        cw.pri = clink.priElseZero();
                        w.add(cw);

                    }
                    //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
                });


            vis.accept(next.write());

            next.commit();

            updated.set(false);
        }
        List<ConceptWidget> r = next.read();

        return r;
    }

    public class ConceptVis2 implements TermWidget.TermVis<ConceptWidget>, BiConsumer<ConceptWidget, PriReference<? extends Termed>> {

        static final int TASKLINK = 0;
        static final int TERMLINK = 1;


        public Bag<ConceptWidget.EdgeComponent, ConceptWidget.EdgeComponent> edges;

        int maxEdges;
        public final FloatParam minSize = new FloatParam(1f, 0.1f, 5f);
        public final FloatParam maxSizeMult = new FloatParam(2f, 1f, 5f);
        public final AtomicBoolean showLabel = new AtomicBoolean(true);
        public final FloatParam termlinkOpacity = new FloatParam(1f, 0f, 1f);
        public final FloatParam tasklinkOpacity = new FloatParam(1f, 0f, 1f);
        public final FloatParam lineWidthMax = new FloatParam(1f, 0f, 4f);
        public final FloatParam lineWidthMin = new FloatParam(0.1f, 0f, 4f);
        public final FloatParam separation = new FloatParam(1f, 0f, 6f);
        public final FloatParam lineAlphaMin = new FloatParam(0.1f, 0f, 1f);
        public final FloatParam lineAlphaMax = new FloatParam(0.8f, 0f, 1f);


        public final FloatParam edgeBrightness = new FloatParam(1 / 16f, 0f, 2f);

        public ConceptVis2(int maxEdges) {
            super();
            this.maxEdges = maxEdges;

            this.edges =
                    new ConcurrentArrayBag<>(
                            //new PLinkHijackBag(0, 2);
                            //new PLinkArrayBag<>(maxEdges,
                            //PriMerge.max,
                            //PriMerge.replace,
                            PriMerge.plus,
                            //new UnifiedMap()
                            //new LinkedHashMap()
                            //new LinkedHashMap() //maybe: edgeBagSharedMap
                            maxEdges
                    ) {
                        @Nullable
                        @Override
                        public ConceptWidget.EdgeComponent key(ConceptWidget.EdgeComponent x) {
                            return x;
                        }
                    };
        }


        @Override
        public void accept(List<ConceptWidget> pending) {

            pending.forEach(this::preCollect);

            //float priSum = edges.priSum();

            pending.forEach(c -> {
                c.edges.write().values().forEach(x -> x.inactive = true);
            });

            edges.commit(ee -> {
                ConceptWidget src = ee.src;
                Map<Concept, ConceptWidget.ConceptEdge> eee = src.edges.write();
                if (ee.tgt.active()) {
                    eee.computeIfAbsent(ee.tgt.id, (t) ->
                            new ConceptWidget.ConceptEdge(src, ee.tgt, 0)
                    ).merge(ee);
                } else {
                    ee.delete();
                    eee.remove(ee.tgt.id);
                }
            });
            float termlinkOpac = termlinkOpacity.floatValue();
            float tasklinkOpac = tasklinkOpacity.floatValue();
            float separation = this.separation.floatValue();
            float minLineWidth = this.lineWidthMin.floatValue();
            float MaxEdgeWidth = this.lineWidthMax.floatValue();
            float _lineAlphaMax = this.lineAlphaMax.floatValue();
            float _lineAlphaMin = this.lineAlphaMin.floatValue();
            float lineAlphaMin = Math.min(_lineAlphaMin, _lineAlphaMax);
            float lineAlphaMax = Math.max(lineAlphaMin, _lineAlphaMax);
            pending.forEach(c -> {
                float srcRad = c.radius();
                c.edges.write().values().removeIf(e -> {
                    if (e.inactive)
                        return true;

                    //e.update(termlinkOpac, tasklinkOpac)

                    float edgeSum = (e.termlinkPri + e.tasklinkPri);

                    if (edgeSum >= 0) {

                        float p = e.priElseZero();
                        if (p != p)
                            return true;

                        e.width = minLineWidth + 0.5f * sqr(1 + p * MaxEdgeWidth);

                        float taskish, termish;
                        if (edgeSum > 0) {
                            taskish = e.tasklinkPri / edgeSum * termlinkOpac;
                            termish = e.termlinkPri / edgeSum * tasklinkOpac;
                        } else {
                            taskish = termish = 0.5f;
                        }
                        e.r = 0.05f + 0.65f * (taskish);
                        e.b = 0.05f + 0.65f * (termish);
                        e.g = 0.1f * (1f - (e.r + e.g) / 1.5f);

                        e.a = Util.lerp(p /* * Math.max(taskish, termish) */, lineAlphaMin, lineAlphaMax);

                        //0.05f + 0.9f * Util.and(this.r * tasklinkBoost, this.g * termlinkBoost);

                        e.attraction = 0.5f * e.width / 2f;// + priSum * 0.75f;// * 0.5f + 0.5f;
                        float totalRad = srcRad + e.tgt().radius();
                        e.attractionDist =
                                //4f;
                                (totalRad * separation) + totalRad; //target.radius() * 2f;// 0.25f; //1f + 2 * ( (1f - (qEst)));
                    } else {
                        e.a = -1;
                        e.attraction = 0;
                    }

                    return false;
                });
                c.edges.commit();
            });


        }

        @Override
        public void accept(ConceptWidget src, PriReference<? extends Termed> link) {
            float pri = link.priElseNeg1();
            if (pri < 0)
                return;

            Termed ttt = link.get();
            if (ttt == null)
                return;

            Term tt = ttt.term().conceptual();
            if (!tt.equals(src.id.term())) {
                @Deprecated Concept cc = nar.concept(tt); //TODO generic key should be Term not Concept this would avoid a lookup in the main index
                if (cc != null) {
                    ConceptWidget tgt = cc.meta(spaceID);
                    if (tgt != null && tgt.active()) {
                        //                Concept c = space.nar.concept(tt);
                        //                if (c != null) {

                        int type;
                        if (!!(ttt instanceof Task)) {
                            type = TASKLINK;
                        } else {
                            type = TERMLINK;
                        }

                        edges.putAsync(new ConceptWidget.EdgeComponent(link, src, tgt, type, pri * edgeBrightness.floatValue()));
                        //new PLinkUntilDeleted(ate, pri)
                        //new PLink(ate, pri)

                        //                }
                    }
                }
            }
        }


        public void preCollect(ConceptWidget cw) {
            float p = cw.pri;


            //long now = space.now();
//            float b = conceptWidget.concept.beliefs().eviSum(now);
//            float g = conceptWidget.concept.goals().eviSum(now);
//            float q = conceptWidget.concept.questions().priSum();

            //sqrt because the area will be the sqr of this dimension

            float volume = 1f / (1f + cw.id.complexity());
            float density = 5f * volume;
            float ep = 1 + p;
            float minSize = this.minSize.floatValue();
            float nodeScale = minSize + (ep * ep) * maxSizeMult.floatValue() * volume /* ^1/3? */;
            //1f + 2f * p;

            boolean atomic = (cw.id.op().atomic);
            if (atomic)
                nodeScale /= 2f;

            if (cw.shape instanceof SphereShape) {
                float r = nodeScale;
                cw.scale(r, r, r);
            } else {
                float l = nodeScale * 1.618f;
                float w = nodeScale;
                float h = 1; //nodeScale / (1.618f * 2);
                cw.scale(l, w, h);
            }


            if (cw.body != null) {
                cw.body.setMass(nodeScale * nodeScale * nodeScale /* approx */ * density);
                cw.body.setDamping(0.99f, 0.9f);

            }

//            Draw.hsb(
//                    (tt.op().ordinal() / 16f),
//                    0.5f + 0.5f / tt.volume(),
//                    0.3f + 0.2f * p,
//                    0.9f, conceptWidget.shapeColor);

            cw.front.visible(showLabel.get());

//            Concept c = cw.id;
//            if (c != null) {
////                Truth belief = c.belief(space.now, space.dur);
////                if (belief == null) belief = zero;
////                Truth goal = c.goal(space.now, space.dur);
////                if (goal == null) goal = zero;
////
////                float angle = 45 + belief.freq() * 180f + (goal.freq() - 0.5f) * 90f;
//                //angle / 360f
            Draw.colorHash(cw.id, cw.shapeColor);// * or(belief.conf(), goal.conf()), 0.9f, cw.shapeColor);

            cw.edges.write().clear();
            cw.id.tasklinks().forEach(x -> this.accept(cw, x));
            cw.id.termlinks().forEach(x -> this.accept(cw, x));


        }
    }

}

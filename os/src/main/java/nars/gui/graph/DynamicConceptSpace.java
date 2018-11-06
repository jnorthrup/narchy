package nars.gui.graph;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Offs;
import jcog.math.FloatRange;
import jcog.math.MutableEnum;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.bag.util.Bagregate;
import jcog.pri.op.PriMerge;
import jcog.util.Flip;
import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.gui.concept.ConceptSurface;
import nars.link.TaskLink;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.phys.shape.SphereShape;
import spacegraph.space3d.widget.DynamicListSpace;
import spacegraph.space3d.widget.SpaceWidget;
import spacegraph.video.Draw;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static jcog.Util.sqr;
import static nars.gui.graph.DynamicConceptSpace.ColorNode.Hash;

public class DynamicConceptSpace extends DynamicListSpace<Concept> {

    public final NAR nar;
    protected final Bagregate<Concept> concepts;

    private final Flip<List<ConceptWidget>> next = new Flip(FasterList::new);
    final float bagUpdateRate = 0.25f;


    volatile static int serial = 0;
    final String spaceID;

    


    public SpaceWidget.TermVis vis;

    private DurService onDur;
    private Offs onClear;
    private DurService updater;

    public DynamicConceptSpace(NAR nar, @Nullable Iterable<? extends PriReference<Concept>> concepts, int maxNodes, int maxEdgesPerNodeMax) {
        super();

        this.spaceID = DynamicConceptSpace.class + "." + (serial++);

        vis = new ConceptVis2(maxNodes * maxEdgesPerNodeMax);
        this.nar = nar;

        if (concepts == null)
            concepts = (Iterable) this;

        this.concepts = new Bagregate<Concept>(concepts, maxNodes, bagUpdateRate) {

            @Override
            public void onRemove(PriReference<Concept> value) {
                
                ConceptWidget cw = value.get().meta(spaceID);
                if (cw != null) {
                    cw.deactivate();
                }
            }
        };

    }


    final AtomicBoolean updated = new AtomicBoolean(false);

    @Override
    public void start(SpaceGraphPhys3D<Concept> space) {
        synchronized (this) {
            super.start(space);
            init();
            updater = DurService.on(nar, nn->{
               concepts.commit();
            });
        }

    }


    public void init() {
        synchronized (this) {
            if (onDur != null)
                onDur.off();
            if (onClear != null)
                onClear.off();

            onDur = DurService.on(nar, () -> {
                if (concepts.commit()) {
                    updated.set(true);
                }

            }).durs(1);
            this.onClear = new Offs(nar.eventClear.on(() -> {
                synchronized (this) {
                    next.write().clear();
                    next.commit();
                    concepts.clear();
                }
            }));
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            onDur.off();
            onDur = null;
            onClear.off();
            onClear = null;
            updater.off();
            updater = null;
            super.stop();
        }
    }


    @Override
    protected List<ConceptWidget> get() {

        if (updated.get()) {

                    List<ConceptWidget> w;
                w = next.write();
                w.clear();

                concepts.forEach((clink) -> {
                    
                    Concept cc = clink.get();
                    ConceptWidget cw = cc.meta(spaceID, (sid) -> new ConceptWidget(cc) {
                        @Override
                        protected void onClicked(PushButton b) {
                            SpaceGraph.window(new ConceptSurface(id.term(), nar), 800, 700);
                        }
                    });
                    if (cw != null) {

                        cw.activate();

                        cw.pri = clink.priElseZero();
                        w.add(cw);

                    }
                    
                });


            vis.accept(next.write());

            next.commit();

            updated.set(false);
        }

        return next.read();
    }

    public enum ColorNode {
        Priority {
            @Override
            public void color(ConceptWidget cw, NAR nar) {


                float p = cw.pri;
                p = 0.25f + 0.75f * p;
                float[] sc = cw.shapeColor;
                sc[0] = sc[1] = sc[2] = p;
            }
        },

        Belief {
            @Override
            public void color(ConceptWidget cw, NAR nar) {
                Truth beliefTruth = cw.id.beliefs().truth(nar.time(), nar);
                float beliefTruthFreq;
                if (beliefTruth != null) {
                    beliefTruthFreq = beliefTruth.freq();
                    Draw.colorUnipolarHue(cw.shapeColor, beliefTruthFreq, 0.25f, 0.75f, 0.1f + beliefTruth.conf() * 0.9f);
                } else {
                    Draw.colorRGBA(cw.shapeColor, 0.5f, 0.5f, 0.5f, 0.1f);
                }
            }
        },

        Hash {
            @Override
            public void color(ConceptWidget cw, NAR nar) {
                Draw.colorHash(cw.id.hashCode(), cw.shapeColor, 0.1f + 0.9f * cw.pri);
            }
        },
        Op {
            @Override
            public void color(ConceptWidget cw, NAR nar) {
                Draw.colorHash(cw.id.op().hashCode(), cw.shapeColor, 0.1f + 0.9f * cw.pri);
            }
        },
        Complexity {
            @Override
            public void color(ConceptWidget cw, NAR nar) {
                float c = Util.unitize(cw.id.volume() / 32f);
                Draw.colorUnipolarHue(cw.shapeColor, c, 0.1f, 0.9f);
            }
        };

        abstract public void color(ConceptWidget cw, NAR nar);
    }

    public class ConceptVis2 implements SpaceWidget.TermVis<ConceptWidget>, BiConsumer<ConceptWidget, PriReference<? extends Termed>> {

        static final int TASKLINK = 0;
        static final int TERMLINK = 1;



        public Bag<ConceptWidget.EdgeComponent, ConceptWidget.EdgeComponent> edges;

        int maxEdges;
        public final FloatRange minSize = new FloatRange(1f, 0.1f, 5f);
        public final FloatRange maxSizeMult = new FloatRange(2f, 1f, 5f);
        public final AtomicBoolean showLabel = new AtomicBoolean(true);
        public final FloatRange termlinkOpacity = new FloatRange(0.5f, 0f, 1f);
        public final FloatRange tasklinkOpacity = new FloatRange(0.5f, 0f, 1f);
        public final FloatRange lineWidthMax = new FloatRange(1f, 0f, 4f);
        public final FloatRange lineWidthMin = new FloatRange(0.1f, 0f, 4f);
        public final FloatRange separation = new FloatRange(1f, 0f, 6f);
        public final FloatRange lineAlphaMin = new FloatRange(0.1f, 0f, 1f);
        public final FloatRange lineAlphaMax = new FloatRange(0.8f, 0f, 1f);
        public final MutableEnum<ColorNode> colorNode = new MutableEnum(Hash);
        public final FloatRange edgeBrightness = new FloatRange(1 / 16f, 0f, 2f);

        public ConceptVis2(int maxEdges) {
            super();
            this.maxEdges = maxEdges;

            this.edges =

                    
                    

                    
                    new PLinkArrayBag(
                            


                            PriMerge.plus,



                            maxEdges

                    );







        }


        @Override
        public void accept(List<ConceptWidget> pending) {

            pending.forEach(this::preCollect);

            

            pending.forEach(c -> {
                c.edges.write().forEachValue(x -> x.inactive = true);
            });

            edges.commit(ee -> {
                ConceptWidget src = ee.src();
                Map<Concept, ConceptWidget.ConceptEdge> eee = src.edges.write();
                ConceptWidget tgt = ee.tgt();
                if (tgt.active()) {
                    eee.computeIfAbsent(tgt.id, (t) ->
                            new ConceptWidget.ConceptEdge(src, tgt, 0)
                    ).merge(ee);
                } else {
                    ee.delete();
                    eee.remove(tgt.id);
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
                c.edges.write().removeIf(e -> {
                    if (e.inactive)
                        return true;

                    

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

                        

                        e.attraction = 0.5f * e.width / 2f;
                        float totalRad = srcRad + e.tgt().radius();
                        e.attractionDist =
                                
                                (totalRad * separation) + totalRad; 
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
            accept(src, link.get().term(), pri, TERMLINK);
        }

        public void accept(ConceptWidget src, TaskLink link) {
            float pri = link.priElseNeg1();
            if (pri < 0)
                return;
            accept(src, link.term(), pri, TASKLINK);
        }

        public void accept(ConceptWidget src, Term term, float pri, int type) {

            if (term.op().conceptualizable) {
                Term tt = term.concept();
                if (!tt.equals(src.id.term())) {
                    @Deprecated Concept cc = nar.concept(tt); 
                    if (cc != null) {
                        ConceptWidget tgt = cc.meta(spaceID);
                        if (tgt != null && tgt.active()) {
                            
                            



                            edges.putAsync(new ConceptWidget.EdgeComponent(src, tgt, type, pri * edgeBrightness.floatValue()));
                            
                            

                            
                        }
                    }
                }
            }
        }


        void preCollect(ConceptWidget cw) {
            float p = cw.pri;


            




            

            float volume = 1f / (1f + cw.id.complexity());
            float density = 5f / (1f + volume);
            float ep = 1 + p;
            float minSize = this.minSize.floatValue();
            float nodeScale = minSize + (ep * ep) * maxSizeMult.floatValue()
                    ;
            

            boolean atomic = (cw.id.op().atomic);
            if (atomic)
                nodeScale /= 2f;

            if (cw.shape instanceof SphereShape) {
                float r = nodeScale;
                cw.scale(r, r, 0.5f);
            } else {
                float l = nodeScale * 1.618f;
                float w = nodeScale;
                float h = 1; 
                cw.scale(l, w, h);
            }


            if (cw.body != null) {
                cw.body.setMass(nodeScale * nodeScale * nodeScale /* approx */ * density);
                cw.body.setDamping(0.99f, 0.9f);

            }







            cw.front.visible(showLabel.get());











            colorNode.get().color(cw, nar);


            cw.edges.write().clear();
            cw.id.tasklinks().forEach(x -> this.accept(cw, x));
//            cw.id.termlinks().forEach(x -> this.accept(cw, x));


        }
    }

}

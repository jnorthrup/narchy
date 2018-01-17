package nars.gui;

import jcog.event.On;
import nars.gui.graph.DynamicConceptSpace;
import nars.gui.graph.EdgeDirected;
import nars.term.Term;
import spacegraph.AbstractSpace;
import spacegraph.Active;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.layout.Flatten;
import spacegraph.phys.util.Animated;
import spacegraph.space.ListSpace;
import spacegraph.widget.meta.AutoSurface;

import java.util.List;

import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.grid;


/**
 * thread-safe visualization of a set of spatials, and
 * calls to their per-frame rendering animation
 */
public abstract class DynamicListSpace<X,Y extends Spatial<X>> extends ListSpace<X,Y> implements Animated {


    //private final TriConsumer<NAR, SpaceGraph<Term>, List<Spatial<X>>> collect;
    private On on;

    public SpaceGraph<X> space;


    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();



    public DynamicListSpace() {
        super();

//        nar.onCycle(x -> {
//            updateIfNotBusy(this::update);
//
//        });
    }

    @Override
    public boolean animate(float dt) {

        //updateIfNotBusy(this::render);

        return true;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        //if (on!=null) {
            space.dyn.removeAnimation(this);
            active.forEach(space::remove);
            active.clear();
            on.off();
            on = null;
        //}
    }

    public final boolean running() {
        return on != null;
    }



    @Override
    public void start(SpaceGraph<X> space) {
        this.space = space;
        space.dyn.addAnimation(this);
        //on = nar.onCycle(nn -> updateIfNotBusy(this::update));
    }


    @Override
    public void update(SpaceGraph<X> s) {

        List<? extends Spatial<X>> prev = this.active;

        prev.forEach(Active::deactivate);

        List next = get();

        //System.out.println(space.dyn.summary() + " " +  prev.size() + " prev " + next.size() + " next");

        this.active = next;

        //disable the undisplayed
        prev.forEach(x -> {
            if (!x.preactive)
                x.order = -1;
        });

        super.update(s);
    }


    abstract protected List<? extends Spatial<X>> get();



    /** displays in a window with default force-directed options */
    public SpaceGraph<Term> show(int w, int h, boolean flat) {


//                        new SpaceTransform<Term>() {
//                            @Override
//                            public void update(SpaceGraph<Term> g, AbstractSpace<Term, ?> src, float dt) {
//                                float cDepth = -9f;
//                                src.forEach(s -> {
//                                    ((SimpleSpatial)s).moveZ(
//                                            s.key.volume() * cDepth, 0.05f );
//                                });
//                            }
//                        }

                //new Flatten()
//                        new Flatten() {
//                            protected void locate(SimpleSpatial s, v3 f) {
//                                f.set(s.x(), s.y(), 10 - ((Term) (s.key)).volume() * 1);
//                            }
//                        }


                //new Spiral()
//                        //new FastOrganicLayout()




        AbstractSpace ss = flat ? with(new Flatten(0.25f, 0.25f)) : this;
        SpaceGraph<Term> s = new SpaceGraph<>(ss);

        EdgeDirected fd = new EdgeDirected();
        s.dyn.addBroadConstraint(fd);


        //s.ortho(Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f));

        if (this instanceof DynamicConceptSpace) {
            window(
                    grid(new AutoSurface<>(fd)
                            , new AutoSurface<>(((DynamicConceptSpace) this).vis)
                    ),
                    400, 400);
        }

        //Vis.conceptsWindow2D
        s
                //.ortho(logConsole( n, 90, 40, new FloatParam(0f)).opacity(0.25f))
                .camPos(0, 0, 90)
                .show(w, h);

        return s;

    }


}

//    public static ConceptWidget newLinkWidget(final NAR nar, SpaceGraph<Term> space, final ConceptWidget core, Term SRC, Term TARGET, BLink bt, boolean task) {
//
//
//
//        @NotNull Compound vTerm = $.p(L, SRC, TARGET);
//        SimpleSpatial targetSpatial = (SimpleSpatial) space.getIfActive(TARGET);
//        if (targetSpatial!=null) {
//            ConceptWidget termLink = space.update(vTerm,
//                    t -> new ConceptWidget(t, nar) {
//
//                        //                                @Override
////                                public Dynamic newBody(boolean collidesWithOthersLikeThis) {
////                                    shape = new SphereShape(.5f);
////                                    Dynamic bb = super.newBody(collidesWithOthersLikeThis);
////                                    return bb;
////                                }
//
//
//                        @Override
//                        protected String label(Term term) {
//                            return "";
//                        }
//
//                        @Override
//                        public void update(SpaceGraph<Term> s) {
//                            super.update(s);
//
//                            clearEdges();
//
//
//                            EDraw in = addEdge(bt, core, task);
//                            in.attraction = 0.25f;
//
//
//                            EDraw out = addEdge(bt, targetSpatial, task);
//                            out.attraction = 1f + (0.5f * bt.priIfFiniteElseZero());
//
//
//                        }
//                    });
//            if (termLink!=null) {
//                termLink.pri = bt.priIfFiniteElseZero();
//            }
//            return termLink;
//        }
//
//        return null;
//
//    }


package spacegraph.space;

import spacegraph.AbstractSpace;
import spacegraph.Active;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.layout.EdgeDirected;
import spacegraph.layout.Flatten;

import java.util.List;
import java.util.function.Consumer;


/**
 * thread-safe visualization of a set of spatials, and
 * calls to their per-frame rendering animation
 */
public abstract class DynamicListSpace<X> extends AbstractSpace<X> {

    private SpaceGraph<X> space;
    public List<? extends Spatial<X>> active = List.of();


    @Override
    public void start(SpaceGraph<X> space) {
        this.space = space;
    }


    @Override
    public void stop() {
        synchronized (this) {
            super.stop();
            //if (on!=null) {
            active.forEach(space::remove);
            active.clear();
            //}
        }
    }

    @Override
    public void forEach(Consumer<? super Spatial<X>> each) {
        active.forEach(each::accept);
    }

    @Override
    protected void update(SpaceGraph<X> s, long dtMS) {

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

        active.forEach(x -> x.update(s.dyn));

        super.update(s, dtMS);
    }

    abstract protected List<? extends Spatial<X>> get();



    /** displays in a window with default force-directed options */
    @Deprecated public SpaceGraph show(int w, int h, boolean flat) {


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



        //new Spiral()
//                        //new FastOrganicLayout()




        AbstractSpace ss = flat ? with(new Flatten(0.25f, 0.25f)) : this;
        SpaceGraph s = new SpaceGraph<>(
                ss
        );

        EdgeDirected fd = new EdgeDirected();
        s.dyn.addBroadConstraint(fd);
        fd.attraction.set(fd.attraction.get() * 8);


        //s.ortho(Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f));

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


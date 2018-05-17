package nars.concept.dynamic;

import jcog.Util;
import jcog.math.LongInterval;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.compound.util.Conj;
import nars.truth.Truth;
import nars.truth.func.BeliefFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel implements BiFunction<DynTruth,NAR,Truth> {


    @Nullable
    public DynTruth eval(final Term superterm, boolean beliefOrGoal, long start, long end, NAR n) {

        assert(superterm.op()!=NEG);

        DynTruth d = new DynTruth(4);

        return components(superterm, start, end, (Term concept, long subStart, long subEnd)->{
            boolean negated = concept.op() == Op.NEG;
            if (negated)
                concept = concept.unneg();

            Concept subConcept =
                    //n.concept(it);
                    n.conceptualize(concept); //force creation of concept, which if dynamic, could provide data for use here

            @Nullable Task bt;
            if (!(subConcept instanceof TaskConcept)) {
//                if (Param.DEBUG) {
//                    if (!(subConcept == null || !(subConcept instanceof NodeConcept)))
//                        throw new RuntimeException(it + " does not reference a TaskConcept: " + subConcept);
//                }
                return false;
            } else {

                BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);

                //TODO if ETERNAL , intersects test isnt necessary
                bt = table.match(subStart, subEnd, concept, x->
                    /* x.intersects(subStart, subEnd) && */ d.doesntOverlap(x), n
                );
                if (bt != null) {

                        /** project to a specific time, and apply negation if necessary */
                        bt = Task.project(bt, subStart, subEnd, n, negated);

                } else {
                    //bt = null; //missing truth, but yet still may be able to conclude a result
                    return false;
                }

            }

            return add(bt, d);

        }) ? d : null;
    }

    @FunctionalInterface
    public interface ObjectLongLongPredicate<T> {
        boolean accept(T object, long start, long end);
    }

    abstract public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    protected boolean add(@Nullable Task bt, DynTruth d) {
        return d.add(bt);
    }





    /**
     * used to reconstruct a dynamic term from some or all components
     */
    abstract public Term construct(Term superterm, List<TaskRegion> c);




    abstract public static class Intersection extends DynamicTruthModel {


        @Override
        public Truth apply(DynTruth l, NAR nar) {

            //compute the lazy computed truth, if possible
            int n = l.size();
            int avail = 0;
            for (int i = 0; i < n; i++) {
                LongInterval li = l.get(i);
                if (li!=null) {
                    if ((((Task) li)).truth() == null)
                        l.set(i, null);
                    else
                        avail++;
                }
            }
            if (avail == 0)
                return null;

            int[] order = new int[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }


            Truth y = null;
            int considered = 0;
            for (int i = 0; i < n; i++) {
                TaskRegion ii = l.get(order[i]);
                if (ii == null)
                    continue;

                Truth x = ((Task)ii).truth(); //either the default truth, or the cached proxied/projected truth
                considered++;

                if (negateFreq())
                    x = x.neg();

                if (y == null) {
                    y = x;
                } else {
                    y = BeliefFunction.Intersection.apply(y, x, nar, Float.MIN_NORMAL);
                    if (y == null)
                        return null;
                }
            }


            if (considered != n) {
                return null;
//                if (f >= freqRes)
//                    return null; //missing components without having reached blackhole asymptote f=0
//                else {
//                    l.removeNulls(); //remove nulls and continue with only the components necessary
//                }
            }

            return y.negIf(negateFreq());
        }

        protected boolean negateFreq() {
            return false;
        }

    }

    public static class SectIntersection extends Intersection {
        /** ahead of time computed components */
        private final Term[] comp;

        public SectIntersection(Term[] comp) {
            this.comp = comp;
        }

        @Override
        public Term construct(Term superterm, List<TaskRegion> components) {
            //SECT's
            int n = components.size();
            if (n == 1) {
                return components.get(0).task().term();
            }
            Term[] ct = Util.map(0, n, c -> components.get(c).task().term(), Term[]::new);
            if (n == 2) {
                return inhConstruct2(superterm, SECTe.bit | SECTi.bit, ct);
            } else {
                return inhConstructN(superterm, SECTe.bit | SECTi.bit, ct);
            }
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            for (Term x : comp) {
                if (!each.accept(x, start, end)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * conf is multiplied, freq is OR'd
     */
    public static final class Union extends SectIntersection {

        public Union(Term... comp) {
            super(comp);
            assert (comp.length > 1);
        }

        @Override
        protected boolean negateFreq() {
            return true;
        }

    }

    public static class ConjIntersection extends Intersection {

        public static final DynamicTruthModel the = new ConjIntersection();

        private ConjIntersection() {

        }

        @Override
        public Term construct(Term superterm, List<TaskRegion> components) {
            Conj c = new Conj();
            //long estVolume = ((FasterList<TaskRegion>)components).sumOfInt(xt -> ((Task)xt).term().volume());
            //TODO heuristic for range sampling parameters
            for (TaskRegion t : components) {
                if (!c.add(((Task)t).term(), t.start(), t.end(), 1, 1))
                    break; //TODO maybe try with less aggressive sampling, if sampling was used
            }
            return c.term(); //null, false, true...
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            int superDT = superterm.dt();
            boolean xternal = superDT ==XTERNAL;
            boolean dternal = superDT ==DTERNAL;
            LongObjectPredicate<Term> sub;
            if (xternal || dternal) {
                //the entire range
                sub = (whenIgnored, event) -> each.accept(event, start, end);
            } else {
                //specific sub-range
                long range = start!=ETERNAL ? end-start : 0;
                sub = (when, event) -> each.accept(event, when, when+range);
            }

            return superterm.eventsWhile((when,event)->{
                if (event!=superterm) //prevent fatal loop
                    return sub.accept(when, event);
                else
                    return false; //fail
                }, start, !xternal && !dternal, dternal, xternal, 0);
        }
    }

    public static class Difference extends DynamicTruthModel {
        private final Term[] comp;

        public Difference(Term[] xy) {

//            assert (!(xy[0] instanceof Bool) && !(xy[1] instanceof Bool));
//            assert (!(xy[0] instanceof Variable) && !(xy[1] instanceof Variable)) :
//                    xy[0] + " or " + xy[1] + " is a variable";

            this.comp = xy;
        }

        public Difference(Term x, Term y) {
            this(new Term[]{x, y});
        }

        @Override
        protected boolean add(@Nullable Task bt, DynTruth d) {
            if (bt == null)
                return false;

            return super.add(bt, d);
        }

        @Override
        public Term construct(Term superterm, List<TaskRegion> components) {

            Term a = components.get(0).task().term();
            Term b = components.get(1).task().term();

            if (superterm.op() == INH) {
                return inhConstruct2(superterm, DIFFe.bit | DIFFi.bit, a, b);
            } else if (superterm.op() == DIFFe) {
                //raw difference
                return Op.DIFFe.compound(DTERNAL, new Term[]{a, b});
            }

            throw new RuntimeException();
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            for (Term x : comp) {
                if (!each.accept(x, start, end)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Truth apply(DynTruth d, NAR n) {
            assert (d.size() == 2);
            Truth a = ((Task)d.get(0)).truth();
            if (a == null)
                return null;
            Truth b = ((Task)d.get(1)).truth();
            if (b == null)
                return null;

            return BeliefFunction.Difference.apply(a, b, n, Float.MIN_NORMAL);
        }
    }

    private static Term inhConstruct2(Term superterm, int bits, Term... components) {
        {
            Term subj = superterm.sub(0);
            Op so = subj.op();
            if (so.isAny(bits))
                return INH.the(so.the(components[0].sub(0), components[1].sub(0)), superterm.sub(1));
        }

        {
            Term pred = superterm.sub(1);
            Op po = pred.op();
            if (po.isAny(bits))
                return INH.the(superterm.sub(0), po.the(components[0].sub(1), components[1].sub(1)));
        }

        throw new UnsupportedOperationException();
    }

    private static Term inhConstructN(Term superterm, int bits, Term[] components) {
        {
            Term subj = superterm.sub(0);
            Op so = subj.op();
            if (so.isAny(bits))
                return INH.the(
                        so.the(Util.map(x -> x.sub(0), new Term[components.length], components)),
                        superterm.sub(1));
        }

        {
            Term pred = superterm.sub(1);
            Op po = pred.op();
            if (po.isAny(bits))
                return INH.the(
                        superterm.sub(0),
                        po.the(Util.map(x -> x.sub(1), new Term[components.length], components))
                );
        }

        throw new UnsupportedOperationException();
    }

}
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
                    
                    n.conceptualize(concept); 

            @Nullable Task bt;
            if (!(subConcept instanceof TaskConcept)) {




                return false;
            } else {

                BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);

                
                bt = table.match(subStart, subEnd, concept, x->
                    /* x.intersects(subStart, subEnd) && */ d.doesntOverlap(x), n
                );
                if (bt != null) {

                    /** project to a specific time, and apply negation if necessary */
                    bt = Task.project(true, bt, subStart, subEnd, n, negated);

                } else {
                    
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

                Truth x = ((Task)ii).truth(); 
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
            
            
            for (TaskRegion t : components) {
                if (!c.add(((Task)t).term(), t.start(), t.end(), 1, 1))
                    break; 
            }
            return c.term(); 
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            int superDT = superterm.dt();
            boolean xternal = superDT ==XTERNAL;
            boolean dternal = superDT ==DTERNAL;
            LongObjectPredicate<Term> sub;
            if (xternal || dternal) {
                
                sub = (whenIgnored, event) -> each.accept(event, start, end);
            } else {
                
                long range = start!=ETERNAL ? end-start : 0;
                sub = (when, event) -> each.accept(event, when, when+range);
            }

            return superterm.eventsWhile((when,event)->{
                if (event!=superterm) 
                    return sub.accept(when, event);
                else
                    return false; 
                }, start, !xternal && !dternal, dternal, xternal, 0);
        }
    }

    public static class Difference extends DynamicTruthModel {
        private final Term[] comp;

        public Difference(Term[] xy) {





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
package nars.concept.dynamic;

import jcog.Util;
import jcog.list.FasterList;
import jcog.math.LongInterval;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.compound.util.Conj;
import nars.term.compound.util.Image;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel implements BiFunction<DynTruth, NAR, Truth> {


    public static final DynamicTruthModel UnionSubj = new SectIntersection(true, true);
    public static final DynamicTruthModel IsectSubj = new SectIntersection(false, true);
    public static final DynamicTruthModel UnionPred = new SectIntersection(true, false);
    public static final DynamicTruthModel IsectPred = new SectIntersection(false, false);
    public static final Difference DiffRoot = new Difference() {

        @Override
        public Term reconstruct(Term superterm, List<TaskRegion> components) {
            return Op.DIFFe.compound(DTERNAL, new Term[]{
                    components.get(0).task().term(),
                    components.get(1).task().term()});
        }

    };
    public static final Difference DiffSubj = new DiffInh(true);
    public static final Difference DiffPred = new DiffInh(false);
    public static final DynamicTruthModel ImageIdentity = new DynamicTruthModel() {

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            return each.accept(Image.imageNormalize(superterm), start, end);
        }

        @Override
        public Term reconstruct(Term superterm, List<TaskRegion> c) {
            return superterm; //exact
        }

        @Override
        public Truth apply(DynTruth taskRegions, NAR nar) {
            return ((Task) taskRegions.get(0)).truth();
        }
    };

    static Term inhComponent(boolean subjOrPred, Term subterm, Term common) {
        Term s, p;
        if (subjOrPred) {
            s = subterm;
            p = common;
        } else {
            s = common;
            p = subterm;
        }
        if (s == null || p == null)
            throw new NullPointerException();
        return INH.the(s, p);
    }

    static Term inhCommonComponent(boolean subjOrPred, Term superterm) {
        return subjOrPred ? superterm.sub(1) : superterm.sub(0);
    }

    @Nullable
    static Term[] inhReconstruct(boolean subjOrPred, List<TaskRegion> components) {

        //extract passive term and verify they all match (could differ temporally, for example)
        Term[] common = new Term[1];
        boolean extOrInh = subjOrPred;
        if (!((FasterList<TaskRegion>) components).allSatisfy(tr -> {
            Term t = tr.task().term();
            Term p = common[0];
            if (p == null) {
                common[0] = t.sub(extOrInh ? 1 : 0 /* reverse */);
                return true;
            } else {
                return p.equals(t.sub(extOrInh ? 1 : 0 /* reverse */));
            }
        }) || (common[0] == null))
            return null; //differing passive component; TODO this can be detected earlier, before truth evaluation starts

        Term[] subs = Util.map(0, components.size(), c -> {
            return components.get(c).task().term().sub(extOrInh ? 0 : 1);
        }, Term[]::new);
        return subs;
    }

    @Nullable
    public DynTruth eval(final Term superterm, boolean beliefOrGoal, long start, long end, boolean timeFlexible, NAR n) {

        assert (superterm.op() != NEG);

        DynTruth d = new DynTruth(4);

        return components(superterm, start, end, (Term concept, long subStart, long subEnd) -> {
            boolean negated = concept.op() == Op.NEG;
            if (negated)
                concept = concept.unneg();

            Concept subConcept =

                    n.conceptualizeDynamic(concept);

            @Nullable Task bt;
            if (!(subConcept instanceof TaskConcept)) {


                return false;
            } else {

                BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);


                bt = table.match(subStart, subEnd, concept, x ->
                        /* x.intersects(subStart, subEnd) && */ d.doesntOverlap(x), n
                );
                if (bt != null) {

                    /** project to a specific time, and apply negation if necessary */
                    bt = Task.project(timeFlexible, bt, subStart, subEnd, n, negated);

                } else {

                    return false;
                }

            }

            return add(bt, d);

        }) ? d : null;
    }

    abstract public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each);

    ;

    protected boolean add(@Nullable Task bt, DynTruth d) {
        return d.add(bt);
    }

    /**
     * used to reconstruct a dynamic term from some or all components
     */
    abstract public Term reconstruct(Term superterm, List<TaskRegion> c);

    @FunctionalInterface
    public interface ObjectLongLongPredicate<T> {
        boolean accept(T object, long start, long end);
    }

    abstract public static class Intersection extends DynamicTruthModel {


        @Override
        public Truth apply(DynTruth l, NAR nar) {


            int n = l.size();
            int avail = 0;
            for (int i = 0; i < n; i++) {
                LongInterval li = l.get(i);
                if (li != null) {
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

                Truth x = ((Task) ii).truth();
                considered++;

                if (negateFreq())
                    x = x.neg();

                if (y == null) {
                    y = x;
                } else {
                    y = NALTruth.Intersection.apply(y, x, nar, Float.MIN_NORMAL);
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

    final static class SectIntersection extends Intersection {

        final boolean union, subjOrPred;

        private SectIntersection(boolean union, boolean subjOrPred) {
            this.union = union;
            this.subjOrPred = subjOrPred;
        }

        @Override
        protected boolean negateFreq() {
            return union;
        }

        @Override
        public Term reconstruct(Term superterm, List<TaskRegion> components) {
            return inhReconstruct(superterm, components, subjOrPred);
        }



        protected final boolean subjOrPred() {
            return subjOrPred;
        }

        public final boolean union() {
            return union;
        }


        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Term common = inhCommonComponent(subjOrPred, superterm);
            Term decomposed = inhCommonComponent(!subjOrPred, superterm);
            return decomposed.subterms().AND(y ->
                    each.accept(inhComponent(subjOrPred, y, common), start, end)
            );
        }


    }

    public static class ConjIntersection extends Intersection {

        public static final DynamicTruthModel the = new ConjIntersection();

        private ConjIntersection() {

        }

        @Override
        public Term reconstruct(Term superterm, List<TaskRegion> components) {
            Conj c = new Conj();


            for (TaskRegion t: components) {
                if (!c.add(((Task) t).term(), t.start(), t.end(), 1, 1))
                    break;
            }
            return c.term();
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            int superDT = superterm.dt();


            if ((superDT == DTERNAL || superDT == XTERNAL) && !superterm.hasAny(CONJ)) {
                /* simple case: */
                return superterm.subterms().AND(event ->
                        each.accept(event, start, end)
                );
            } else {

                boolean xternal = superDT == XTERNAL;
                boolean dternal = superDT == DTERNAL;
                LongObjectPredicate<Term> sub;
                if (xternal || dternal) {
                    //propagate start,end to each subterm.  allowing them to match freely inside
                    sub = (whenIgnored, event) -> each.accept(event, start, end);
                } else {
                    //??subterm refrences a specific point as a result of event time within the term. so start/end range gets collapsed at this point
                    long range = (long) (end - start);
                    sub = (when, event) -> each.accept(event, when, when + range);
                }

                return superterm.eventsWhile((when, event) -> {
                            //if (event!=superterm)
                            return sub.accept(when, event);
                            //else
                            //return false;
                        }, start, superDT == 0, superDT == DTERNAL,
                        true /* always decompose xternal */, 0);
            }
        }
    }

    abstract static class Difference extends DynamicTruthModel {


        @Override
        public Truth apply(DynTruth d, NAR n) {
            assert (d.size() == 2);
            Truth a = ((Task) d.get(0)).truth();
            if (a == null)
                return null;
            Truth b = ((Task) d.get(1)).truth();
            if (b == null)
                return null;

            return NALTruth.Difference.apply(a, b, n, Float.MIN_NORMAL);
        }


        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            return each.accept(superterm.sub(0), start, end) && each.accept(superterm.sub(1), start, end);
        }
    }

    static class DiffInh extends Difference {
        final boolean subjOrPred;

        DiffInh(boolean subjOrPred) {
            this.subjOrPred = subjOrPred;
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Term common = inhCommonComponent(subjOrPred, superterm);
            Term decomposed = inhCommonComponent(!subjOrPred, superterm);
            return each.accept(inhComponent(subjOrPred, decomposed.sub(0), common), start, end) &&
                    each.accept(inhComponent(subjOrPred, decomposed.sub(1), common), start, end);
        }

        @Override
        public Term reconstruct(Term superterm, List<TaskRegion> c) {
            return inhReconstruct(superterm, c, subjOrPred);
        }
    }

    @Nullable
    static public Term inhReconstruct(Term superterm, List<TaskRegion> components, boolean subjOrPred) {
        Term superSect = superterm.sub(subjOrPred ? 0 : 1);

        if (!Param.DEBUG) {
            //elide reconstruction when superterm will not differ by temporal terms
            //TODO improve
            if (superSect.subs() == components.size() && ((FasterList<TaskRegion>) components).allSatisfy(t -> t != null && !((Task)t).term().sub(subjOrPred ? 0 : 1).isTemporal())) {
                if (!superSect.isTemporal())
                    return superterm;
            }
        }


        Term[] subs = inhReconstruct(subjOrPred, components);
        if (subs == null)
            return null;


        Term sect = superSect.op().the(subs);
        Term common = superterm.sub(subjOrPred ? 1 : 0);
        Term x = subjOrPred ? INH.the(sect, common) : INH.the(common, sect);
        if (x.op() != INH)
            return null;
        return x;
    }

}
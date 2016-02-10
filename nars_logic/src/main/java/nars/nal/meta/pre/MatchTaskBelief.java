package nars.nal.meta.pre;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Ints;
import com.gs.collections.api.map.ImmutableMap;
import nars.Global;
import nars.Op;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.BooleanCondition;
import nars.nal.meta.PremiseMatch;
import nars.nal.meta.TaskBeliefPair;
import nars.nal.meta.constraint.AndConstraint;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.op.MatchTerm;
import nars.nal.meta.op.SubTermOp;
import nars.nal.meta.op.SubTermStructure;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.gs.collections.impl.factory.Maps.immutable;


@Deprecated
public class MatchTaskBelief extends AtomicBooleanCondition<PremiseMatch> {


    @NotNull
    final String id;


    @NotNull
    public final Term[] pre;
    @NotNull
    public final Term[] code;
    public final Term term;


    public MatchTaskBelief(@NotNull TaskBeliefPair pattern, ListMultimap<Term, MatchConstraint> constraints) {

        //this.pattern = pattern;
        //compiled = new TermPattern(pattern, constraints);

        this.term = pattern;

        List<BooleanCondition<PremiseMatch>> pre = Global.newArrayList();
        List<BooleanCondition<PremiseMatch>> code = Global.newArrayList();

        compile(pattern, pre, code, constraints);

        this.pre = pre.toArray(new BooleanCondition[pre.size()]);
        this.code = code.toArray(new BooleanCondition[code.size()]);


        //Term beliefPattern = pattern.term(1);

        //if (Global.DEBUG) {
//            if (beliefPattern.structure() == 0) {

        // if nothing else in the rule involves this term
        // which will be a singular VAR_PATTERN variable
        // then allow null
//                if (beliefPattern.op() != Op.VAR_PATTERN)
//                    throw new RuntimeException("not what was expected");

//            }
        //}

        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + pattern
        );*/

        id = getClass().getSimpleName() + '[' + pattern.toStringCompact() + ']';

    }

    public void addPreConditions(@NotNull Collection<Term> l) {
        Collections.addAll(l, pre);
    }

    @Override
    public void addConditions(@NotNull List<Term> l) {
        Collections.addAll(l, code);
    }

    @Override
    public boolean booleanValueOf(PremiseMatch m) {
        throw new RuntimeException("this should not be called");
    }

    @NotNull
    @Override
    public final String toString() {
        return id;
    }


    private static void compile(@NotNull TaskBeliefPair pattern,
                                @NotNull List<BooleanCondition<PremiseMatch>> pre,
                                @NotNull List<BooleanCondition<PremiseMatch>> code,
                                ListMultimap<Term, MatchConstraint> constraints) {



        Term task = pattern.term(0);
        Term belief = pattern.term(1);

        BooleanCondition addToEndOfPreGuards = null;

        //check for any self similarity
        if (task.equals(belief)) {
            //add precondition constraint that task and belief must be equal.
            // assuming this succeeds, only need to test the task half
            addToEndOfPreGuards = new TaskBeliefEqualCondition();
            belief = null;
        }

        else if (task.isCompound() && belief.isCompound() && !task.isCommutative()
                && null==Ellipsis.firstEllipsis((Compound)task)
                && null==Ellipsis.firstEllipsis((Compound)belief)
                ) {
            int[] beliefInTask = ((Compound)task).isSubterm(belief);
            if (beliefInTask != null) {
                //add precondition for this constraint that is checked between the premise's terms
                //assuming this succeeds, only need to test the task half
                addToEndOfPreGuards = new ComponentCondition(0, beliefInTask, 1);
                belief = null;
            }
        }

        else if (belief.isCompound() && task.isCompound() && !belief.isCommutative()
                && null==Ellipsis.firstEllipsis((Compound)belief)
                && null==Ellipsis.firstEllipsis((Compound)task)
         ) {
            int[] taskInBelief = ((Compound)belief).isSubterm(task);
            if (taskInBelief != null) {
                //add precondition for this constraint that is checked between the premise's terms
                //assuming this succeeds, only need to test the belief half
                addToEndOfPreGuards = new ComponentCondition(1, taskInBelief, 0);
                task = null;
            }
        }


        //default case: exhaustively match both, with appropriate pruning guard preconditions
        compileTaskBelief(pre, code, task, belief, pattern, constraints);

        //put this one after the guards added in the compileTaskBelief like checking for op, subterm vector etc which will be less expensive
        if (addToEndOfPreGuards!=null)
            pre.add(addToEndOfPreGuards);
    }

    private static void compileTaskBelief(
            @NotNull List<BooleanCondition<PremiseMatch>> pre,
            List<BooleanCondition<PremiseMatch>> code, Term task, Term belief, TaskBeliefPair pattern, ListMultimap<Term, MatchConstraint> constraints) {

        boolean taskIsPatVar = task!=null && task.op() == Op.VAR_PATTERN;

        boolean belIsPatVar = belief!=null && belief.op() == Op.VAR_PATTERN;

        if (task!=null && !taskIsPatVar)
            pre.add(new SubTermOp(0, task.op()));
        if (belief!=null && !belIsPatVar)
            pre.add(new SubTermOp(1, belief.op()));

        if (task!=null && !taskIsPatVar)
            pre.add(new SubTermStructure(Op.VAR_PATTERN, 0, task.structure()));
        if (belief!=null && !belIsPatVar)
            pre.add(new SubTermStructure(Op.VAR_PATTERN, 1, belief.structure()));

        //        } else {
        //            if (x0.containsTermRecursively(x1)) {
        //                //pre.add(new TermContainsRecursively(x0, x1));
        //            }
        //        }

        //@Nullable ListMultimap<Term, MatchConstraint> c){


        @Nullable ImmutableMap<Term, MatchConstraint> cc = initConstraints(constraints);
        if (task!=null && belief!=null) {

            //match both
            //code.add(new MatchTerm.MatchTaskBeliefPair(pattern, initConstraints(constraints)));

            code.add(new MatchTerm.MatchOneSubterm(task, cc, 0, false));
            code.add(new MatchTerm.MatchOneSubterm(belief, cc, 1, true));

        } else if (belief!=null) {
            //match belief only
            code.add(new MatchTerm.MatchOneSubterm(belief, cc, 1, true));
        } else if (task!=null) {
            //match task only
            code.add(new MatchTerm.MatchOneSubterm(task, cc, 0, true));
        } else {
            throw new RuntimeException("invalid");
        }




    }


    @Nullable
    public static ImmutableMap<Term, MatchConstraint> initConstraints(@NotNull ListMultimap<Term, MatchConstraint> c) {
        if ((c == null) | (c.isEmpty())) return null;

        Map<Term, MatchConstraint> con = Global.newHashMap(c.size());
        c.asMap().forEach((t, cc) -> {
            switch (cc.size()) {
                case 0:
                    return;
                case 1:
                    con.put(t, cc.iterator().next());
                    break;
                default:
                    con.put(t, new AndConstraint(cc));
                    break;
            }
        });
        return immutable.ofAll(con);
    }

    /** matches the possibility that one half of the premise must be contained within the other.
     * this would in theory be more efficient than performing a complete match for the redundancies
     * which we can determine as a precondition of the particular task/belief pair
     * before even beginning the match. */
    static final class TaskBeliefEqualCondition extends AtomicBooleanCondition<PremiseMatch> {

        @Override
        public boolean booleanValueOf(PremiseMatch m) {
            Term[] x =  ((Compound)m.term.get()).terms();
            return x[0].equals(x[1]);
        }

        @Override
        public String toString() {
            return "taskbeliefEq";
        }
    }

    /** matches the possibility that one half of the premise must be contained within the other.
     * this would in theory be more efficient than performing a complete match for the redundancies
     * which we can determine as a precondition of the particular task/belief pair
     * before even beginning the match. */
    static final class ComponentCondition extends AtomicBooleanCondition<PremiseMatch> {

        private final String id;
        private final int container, contained;
        private final int[] path;

        public ComponentCondition(int container, int[] path, int contained) {
            this.id = "component(" + container + ",(" + Joiner.on(",").join(
                    Ints.asList(path)
            ) + ")," + contained + ")";

            this.container = container;
            this.contained = contained;
            this.path = path;
        }

        @Override
        public boolean booleanValueOf(PremiseMatch m) {
            Term[] x =  ((Compound)m.term.get()).terms();
            Term maybeContainer = x[this.container];
            if (!maybeContainer.isCompound())
                return false;
            Compound container = (Compound)maybeContainer;
            Term contained = x[this.contained];
            if (!container.impossibleSubterm(contained)) {
                Term whatsThere = container.subterm(path);
                if ((whatsThere != null) && contained.equals(whatsThere))
                    return true;
            }
            return false;
        }


        @Override
        public String toString() {
            return id;
        }
    }

//    private void compile(Term x, List<BooleanCondition<PremiseMatch>> code) {
//        //??
//    }

//    private void compileRisky(Term x, List<PreCondition> code) {
//
//
//        if (x instanceof TaskBeliefPair) {
//
//            compileTaskBeliefPair((TaskBeliefPair)x, code);
//
//        } else if (x instanceof Compound) {
//
//            //compileCompound((Compound)x, code);
//            code.add(new FindSubst.TermOpEquals(x.op())); //interference with (task,belief) pair term
//
//            /*
//            if (!Ellipsis.hasEllipsis((Compound)x)) {
//                code.add(new FindSubst.TermSizeEquals(x.size()));
//            }
//            else {
//                //TODO get a min bound for the term's size according to the ellipsis type
//            }
//            */
//            //code.add(new FindSubst.TermStructure(type, x.structure()));
//
//            //code.add(new FindSubst.TermVolumeMin(x.volume()-1));
//
//            int numEllipsis = Ellipsis.numEllipsis((Compound)x);
//            if (x.op().isImage()) {
//                if (numEllipsis == 0) {
//                    //TODO implement case for varargs
//                    code.add(new FindSubst.ImageIndexEquals(
//                            ((Compound) x).relation()
//                    ));
//                } else {
//                    //..
//                }
//            }
//
//
//
//            //if (!x.isCommutative() && Ellipsis.countEllipsisSubterms(x)==0) {
//                //ACCELERATED MATCH allows folding of common prefix matches between rules
//
//
//
//                //at this point we are certain that the compound itself should match
//                //so we proceed with comparing subterms
//
//                //TODO
//                //compileCompoundSubterms((Compound)x, code);
//            //}
//            //else {
//                //DEFAULT DYNAMIC MATCH (should work for anything)
//                code.add(new FindSubst.MatchTerm(x, null));
//            //}
//            //code.add(new FindSubst.MatchCompound((Compound)x));
//
//        } else {
//            //an atomic term, use the general entry dynamic match point 'matchTerm'
//
////            if ((x.op() == type) && (!(x instanceof Ellipsis) /* HACK */)) {
////                code.add(new FindSubst.MatchXVar((Variable)x));
////            }
////            else {
////                //something else
//            code.add(new FindSubst.MatchTerm(x));
////            }
//        }
//
//    }

//    /** compiles a match for the subterms of an ordered, non-commutative compound */
//    private void compileCompoundSubterms(Compound x, List<PreCondition> code) {
//
//        //TODO
//        //1. test equality. if equal, then skip past the remaining tests
//        //code.add(FindSubst.TermEquals);
//
//        code.add(FindSubst.Subterms);
//
//        for (int i = 0; i < x.size(); i++)
//            matchSubterm(x, i, code); //eventually this will be fully recursive and can compile not match
//
//        code.add(new FindSubst.ParentTerm(x)); //return to parent/child state
//
//    }

//    private void compileTaskBeliefPair(TaskBeliefPair x, List<PreCondition> code) {
//        //when derivation begins, frame's parent will be set to the TaskBeliefPair so that a Subterm code isnt necessary
//
//        int first, second;
//        if (x.term(1).op() == Op.VAR_PATTERN) {
//            //if the belief term is just a pattern,
//            //meaning it can match anything,
//            //then match this first because
//            //likely something in the task term will
//            //depend on it.
//            first = 1;
//            second = 0;
//
//        } else {
//            first = 0;
//            second = 1;
//        }
//
//
//        Term x0 = x.term(first);
//        Term x1 = x.term(second);
//
//        //add early preconditions for compounds
//        if (x0.op()!=Op.VAR_PATTERN) {
//            code.add(new FindSubst.SubTermOp(first, x0.op()));
//            code.add(new FindSubst.SubTermStructure(type, first, x0.structure()));
//        }
//        if (x1.op()!=Op.VAR_PATTERN) {
//            code.add(new FindSubst.SubTermOp(second, x1.op()));
//            code.add(new FindSubst.SubTermStructure(type, second, x1.structure()));
//        }
//
////        compileSubterm(x, first, code);
////        compileSubterm(x, second, code);
//        compileSubterm(x, 0, code);
//        compileSubterm(x, 1, code);
//    }

//    private void compileSubterm(Compound x, int i, List<PreCondition> code) {
//        Term xi = x.term(i);
//        code.add(new FindSubst.Subterm(i));
//        compile(xi, code);
//    }
//    private void matchSubterm(Compound x, int i, List<PreCondition> code) {
//        code.add(new FindSubst.Subterm(i));
//        code.add(new FindSubst.MatchTerm(x.term(i)));
//    }

//    private void compileCompound(Compound<?> x, List<PreCondition> code) {
//
//        int s = x.size();
//
//        /** whether any subterms are matchable variables */
//        final boolean constant = !Variable.hasPatternVariable(x);
//        final boolean vararg = constant ? Ellipsis.hasEllipsis(x) : false;
//
//        if (constant) { /*(type == Op.VAR_PATTERN && (*/
//
//            /** allow to compile the structure of the compound
//             *  match statically, including any optimization
//             *  possibilties that foreknowledge of the pattern
//             *  like we have here may provide
//             */
//            //compileConstantCompound(x, code);
//        } else {
//
//        }
//
//
//        code.add(new FindSubst.TermOpEquals(x.op())); //interference with (task,belief) pair term
//
//        //TODO varargs with greaterEqualSize etc
//        //code.add(new FindSubst.TermSizeEquals(c.size()));
//
//        //boolean permute = x.isCommutative() && (s > 1);
//
//        switch (s) {
//            case 0:
//                //nothing to match
//                break;
//
////            case 1:
////                code.add(new FindSubst.MatchTheSubterm(x.term(0)));
////                break;
//
//            default:
//
//                /*if (x instanceof Image) {
//                    code.add(new FindSubst.MatchImageIndex(((Image)x).relationIndex)); //TODO varargs with greaterEqualSize etc
//                }*/
//
//                //TODO this may only be safe if no var-args
//                //code.add(new FindSubst.TermVolumeMin(c.volume()-1));
//
//
//                code.add(new FindSubst.MatchCompound(x));
//
//
////                if (permute) {
////                    code.add(new FindSubst.MatchPermute(c));
////                }
////                else {
////                    compileNonCommutative(code, c);
////                }
//
//            break;
//        }
//    }


    /*private void compileConstantNonCommutiveCompound(Compound<?> x, List<PreCondition> code) {
        //TODO
    }*/


//

//    private void compileNonCommutative(List<PreCondition> code, Compound<?> c) {
//
//        final int s = c.size();
//        TreeSet<SubtermPosition> ss = new TreeSet();
//
//        for (int i = 0; i < s; i++) {
//            Term x = c.term(i);
//            ss.add(new SubtermPosition(x, i, subtermPrioritizer));
//        }
//
//        code.add( FindSubst.Subterms );
//
//        ss.forEach(sp -> { //iterate sorted
//            Term x = sp.term;
//            int i = sp.position;
//
//            compile2(x, code, i);
//            //compile(type, x, code);
//        });
//
//        code.add( FindSubst.Superterm );
//    }

//    private void compile2(Term x, List<PreCondition> code, int i) {
//        //TODO this is a halfway there.
//        //in order for this to work, parent terms need to be stored in a stack or something to return to, otherwise they get a nulll and it crashes:
//
////            code.add(new SelectSubterm(i));
////            compile(x, code);
////
//         if (x instanceof Compound) {
////                //compileCompound((Compound)x, code);
////            /*}
////            else {
//             code.add(new FindSubst.MatchSubterm(x, i));
//         }
//         else {
//             //HACK this should be able to handle atomic subterms without a stack
//             code.add(new FindSubst.SelectSubterm(i));
//             compile(x, code);
//         }
//
//    }

//    final static class SubtermPosition implements Comparable<SubtermPosition> {
//
//        public final int score;
//        public final Term term; //the subterm
//        public final int position; //where it is located
//
//        public SubtermPosition(Term term, int pos, ToIntFunction<Term> scorer) {
//            this.term = term;
//            this.position = pos;
//            this.score = scorer.applyAsInt(term);
//        }
//
//        @Override
//        public int compareTo(SubtermPosition o) {
//            if (this == o) return 0;
//            int p = Integer.compare(o.score, score); //lower first
//            if (p!=0) return p;
//            return Integer.compare(position, o.position);
//        }
//
//        @Override
//        public String toString() {
//            return term + " x " + score + " (" + position + ')';
//        }
//    }
//    /** heuristic for ordering comparison of subterms; lower is first */
//    private ToIntFunction<Term> subtermPrioritizer = (t) -> {
//
//        if (t.op() == type) {
//            return 0;
//        }
//        else if (t instanceof Compound) {
//            if (!t.isCommutative()) {
//                return 1 + (1 * t.volume());
//            } else {
//                return 1 + (2 * t.volume());
//            }
//        }
//        else {
//            return 1; //atomic
//        }
//    };

//    @NotNull
//    @Override
//    public String toString() {
//        return "TermPattern{" + Arrays.toString(code) + '}';
//    }


}
//    @Override public final boolean test(final RuleMatch m) {
//
//        boolean sameAsPrevPattern =
//                (m.prevRule!=null) && (m.prevRule.pattern.equals(m.rule.pattern);
//
//        if (!m.prevXY.isEmpty()) {
//            //re-use previous rule's result
//            m.xy.putAll(m.prevXY);
//            m.yx.putAll(m.prevYX);
//            return true;
//        }
//        else {
//            boolean b = _test(m);
//            if (b) {
//
//            }
//            else {
//                //put a placeholder to signal that this does not succeed
//            }
//
//        }
//
//
//        if
//                this.prevXY.putAll(xy);
//                this.prevYX.putAll(yx);
//            }
//            else {
//                this.prevXY.clear(); this.prevYX.clear();
//            }
//        }
//    }

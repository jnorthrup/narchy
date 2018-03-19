package nars.derive.rule;

import jcog.TODO;
import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.derive.*;
import nars.derive.constraint.*;
import nars.derive.op.*;
import nars.index.term.PatternIndex;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.pred.AndCondition;
import nars.term.pred.PrediTerm;
import nars.term.transform.TermTransform;
import nars.truth.func.BeliefFunction;
import nars.truth.func.GoalFunction;
import nars.truth.func.TruthOperator;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.util.Collections.addAll;
import static nars.$.*;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRule extends ProxyTerm {

    public static final Atomic Task = Atomic.the("task");
    public static final Atomic Belief = Atomic.the("belief");
    private static final Term TaskAny = $.func("task", Atomic.the("any"));
    private static final Term QUESTION_PUNCTUATION = $.inh(Atomic.the("Question"), Atomic.the("Punctuation"));

    /**
     * conditions which can be tested before unification
     */
    public PrediTerm<ProtoDerivation>[] PRE;

    /**
     * consequences applied after unification
     */
    public PostCondition[] POST;


    public String source;


    final SortedSet<MatchConstraint> constraints = new TreeSet(PrediTerm.sortByCost);
    final List<PrediTerm<ProtoDerivation>> pre = new FasterList(8);
    final List<PrediTerm<Derivation>> post = new FasterList(8);

    public PremiseRule(Subterms premiseAndResult, String src) {
        super(Op.PROD.the(premiseAndResult));
        this.source = src;
    }


    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(@NotNull Term x) {
        Terms.printRecursive(System.out, x);
    }

    @NotNull
    private Compound match() {
        return (Compound) term().sub(0);
    }

    @NotNull
    public Compound conclusion() {
        return (Compound) term().sub(1);
    }



    /**
     * compiles the conditions which are necessary to activate this rule
     */
    public Pair<Set<PrediTerm<ProtoDerivation>>, PrediTerm<Derivation>> build(PostCondition post) {

        byte puncOverride = post.puncOverride;

        TruthOperator belief = BeliefFunction.get(post.beliefTruth);
        if ((post.beliefTruth != null) && !post.beliefTruth.equals(TruthOperator.NONE) && (belief == null)) {
            throw new RuntimeException("unknown BeliefFunction: " + post.beliefTruth);
        }
        TruthOperator goal = GoalFunction.get(post.goalTruth);
        if ((post.goalTruth != null) && !post.goalTruth.equals(TruthOperator.NONE) && (goal == null)) {
            throw new RuntimeException("unknown GoalFunction: " + post.goalTruth);
        }

        //if (puncOverride==0) {
            if (belief!=null && goal!=null) {
                if (!belief.single() && !goal.single()) {
                    pre.add(TaskPolarity.belief);
                } else if (belief.single() ^ goal.single()){
                    throw new TODO();
                }
            } else if (belief!=null && !belief.single()) {
                pre.add(TaskPolarity.belief);
            } else if (goal!=null && !goal.single()) {
                pre.add(TaskPolarity.belief);
            }

            //TODO add more specific conditions that also work
        //}



        String beliefLabel = belief != null ? belief.toString() : "_";
        String goalLabel = goal != null ? goal.toString() : "_";

        FasterList<Term> args = new FasterList();
        args.add($.the(beliefLabel));
        args.add($.the(goalLabel));
        if (puncOverride != 0)
            args.add($.quote(((char) puncOverride)));

        Compound ii = (Compound) $.func("truth", args.toArrayRecycled(Term[]::new));


        Solve truth = (puncOverride == 0) ?
                new Solve.SolvePuncFromTask(ii, belief, goal) :
                new Solve.SolvePuncOverride(ii, puncOverride, belief, goal);

        //PREFIX
        Set<PrediTerm<ProtoDerivation>> precon = newHashSet(4); //for ensuring uniqueness / no duplicates

        addAll(precon, PRE);

        precon.addAll(this.pre);


        ////-------------------
        //below here are predicates which affect the derivation


        //SUFFIX (order already determined for matching)
        int n = 1 + this.constraints.size() + this.post.size();

        PrediTerm[] suff = new PrediTerm[n];
        int k = 0;
        suff[k++] = truth;
        for (PrediTerm p : this.constraints) {
            suff[k++] = p;
        }
        for (PrediTerm p : this.post) {
            suff[k++] = p;
        }

        return pair(precon, (PrediTerm<Derivation>) AndCondition.the(suff));
    }


    public PremiseRule withSource(String source) {
        this.source = source;
        return this;
    }


    /**
     * source string that generated this rule (for debugging)
     */
    public String getSource() {
        return source;
    }

    /**
     * the task-term pattern
     */
    public final Term getTask() {
        return (match().sub(0));
    }

    /**
     * the belief-term pattern
     */
    public final Term getBelief() {
        return (match().sub(1));
    }

    private Term getConclusionTermPattern() {
        return conclusion().sub(0);
    }


}






package nars.link;

import jcog.bag.Bag;
import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.control.BatchActivation;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.sub.Subterms;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Collections.emptyList;
import static nars.Op.INT;

public enum TermLinks {
    ;


    public static List<Termed> templates(Term term) {

        if (term.subs() > 0) {

            ArrayHashSet<Termed> tc =
                    //new UnifiedSet<>(id.volume() /* estimate */);
                    new ArrayHashSet<>(term.volume());

            if (Param.DEBUG_EXTRA) {
                if (!term.equals(term.conceptual())) {
                    throw new RuntimeException("templates only should be generated for rooted terms:\n\t" + term + "\n\t" + term.conceptual());
                }
            }

            TermLinks.templates(term, tc, 0, layers(term));

            int tcs = tc.size();
            if (tcs > 0)
                return ((FasterList)tc.list).compact(); //store as list for compactness and fast iteration
            else
                return emptyList();

        } else {

            //return List.of(term);
            return emptyList(); //atomic self term-link disabled
        }
    }

    /**
     * recurses
     */
    static void templates(Term _x, Set<Termed> tc, int depth, int maxDepth) {

        Term x = _x;

//        switch (o) {
//            case VAR_QUERY:
//            case VAR_DEP:
//            case VAR_INDEP:
//                //return; //NO
//                break; //YES
//
//        }

        if ((depth > 0 || selfTermLink(x)) && !(tc.add(x)))
            return; //already added

        Op o = x.op();
        if ((++depth >= maxDepth) || !o.conceptualizable)
            return;

        Subterms bb = x.subterms();
        int bs = bb.subs();
        if (bs > 0) {
            int nextDepth = depth;
            bb.forEach(s -> templates(s.unneg(), tc, nextDepth, maxDepth));
        }
    }


    /** whether to allow formation of the premise */
    public static boolean premise(Term task, Term term) {
        return true;
    }

    /** determines ability to structural transform, so those terms which have no structural transforms should not link to themselves */
    static boolean selfTermLink(Term b) {
        return true;
    }

    /**
     * includes the host as layer 0, so if this returns 1 it will only include the host
     */
    static int layers(Term x) {
        switch (x.op()) {


            case SETe:
            case SETi:
                return 2;

            case PROD:
                return 2;

            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
                return 2;

            case CONJ:
                return 3;

            case SIM:
                return 2;

            case INH:
                return 4;

            case IMPL:
//                if (host.hasAny(Op.CONJ))
                return 4;
//                else
//                    return 3;

            default:
                throw new UnsupportedOperationException("unhandled operator type: " + x.op());

        }
    }

    @Nullable
    public static Concept linkTemplate(Term srcTerm, Bag srcTermLinks, Termed target, float priForward, float priReverse, BatchActivation a, NAR nar, MutableFloat refund) {

        Term targetTerm = null;
        boolean reverseLinked = false;
        Concept c = null;
        if (!target.equals(srcTerm)) {
            c = nar.conceptualize(target);
            if (c != null) {
                c.termlinks().put(
                        new PLink<>(srcTerm, priReverse), refund
                );
                a.put(c, priForward + priReverse);
                reverseLinked = true;
                targetTerm = c.term();
            }
        }

        if (targetTerm == null)
            targetTerm = target.term();

        if (!reverseLinked)
            refund.add(priReverse);

        srcTermLinks.put(
                new PLink<>(targetTerm, priForward), refund
        );

        return c;
    }

//    @Nullable
//    public static List<Termed> templates(Concept id, NAR nar) {
//
//        Collection<Termed> localTemplates = id.templates();
//        int n = localTemplates.size();
//        if (n <= 0)
//            return null;
//
//        Term thisTerm = id.term();
//
//        List<Termed> localSubConcepts =
//                //new HashSet<>(); //temporary for this function call only, so as not to retain refs to Concepts
//                //new UnifiedSet(); //allows concurrent read
//                $.newArrayList(n); //maybe contain duplicates but its ok, this is fast to construct
//
//        //Random rng = nar.random();
//        //float balance = Param.TERMLINK_BALANCE;
//
//        float spent = 0;
//        for (Termed localSub : localTemplates) {
//
//            //localSub = mutateTermlink(localSub.term(), rng); //for special Termed instances, ex: RotatedInt etc
////                if (localSub instanceof Bool)
////                    continue; //unlucky mutation
//
//
//            Termed target = localSub.term(); //if mutated then localSubTerm would change so do it here
//
//            float d;
//
//            if (target.op().conceptualizable && !target.equals(thisTerm)) {
//
//                Concept targetConcept = nar.conceptualize(localSub);
//                if (targetConcept != null) {
//                    target = (targetConcept);
//
//                }
//            }
//
//            localSubConcepts.add(target);
//        }
//
//        return !localSubConcepts.isEmpty() ? localSubConcepts : null;
//
//    }

    /**
     * preprocess termlink
     */
    private static Termed mutateTermlink(Term t, Random rng) {

        if (Param.MUTATE_INT_CONTAINING_TERMS_RATE > 0) {
            if (t.hasAny(INT)) {
                Subterms ts = t.subterms();
                if (ts.OR(Int.class::isInstance) && rng.nextFloat() <= Param.MUTATE_INT_CONTAINING_TERMS_RATE) {

                    Term[] xx = ts.arrayClone();
                    boolean changed = false;
                    for (int i = 0; i < xx.length; i++) {
                        Term y = xx[i];
                        if (y instanceof Int && y.op()==INT) {
                            int shift =
                                    rng.nextInt(3) - 1;
                            //nar.random().nextInt(5) - 2;
                            if (shift != 0) {
                                int yy = ((Int) y).id;
                                int j =
                                        Math.max(0 /* avoid negs for now */, yy + shift);
                                if (yy != j) {
                                    xx[i] = Int.the(j);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (changed)
                        return t.op().the(t.dt(), xx);

                }
            }

        }

        return t;
    }

//    @NotNull
//    public static Concept[] templateConcepts(List<Termed> templates) {
//        if (templates.isEmpty())
//            return Concept.EmptyArray;
//
//        FasterList<Concept> templateConcepts = new FasterList(0, new Concept[templates.size()]);
//        for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
//            Termed x = templates.get(i);
//            if (x instanceof Concept)
//                templateConcepts.add((Concept) x);
//        }
//        return templateConcepts.toArrayRecycled(Concept[]::new);
//    }

    /**
     * send some activation, returns the cost
     */
    public static float linkTemplates(Concept src, List<Termed> templates, List<Concept> conceptualizedTemplates, float totalBudget, float momentum, NAR nar, BatchActivation ba) {

        int n = templates.size();
        if (n == 0)
            return 0;

        float freed = 1f - momentum;
        int toFire = (int) Math.ceil(n * freed);
        float budgeted = totalBudget * freed;

        float budgetedToEach = budgeted / toFire;
        if (budgetedToEach < Pri.EPSILON)
            return 0;

        MutableFloat refund = new MutableFloat(0);

        int nextTarget = nar.random().nextInt(n);
        Term srcTerm = src.term();
        Bag<Term, PriReference<Term>> srcTermLinks = src.termlinks();
        float balance = nar.termlinkBalance.floatValue();
        for (int i = 0; i < toFire; i++) {

            Termed t = templates.get(nextTarget++);
            if (nextTarget == n) nextTarget = 0; //wrap around

            Concept c = linkTemplate(srcTerm, srcTermLinks, t,
                    budgetedToEach * balance,
                    budgetedToEach * (1f - balance),
                    ba, nar, refund);
            if (c!=null)
                conceptualizedTemplates.add(c);
        }

        float r = refund.floatValue();
        float cost = budgeted - r;
        return cost;
    }

}

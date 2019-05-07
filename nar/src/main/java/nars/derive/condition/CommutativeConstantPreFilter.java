package nars.derive.condition;

import nars.$;
import nars.derive.model.PreDerivation;
import nars.derive.rule.PremiseRule;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.var.ellipsis.Ellipsislike;
import nars.unify.Unify;

import java.util.Collection;

import static nars.derive.model.Derivation.Belief;
import static nars.derive.model.Derivation.Task;

public class CommutativeConstantPreFilter extends AbstractPred<PreDerivation> {

    private final byte[] ellipsisPath, contentPath;
    private final boolean ellipsisInTaskOrBelief;

    private static final Atom id = Atomic.atom("ellipsisCommutativeConstant");

    /**
     * from = non-ellipsis target to check if present as a subterm of what appears at to
     */
    public CommutativeConstantPreFilter(byte[] ellipsisPath, byte[] contentPath, boolean ellipsisInTaskOrBelief /* direction */) {
        super($.func(id, PremiseRule.pathTerm(ellipsisPath), PremiseRule.pathTerm(contentPath), ellipsisInTaskOrBelief ? Task : Belief));
        this.ellipsisInTaskOrBelief = ellipsisInTaskOrBelief;
        this.ellipsisPath = ellipsisPath;
        this.contentPath = contentPath;
    }

    public static void tryFilter(boolean commInTaskOrBelief, Term taskPattern, Term beliefPattern, Collection<PREDICATE<? extends Unify>> pre) {

        Term commutiveContainer = (commInTaskOrBelief) ? taskPattern : beliefPattern;

        if (commutiveContainer instanceof Compound /* && concPattern.hasAny(Op.commutative)*/) {
            //target.pathsTo((Term t)->true, (Term t)->true, (ByteList ss, Term x)-> {
            commutiveContainer.recurseTerms(t -> true, (Term x) -> {

                if (x!=commutiveContainer && x.op().commutative) {
                //if (x instanceof PatternIndex.PremisePatternCompound.PremisePatternCompoundWithEllipsisCommutive) {
                    byte[] commPath = Terms.pathConstant(commutiveContainer, x);
                    if (commPath != null) {

                        x.subterms().forEach(s -> {
                            if (s instanceof Ellipsislike) return; //skip ellipsis terms

                            //s is constant:
                            Term contentHolder = commInTaskOrBelief ? beliefPattern : taskPattern;
                            byte[] contentPath = Terms.pathConstant(contentHolder, s); //try the belief
                            if (contentPath != null)
                                pre.add(new CommutativeConstantPreFilter(
                                        commPath, contentPath, commInTaskOrBelief)
                                );
                        });
                    }
                }
                return true;
            }, null);
        }
    }


    @Override
    public boolean test(PreDerivation d) {

        Term contentHolder = ellipsisInTaskOrBelief ? d.beliefTerm : d.taskTerm;
        Term content = contentHolder.subPath(this.contentPath);
        if (content == null)
            return false;

        Term containerHolder = ellipsisInTaskOrBelief ?  d.taskTerm : d.beliefTerm;
        Term ellipsisContainer = containerHolder.subPath(this.ellipsisPath);

        return ellipsisContainer!=null && ellipsisContainer.contains(content);
    }

    @Override
    public float cost() {
        return 0.3f;
    }
}

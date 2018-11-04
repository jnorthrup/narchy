package nars.derive.filter;

import nars.$;
import nars.derive.Derivation;
import nars.derive.premise.PatternIndex;
import nars.derive.premise.PremiseRuleSource;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.match.Ellipsislike;

import java.util.Collection;

import static nars.derive.Derivation.Belief;
import static nars.derive.Derivation.Task;

public class EllipsisCommutativeConstantPreFilter extends AbstractPred<Derivation> {

    private final byte[] ellipsisPath, contentPath;
    private final boolean ellipsisInTaskOrBelief;

    private static final Atomic id = Atomic.the("ellipsisCommutativeConstant");

    /**
     * from = non-ellipsis term to check if present as a subterm of what appears at to
     */
    public EllipsisCommutativeConstantPreFilter(byte[] ellipsisPath, byte[] contentPath, boolean ellipsisInTaskOrBelief /* direction */) {
        super($.func(id, PremiseRuleSource.pp(ellipsisPath), PremiseRuleSource.pp(contentPath), ellipsisInTaskOrBelief ? Task : Belief));
        this.ellipsisInTaskOrBelief = ellipsisInTaskOrBelief;
        this.ellipsisPath = ellipsisPath;
        this.contentPath = contentPath;
    }

    public static void tryFilter(boolean ellipsisInTaskOrBelief, Term taskPattern, Term beliefPattern, Collection<PREDICATE> pre) {

        Term ellipsisContainer = (ellipsisInTaskOrBelief) ? taskPattern : beliefPattern;

        if (ellipsisContainer instanceof Compound /* && concPattern.hasAny(Op.commutative)*/) {
            //target.pathsTo((Term t)->true, (Term t)->true, (ByteList ss, Term x)-> {
            ellipsisContainer.recurseTerms(t -> true, (Term x) -> {

                if (x instanceof PatternIndex.PremisePatternCompound.PremisePatternCompoundWithEllipsisCommutive) {
                    byte[] ellipsisPath = Terms.constantPath(ellipsisContainer, x);
                    if (ellipsisPath != null) {

                        x.subterms().forEach(s -> {
                            if (s instanceof Ellipsislike) return; //skip ellipsis terms

                            //s is constant:
                            Term contentHolder = ellipsisInTaskOrBelief ? beliefPattern : taskPattern;
                            byte[] contentPath = Terms.constantPath(contentHolder, s); //try the belief
                            if (contentPath != null)
                                pre.add(new EllipsisCommutativeConstantPreFilter(
                                        ellipsisPath, contentPath, ellipsisInTaskOrBelief)
                                );
                        });
                    }
                }
                return true;
            }, null);
        }
    }


    @Override
    public boolean test(Derivation d) {

        Term contentHolder = ellipsisInTaskOrBelief ? d.beliefTerm : d.taskTerm;
        Term content = contentHolder.subPath(this.contentPath);
        if (content == null)
            return false;

        Term containerHolder = ellipsisInTaskOrBelief ?  d.taskTerm : d.beliefTerm;
        Term ellipsisContainer = containerHolder.subPath(this.ellipsisPath);

        boolean ok = ellipsisContainer.contains(content);
        return ok;
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}

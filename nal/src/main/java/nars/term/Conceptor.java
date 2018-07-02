package nars.term;

import nars.$;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.link.TermlinkTemplates;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

import static nars.Op.PROD;

/** dynamic concept builder.
 *
 * conceptor(base[, (args,...)])
 * */
abstract public class Conceptor extends NodeConcept implements PermanentConcept, BiFunction<Term, Subterms, Concept>, Atomic {

    protected Conceptor(@NotNull String atom) {
        this(Functor.fName(atom));
    }

    protected Conceptor(@NotNull Atom atom) {
        super(atom, ConceptBuilder.Null);
    }

    /** names the concept with this conceptor and params */
    public Term id(Term base, Subterms param) {
        return $.func((Atomic)this.term, base, PROD.the(param.arrayShared()));
    }

    @Override
    protected TermlinkTemplates buildTemplates(Term term) {
        return TermlinkTemplates.EMPTY;
    }

    @Override
    public final Term term() {
        return this;
    }


    @Override
    public final int opX() {
        return term.opX();
    }

    @Override
    public final byte[] bytes() {
        return ((Atomic) term).bytes();
    }

}

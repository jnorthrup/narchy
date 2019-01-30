//package nars.target;
//
//import nars.$;
//import nars.Op;
//import nars.concept.Concept;
//import nars.concept.NodeConcept;
//import nars.concept.PermanentConcept;
//import nars.concept.util.ConceptBuilder;
//import nars.subterm.Subterms;
//import nars.target.atom.Atom;
//import nars.target.atom.Atomic;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.function.BiFunction;
//
///** dynamic concept builder.
// *
// * conceptor(base[, (args,...)])
// * */
//abstract public class Conceptor extends NodeConcept implements PermanentConcept, BiFunction<Term, Subterms, Concept>, Atomic {
//
//    protected Conceptor(@NotNull String atom) {
//        this(Functor.fName(atom));
//    }
//
//    protected Conceptor(@NotNull Atom atom) {
//        super(atom, ConceptBuilder.NullConceptBuilder);
//    }
//
//    /** names the concept with this conceptor and params */
//    public Term id(Subterms args) {
//        return $.func((Atomic)this.target, args.arrayShared());
//    }
//    public Term id(Term... args) {
//        return id(Op.terms.subterms(args));
//    }
//
//
//    @Override
//    public final Term target() {
//        return this;
//    }
//
//
//    @Override
//    public final int opX() {
//        return target.opX();
//    }
//
//    @Override
//    public final byte[] bytes() {
//        return ((Atomic) target).bytes();
//    }
//
//}

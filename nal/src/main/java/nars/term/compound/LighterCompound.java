package nars.term.compound;

import jcog.TODO;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Term;

import java.util.function.Predicate;

/** mutable, use with caution; hashCode is dynamically computed */
public class LighterCompound extends TermList implements AbstractLightCompound {
    private final byte op;

    private LighterCompound(byte op) {
        super();
        this.op = op;
    }


    public LighterCompound(Op op) {
        this(op.id);
    }

    public LighterCompound(Op op, Term... subs) {
        super(subs);
        this.op = op.id;
    }
    @Override
    public boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return !impossibleSubTerm(t) && inSubtermsOf.test(this)
                && super.containsRecursively(t, root, inSubtermsOf);
    }
    @Override
    public boolean isNormalized() {
        return super.isNormalized();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Compound) {
            Compound c = (Compound)obj;
            if (c.op() == op() && c.dt()==dt()) {
                int s = c.subs();
                if (subs()==s) {
                    for (int i = 0; i < s; i++) {
                        if (!sub(i).equals(c.sub(i)))
                            return false;
                    }
                    return true;
                }
            }
        }
        return false;

    }

    @Override
    public int volume() {
        return super.volume();
    }

    @Override
    public int complexity() {
        return super.complexity();
    }

    @Override
    public Term[] arrayShared() {
        return arrayKeep();
    }

    @Override
    public final int hashCode() {
        return hashWith(op());
    }


    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public final Op op() {
        return Op.ops[op];
    }


    @Override
    public Subterms subterms() {
        throw new TODO(); //must use a separate view instance for correctness, ex: distinguish between structure of the compound and the structure of the subterms
    }

    @Override
    public boolean isTemporal() {
        return super.isTemporal();
    }

    @Override
    public int hashCodeSubterms() {
        return super.hashCode();
    }
}

package nars.term.compound;

import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.util.TermList;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

/** mutable, use with caution; hashCode is dynamically computed */
public class LighterCompound extends TermList implements AbstractLightCompound {
    protected final byte op;

    public LighterCompound(byte op) {
        super();
        this.op = op;
    }

    @Override
    public int structure() {
        return super.structure() | op().bit;
    }

    public LighterCompound(Op op) {
        this(op.id);
    }

    public LighterCompound(Op op, Term... subs) {
        super(subs);
        this.op = op.id;
    }

    @Override
    public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return super.intifyShallow(reduce, v);
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
        return super.volume()+1;
    }

    @Override
    public int complexity() {
        return super.complexity()+1;
    }

    @Override
    public int varPattern() {
        return super.varPattern();
    }
    @Override
    public int varQuery() {
        return super.varQuery();
    }
    @Override
    public int varDep() {
        return super.varDep();
    }
    @Override
    public int varIndep() {
        return super.varIndep();
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
        return this;
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

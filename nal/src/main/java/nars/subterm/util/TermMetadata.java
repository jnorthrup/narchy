package nars.subterm.util;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import jcog.TODO;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import nars.term.anon.Intrin;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.term.util.TermException;
import nars.term.var.NormalizedVariable;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * cached values for target/subterm metadata
 */
public abstract class TermMetadata implements Termlike {

    /**
     * bitvector of subterm types, indexed by Op.id and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    public final int structure;
    /**
     * normal high-entropy "content" hash of the terms
     */
    public final int hash;
    /**
     * stored as volume+1 as if this termvector were already wrapped in its compound
     */
    private final short volume;
    /**
     * stored as complexity+1 as if this termvector were already wrapped in its compound
     */
    private final short complexity;
    private final byte varPattern;
    private final byte varDep;
    private final byte varQuery;
    private final byte varIndep;

    protected TermMetadata(Term[] terms) {
        this(new SubtermMetadataCollector(terms));
    }

    protected TermMetadata(SubtermMetadataCollector s) {

        if (s.varPattern > Byte.MAX_VALUE || s.varQuery > Byte.MAX_VALUE || s.varDep > Byte.MAX_VALUE || s.varIndep > Byte.MAX_VALUE)
            throw new TermException("variable overflow");

        int varTot =
                (this.varPattern = (byte) s.varPattern) +
                (this.varQuery = (byte) s.varQuery) +
                (this.varDep = (byte) s.varDep) +
                (this.varIndep = (byte) s.varIndep);

        this.complexity = (short) ((this.volume = s.vol) - varTot);

        this.hash = s.hash;
        this.structure = s.structure;

    }


    /** not a conclusive test but meant to catch most cases where the target is already normalized */
    public static boolean normalized(Subterms x) {

        //the product containing an image itself may be normalized but superterms containing it are not automatically considered normal
        if (Image.imageNormalizable(x))
            return false;

        int vars = x.vars();
        if (vars==0)
            return true;

        //depth first traversal, determine if variables encountered are monotonically increasing

        ByteArrayList types = new ByteArrayList(vars);
        return x.recurseTermsOrdered(Term::hasVars, v -> {
            if (!(v instanceof Variable))
                return true;

            if (v instanceof NormalizedVariable) {
                NormalizedVariable nv = (NormalizedVariable) v;
                byte varID = nv.id();
                int nTypes = types.size();
                if (varID <= nTypes) {
                    return types.getByte(varID-1) == nv.anonType();
                } else if (varID == 1 + nTypes) {
                    types.add(nv.anonType());
                    return true;
                }
            }

            return false;

        }, null);
    }

    /**
     * for AnonVector
     */
    protected static boolean normalized(short[] subterms) {
        /* checks for monotonically increasing variable numbers starting from 1,
         which will indicate that the subterms is normalized
         */

        int minID = 0;
        int typeToMatch = -1;
        for (short x: subterms) {
            boolean neg = x < 0;
            if (neg) {
                x = (short) -x;
            }
            int varID = Intrin.isVariable(x, -1);
            if (varID == -1) {
                /*..*/
            } else if (varID == minID) {
                //same order, ok
                int type = Intrin.group(x);
                if (typeToMatch == -1)
                    typeToMatch = type;
                else if (typeToMatch!=type)
                    return false; //same id different type, needs normalized
            } else if (varID == minID + 1) {
                //increase the order, ok, set new type
                typeToMatch = Intrin.group(x);
                minID++;
            } else if (varID > minID + 1) {
                return false; //cant be sure
            }
        }
        return true;
    }

    public final int vars() {
        return varDep + varIndep + varQuery + varPattern;
    }

    public final int varQuery() {
        return varQuery;
    }

    public final int varDep() {
        return varDep;
    }

    public final int varIndep() {
        return varIndep;
    }

    public final int varPattern() {
        return varPattern;
    }

    @Override
    public final boolean hasVarQuery() {
        return varQuery > 0;
    }

    @Override
    public final boolean hasVarIndep() {
        return varIndep > 0;
    }

    @Override
    public final boolean hasVarDep() {
        return varDep > 0;
    }

    @Override
    public boolean hasVarPattern() {
        return varPattern > 0;
    }

    public final int structure() {
        return structure;
    }

    public final int volume() {
        return volume;
    }

    public final int complexity() {
        return complexity;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract Term sub(int i);

    @Override
    public Term subSafe(int i) {
        return sub(i, Bool.Null);
    }

    @Override
    public Term sub(int i, Term ifOutOfBounds) {
        return i >= subs() ? ifOutOfBounds : sub(i);
    }

    @Override
    public abstract int subs();

    @Override
    public int height() {
        return subs() == 0 ? 1 : 1 + max(Term::height);
    }

    @Override
    public int sum(ToIntFunction<Term> value) {
//        int x = 0;
//        int s = subs();
//        for (int i = 0; i < s; i++)
//            x += value.applyAsInt(sub(i));
//
//        return x;
        return intifyShallow(0, (x, t) -> x + value.applyAsInt(t));
    }

    @Override
    public int max(ToIntFunction<Term> value) {
        return intifyShallow(Integer.MIN_VALUE, (x, t) -> Math.max(value.applyAsInt(t), x));
    }

    @Override
    public int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = reduce.intValueOf(v, sub(i));
        return v;
    }

    @Override
    public int intifyRecurse(int _v, IntObjectToIntFunction<Term> reduce) {
        return intifyShallow(_v, (v, s) -> s.intifyRecurse(v, reduce));
    }

    @Override
    public abstract boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    @Override
    public abstract boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent);

    @Override
    public float voluplexity() {
        return (complexity() + volume()) / 2f;
    }

    @Override
    @Deprecated
    public abstract boolean contains(Term t);

    @Override
    @Deprecated
    public abstract boolean containsInstance(Term t);

    @Override
    public abstract boolean hasXternal();

    @Override
    public abstract boolean impossibleSubTerm(Termlike target);

    @Override
    public boolean hasAll(int structuralVector) {
        return Op.has(structure(), structuralVector, true);
    }

    @Override
    public boolean hasAny(int structuralVector) {
        return Op.has(structure(), structuralVector, false);
    }

    @Override
    public /* final */ boolean hasAny(/*@NotNull*/ Op op) {
        return hasAny(op.bit);
    }

    @Override
    public /* final */ boolean hasAllAny(/*@NotNull*/ int all, int any) {
        int s = structure();
        return Op.has(s, all, true) && Op.has(s, any, false);
    }

    @Override
    public abstract boolean impossibleSubStructure(int structure);

    @Override
    public boolean impossibleSubVolume(int otherTermVolume) {
        return otherTermVolume > volume() - subs();
    }

    @Override
    public boolean hasVars() {
        return hasAny(VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit | VAR_PATTERN.bit);
    }

    @Override
    public int structureSurface() {
        return intifyShallow(0, (s, x) -> s | x.opBit());
    }

    @Override
    public boolean these() {
        throw new TODO();
    }

    @Override
    public int addAllTo(Term[] t, int offset) {
        int s = subs();
        for (int i = 0; i < s; )
            t[offset++] = sub(i++);
        return s;
    }

    @Override
    public int subStructure() {
        return 0;
    }
}

package nars.derive.rule;

import jcog.TODO;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.derive.PreDerivation;
import nars.derive.action.How;
import nars.derive.util.Forkable;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.control.PREDICATE;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

/**
 * an intermediate representation of a premise rule
 * with fully expanded opcodes
 *
 * instantiated for each NAR, because it binds the conclusion steps to it
 *
 * anything non-NAR specific (static) is done in PremiseDeriverSource as a
 * ready-made template to make constructing this as fast as possible
 * in potentially multiple NAR instances later
 */
public class PremiseRule extends ProxyTerm  {

    final PREDICATE<PreDerivation>[] condition;
    final Function<RuleCause, How> action;


    @Nullable
    public final String tag;

    public PremiseRule(Term id, String tag, PREDICATE<PreDerivation>[] condition, Function<RuleCause, How> action) {
        super(id);

        this.tag = tag;
        this.condition = condition;
        this.action = action;
    }

    /** instance a list of conditions */
    FasterList<PREDICATE<PreDerivation>> conditions(short i) {
        FasterList<PREDICATE<PreDerivation>> pre = new FasterList<>(this.condition.length + 1);
        pre.addAll(this.condition);
        pre.add(new Forkable(/* branch ID */  i));
        return pre;
    }

    How action(NAR n, Map<String, RuleCause> tags) {
        return action.apply(
            //tag==null ? cause(n, ref) : tags.computeIfAbsent(tag, t -> cause(n, Atomic.the(t))) //GROUPED
            cause(n, ref) //UNIQUE
        );
    }

    private static RuleCause cause(NAR n, Term ref) {
        return n.newCause(ruleInstanceID -> new RuleCause(ref, ruleInstanceID));
    }

    @Override
    public final boolean the() {
        return false;
    }

    @Override
    public String toString() {
        return ref.toString();
    }

    @Override
    public final Subterms subterms() {
        return ref.subterms();
    }

    @Override
    public Subterms subtermsDirect() {
        return ((Compound)ref).subtermsDirect();
    }

    @Override
    public final int dt() {
        return ref.dt();
    }

    @Override
    public Op op() {
        return ref.op();
    }

    @Override
    public Term unneg() {
        return ifDifferentElseThis(ref.unneg());
    }

    @Override
    public final Term ifDifferentElseThis(Term u) {
		//continue proxying
		return u == ref ? this : u;
    }

    @Override
    public @Nullable Term replaceAt(ByteList path, Term replacement) {
        throw new TODO();
    }

    @Override
    public @Nullable Term replaceAt(ByteList path, int depth, Term replacement) {
        throw new TODO();
    }

    @Override
    public @Nullable Term normalize(byte varOffset) {
        return ifDifferentElseThis(ref.normalize(varOffset));
    }

    @Override
    public int volume() {
        return ref.volume();
    }

    @Override
    public int complexity() {
        return ref.complexity();
    }

    @Override
    public int structure() {
        return ref.structure();
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ProxyTerm)
            o = ((ProxyTerm)o).ref;
//        if (o instanceof Termed)
//            o = ((Termed)o).term();
        return ref.equals(o);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public Term root() {
        return ifDifferentElseThis(ref.root());
    }

    @Override
    public Term concept() {
        return ifDifferentElseThis(ref.concept());
    }

    @Override
    public boolean isCommutative() {
        return ref.isCommutative();
    }

    @Override
    public void appendTo(Appendable w) throws IOException {
        ref.appendTo(w);
    }

    @Override
    public boolean isNormalized() {
        return ref.isNormalized();
    }

    @Override
    public Term sub(int i) {
        return ref.sub(i);
    }

    @Override
    public int subs() {
        return ref.subs();
    }

    @Override
    public final int compareTo(Term t) {
        return ref.compareTo(t);
    }

    @Override
    public int vars() {
        return ref.vars();
    }

    @Override
    public int varIndep() {
        return ref.varIndep();
    }

    @Override
    public int varDep() {
        return ref.varDep();
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }

    @Override
    public int varPattern() {
        return ref.varPattern();
    }
}

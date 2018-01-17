package nars.subterm;

import com.google.common.collect.Iterators;
import nars.Op;
import nars.derive.match.EllipsisMatch;
import nars.term.Term;
import nars.term.subst.Unify;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Size 1 TermVector
 */
public class TermVector1 extends TermVector /*implements Set<Term>*/ {

    public final Term sub;


    public TermVector1(Term sub, TermVector of) {
        super(of.hashCodeSubterms(), of.structure,
             of.varPattern, of.varDep, of.varQuery, of.varIndep,
             of.complexity, of.volume, of.normalized
        );
        this.sub = sub;
    }

    public TermVector1(Term sub) {
        super(sub);
        assert(!(sub instanceof EllipsisMatch));

        this.sub = sub;
    }


    @Override
    public boolean unifyLinear(Subterms Y, Unify u) {
        return sub.unify(Y.sub(0), u);
    }


    @Override
    public final boolean isTemporal() {
        return sub.isTemporal();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Subterms) {
            Subterms t = (Subterms) obj;
            if (hash == t.hashCodeSubterms() && t.subs() == 1 && sub.equals(t.sub(0))) {
                if (t instanceof TermVector)
                    equivalentTo((TermVector) t);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equalTerms(Term[] c) {
        return c.length == 1 && sub.equals(c[0]);
    }

    @Override
    public Term[] arrayClone() {
        return new Term[]{sub};
    }

    @Override
    public final Term sub(int i) {
        if (i != 0)
            throw new ArrayIndexOutOfBoundsException();
        return sub;
    }

    @Override
    public boolean subIs(int i, Op o) {
        return i == 0 && sub.op() == o;
    }

    @Override
    public boolean subEquals(int i, Term x) {
        return i == 0 && sub.equals(x);
    }


    @Override
    public final int subs() {
        return 1;
    }

    @Override
    @NotNull
    public MutableSet<Term> toSet() {
        return Sets.mutable.of(sub);
    }

    @Override
    public Set<Term> toSet(Predicate<Term> ifTrue) {
        return ifTrue.test(sub) ? toSet() : Collections.emptySet();
    }

    @Override
    public String toString() {
        return "(" + sub + ')';
    }



    @Override
    public Iterator<Term> iterator() {
        return Iterators.singletonIterator(sub);
    }


    @Override
    public void forEach(Consumer<? super Term> action, int start, int stop) {
        if (start!=0 || stop != 1)
            throw new ArrayIndexOutOfBoundsException();
        action.accept(sub);
    }

    @Override
    public void forEach(Consumer<? super Term> action) {
        action.accept(sub);
    }

    @Override
    public final boolean contains(Term t) {
        return sub.equals(t);
    }

    @Override
    public boolean OR(Predicate<Term> p) {
        return p.test(sub);
    }

    @Override
    public boolean AND(Predicate<Term> p) {
        return p.test(sub);
    }


}

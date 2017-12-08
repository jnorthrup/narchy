package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.Subterms;
import nars.term.container.TermVector1;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

/**
 * Compound inheriting directly from TermVector1
 */
public class UnitCompound1 extends TermVector1 implements Compound {

    private final Op op;

    /** hash including this compound's op (cached) */
    transient private final int chash;

    /** structure including this compound's op (cached) */
    transient private final int cstruct;

    public UnitCompound1(/*@NotNull*/ Op op, /*@NotNull*/ Term arg) {
        super(arg);

        this.op = op;
        this.chash = Util.hashCombine(hashCodeSubterms(), op.id);
        this.cstruct = op.bit | arg.structure();

        if (!normalized && arg.isNormalized())
            setNormalized();
    }



    @Override
    public final int structure() {
        return cstruct;
    }


    @Override
    public void recurseTerms(/*@NotNull*/ Consumer<Term> v) {
        v.accept(this);
        sub.recurseTerms(v);
    }

    @Override
    public boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return inSubtermsOf.test(this) && (sub.equals(t) || sub.containsRecursively(t, root, inSubtermsOf));
    }


    @Override
    public int hashCode() {
        return chash;
    }



    @Override
    public Term dt(int nextDT) {
        switch (nextDT) {
            case DTERNAL:
//            case XTERNAL:
//            case 0:
                return this;
        }
        throw new UnsupportedOperationException();
    }


    @Override
    public final boolean equals(@Nullable Object that) {
        if (this == that) return true;
        if (!(that instanceof Compound) || chash !=that.hashCode())
            return false;

        Term tt = (Term) that;
        return tt.subs()==1 && opX()==tt.opX() && sub.equals(tt.sub(0)); //elides dt() comparison
//
//        if (hash == that.hashCode()) {
//            Term t = (Term)that;
//            return (op == t.op()) && (t.size() == 1) && /*&& (t.dt() == DTERNAL) &&*/ sub.equals(t.sub(0));
//        }
//
//        return false;
    }


    /*@NotNull*/
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public final /*@NotNull*/ Op op() {
        return op;
    }

    @Deprecated /* HACK */
    @Override
    public /*@NotNull*/ Subterms subterms() {

        //return new TermVector1(sub);
        return new TermVector1(sub, this);
        //return new SubtermView(this);
    }




    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > sub.volume() /* volume() -  size() */;
    }

    @Override
    public boolean isNormalized() {
        return sub.isNormalized();
    }

    @Override
    public int dt() {
        return DTERNAL;
    }

//    private static final class SubtermView implements TermContainer {
//        private final UnitCompound1 c;
//
//        public SubtermView(UnitCompound1 c) {
//            this.c = c;
//        }
//
//        @Override
//        public Term[] toArray() {
//            return new Term[]{c.sub};
//        }
//
//        @Override
//        public boolean isTemporal() {
//            return c.sub.isTemporal();
//        }
//
//        @Override
//        public int vars() {
//            return c.sub.vars();
//        }
//
//        @Override
//        public int varQuery() {
//            return c.sub.varQuery();
//        }
//
//        @Override
//        public int varDep() {
//            return c.sub.varDep();
//        }
//
//        @Override
//        public int varIndep() {
//            return c.sub.varIndep();
//        }
//
//        @Override
//        public int varPattern() {
//            return c.varPattern();
//        }
//
//
//        @Override
//        public int structure() {
//            return c.sub.structure();
//        }
//
//        @Override
//        public int volume() {
//            return c.volume();
//        }
//
//        @Override
//        public int complexity() {
//            return c.complexity();
//        }
//
//        @Override
//        public /*@NotNull*/ Term sub(int i) {
//            assert(i == 0);
//            return c.sub;
//        }
//
//        @Override
//        public int hashCode() {
//            return c.hashCodeSubTerms();
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj) return true;
//
//            if (obj instanceof TermContainer) {
//                TermContainer t = (TermContainer) obj;
//                if (t.size() == 1) {
//                    return c.sub.equals(t.sub(0));
//                }
//            }
//            return false;
//        }
//
//        @Override
//        public void forEach(Consumer<? super Term> action, int start, int stop) {
//            assert(start == 0 && stop == 1);
//            action.accept(c.sub);
//        }
//
//        /*@NotNull*/
//        @Override
//        public Iterator<Term> iterator() {
//            return singletonIterator(c.sub);
//        }
//
//        @Override
//        public int size() {
//            return 1;
//        }
//    }
}

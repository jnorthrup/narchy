package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Term;
import nars.term.sub.TermVector1;
import org.jetbrains.annotations.Nullable;

/**
 * Compound inheriting directly from TermVector1
 */
public class UnitCompound1 extends TermVector1 implements AbstractUnitCompound {

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
    public final boolean equals(@Nullable Object that) {
        if (this == that) return true;
        if (!(that instanceof Term) || chash != that.hashCode())
            return false;
        return equivalent((Term)that);
    }

    @Override
    public Term sub() {
        return sub;
    }

    @Override
    public final int structure() {
        return cstruct;
    }


    @Override
    public int hashCode() {
        return chash;
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

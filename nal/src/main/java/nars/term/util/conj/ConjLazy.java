package nars.term.util.conj;

import nars.term.Term;
import org.eclipse.collections.api.iterator.LongIterator;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * prepares construction of a conjunction target from components,
 * in the most efficient method possible according to them.
 * it is lighter weight than Conj.java in buffering / accumulating
 * events prior to complete construction.
 */
public class ConjLazy extends LongObjectArraySet<Term> implements ConjBuilder {


    public ConjLazy(int expectedSize) {
        super(expectedSize);
    }

    public ConjLazy() {
        this(4);
    }

    /** consistent with ConjBuilder - semantics slightly different than superclass and List.add: returns true only if False or Null have been added; a duplicate value returns true */
    @Override public boolean add(long when, Term t) {
        boolean result = true;
        if (t == True)
            return true; //ignore

        if (t == False || t == Null) {
            clear(); //fail
            result = false;
        }

        super.add(when, t);

        return result;
    }

    @Override
    public final LongIterator eventOccIterator() {
        return longIterator();
    }

    public static ConjLazy events(Term conj) {
        return events(conj, 0);
    }

    public static ConjLazy events(Term conj, long occOffset) {
        ConjLazy l = new ConjLazy(conj.op() == CONJ ? 4 : 1);
        conj.eventsWhile(l::add,
                occOffset, true, true, false);
        return l;
    }

    @Override
    protected final Object[] newArray(int newCapacity) {
        return new Term[newCapacity];
    }

    @Override
    public void negateEvents() {
        replaceAll(Term::neg);
    }

    @Override
    public Term term() {
        int n = size();
        switch (n) {
            case 0:
                return True;
            case 1:
                return get(0);
            case 2: {
                long w0 = when[0], w1 = when[1];

                if (w0 == ETERNAL && w1 != ETERNAL)
                    w0 = w1 = 0; //quick promote to parallel
                else if (w1 == ETERNAL && w0 != ETERNAL)
                    w0 = w1 = 0; //quick promote to parallel

                if (w0 == w1) {
                    Term a = items[0], b = items[1];
                    if (a.equals(b))
                        return a; //quick test
                    else
                        return ConjCommutive.the((w0 == ETERNAL) ? DTERNAL : 0, a, b);
                }
                break;
            }
            default: {
                long w0 = when[0];
                boolean parallel = true;
                for (int i = 1, whenLength = when.length; i < whenLength; i++) {
                    if (when[i] != w0) {
                        parallel = false;
                        break; //difference
                    }
                }
                //all same time
                if (parallel)
                    return ConjCommutive.the((w0 == ETERNAL) ? DTERNAL : 0, this);
            }
        }

        //failsafe impl:
        Conj c = new Conj(n);
        for (int i = 0; i < n; i++) {
            Term t = this.get(i);
            if (!c.add(when[i], t))
                break;
        }
        return c.term();
    }



}

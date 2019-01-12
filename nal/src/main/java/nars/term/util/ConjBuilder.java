package nars.term.util;

import nars.term.Term;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

public interface ConjBuilder {
    boolean add(long at, Term x);
    boolean remove(long at, Term t);
    boolean removeAll(Term term);

    void negateEvents();

    Term term();

    default boolean addAuto(Term t) {
        return add(t.dt() == DTERNAL ? ETERNAL : 0, t);
    }



}

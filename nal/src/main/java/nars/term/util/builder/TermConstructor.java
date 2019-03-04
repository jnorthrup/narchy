package nars.term.util.builder;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;

/** lowest-level (raw, possibly un-checked) compound construction interface */
@FunctionalInterface public interface TermConstructor {

    default Term compound(Op op, int dt, Subterms t) {
        return compound(op, dt, t.arrayShared());
    }

    Term compound(Op op, int dt, Term... subterms);

//    default Term compound(Op op, int dt, Term... subterms) {
//        return compound(op, dt, $.vFast(subterms));
//    }

}

package nars.term;

import nars.Op;
import nars.subterm.Subterms;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/** effectively a marker interface indicating the possibility of a term involving dt */
public interface CompoundDT extends Compound {

    @Override
    int dt();

    @Override
    default int subTimeSafe(Term x, int after) {
        if (equals(x))
            return 0;

        Op op = op();
        if (!op.temporal || impossibleSubTerm(x))
            return DTERNAL;

        //TODO do shuffled search to return different equivalent results wherever they may appear


        int dt = dt();
//        if (dt == DTERNAL)
//            return DTERNAL;
        if (dt == XTERNAL) //unknown
            return DTERNAL;

        /*@NotNull*/
        Subterms yy = subterms();


        if (op == IMPL) {
            //only two options
            Term s0 = yy.sub(0);
            if (s0.equals(x)) {
                return 0;
            }
            int s1offset = s0.dtRange() + (dt == DTERNAL ? 0 : dt);
            Term s1 = yy.sub(1);
            if (s1.equals(x)) {
                return s1offset; //the subject's dtrange + the dt between points to the start of the predicate
            }
            if (s0.op() == CONJ) {
                int s0d = s0.subTimeSafe(x);
                if (s0d != DTERNAL)
                    return s0d;
            }
            if (s1.op() == CONJ) {
                int s1d = s1.subTimeSafe(x);
                if (s1d != DTERNAL)
                    return s1d + s1offset;
            }

        } else if (op == CONJ) {

            if (after >= dt && yy.sub(0).equals(yy.sub(1)) /* HACK apply to other cases too */) {
                //repeat
                //return yy.sub(1).subTimeSafe(x, after - dt) + dt;
                if (x.equals(yy.sub(1)))
                    return dt;
            }

            boolean reverse;
            int idt;
            if (dt == DTERNAL || dt == 0) {
                idt = 0; //parallel or eternal, no dt increment
                reverse = false;
            } else {
                idt = dt;
                if (idt < 0) {
                    idt = -idt;
                    reverse = true;
                } else {
                    reverse = false;
                }
            }

            int ys = yy.subs();
            int offset = 0;
            for (int yi = 0; yi < ys; yi++) {
                Term yyy = yy.sub(reverse ? ((ys - 1) - yi) : yi);
                int sdt = yyy.subTimeSafe(x, after - offset);
                if (sdt != DTERNAL)
                    return sdt + offset;
                offset += idt + yyy.dtRange();
            }
        }

        return DTERNAL;
    }
}

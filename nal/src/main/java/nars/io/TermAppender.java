package nars.io;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * prints readable forms of terms
 */
public final class TermAppender{

    static void compoundAppend(Compound c, Appendable p, Op op) throws IOException {

        p.append(Op.COMPOUND_TERM_OPENER);

        op.append(c, p);

        Subterms cs = c.subterms();
        if (cs.subs() == 1)
            p.append(Op.ARGUMENT_SEPARATOR);

        appendArgs(cs, p);

        p.append(Op.COMPOUND_TERM_CLOSER);

    }

    /** auto-infix if subs == 2 */
    static void compoundAppend(String o, Subterms c, Appendable p, UnaryOperator<Term > filter) throws IOException {

        p.append(Op.COMPOUND_TERM_OPENER);

        int n = c.subs();
        if (n == 2) {


            appendArg(c, 0, filter, p);

            if (o.endsWith(" "))
                p.append(' '); //prepend ' '

            p.append(o);

            appendArg(c, 1, filter, p);

        } else {

            p.append(o);

            if (n == 1)
                p.append(Op.ARGUMENT_SEPARATOR);

            appendArgs(c, filter, p);
        }

        p.append(Op.COMPOUND_TERM_CLOSER);

    }


    static void appendArgs(Subterms c, Appendable p) throws IOException {
        int nterms = c.subs();

        boolean bb = nterms > 1;
        for (int i = 0; i < nterms; i++) {
            if ((i != 0) || bb) {
                p.append(Op.ARGUMENT_SEPARATOR);
            }
            c.sub(i).appendTo(p);
        }
    }

    static void appendArgs(Subterms c, UnaryOperator<Term> filter, Appendable p) throws IOException {
        int nterms = c.subs();

        boolean bb = nterms > 1;
        for (int i = 0; i < nterms; i++) {
            if ((i != 0) || bb) {
                p.append(Op.ARGUMENT_SEPARATOR);
            }
            appendArg(c, i, filter, p);
        }
    }

    private static void appendArg(Subterms c, int i, UnaryOperator<Term> filter, Appendable p) throws IOException {
        filter.apply(c.sub(i)).appendTo(p);
    }

    public static void append(Compound c, Appendable p) throws IOException {
        Op op = c.op();

        switch (op) {

//            case SECTi:
//            case SECTe:
//                sectAppend(c, p);
//                return;

            case SETi:
            case SETe:
                setAppend(c, p);
                return;
            case PROD:
                productAppend(c.subtermsDirect(), p);
                return;
            case NEG:
                negAppend(c, p);
                return;
            case FRAG:
                fragAppend(c, p);
                return;

            default:
                if (op.statement || c.subs() == 2) {


                    if (c.hasAll(Op.FuncBits)) {
                        Term subj = c.sub(0);
                        if (op == INH && subj.op() == Op.PROD) {
                            Term pred = c.sub(1);
                            Op pOp = pred.op();
                            if (pOp == ATOM) {
                                operationAppend((Compound) subj, (Atomic) pred, p);
                                return;
                            }
                        }
                    }

                    statementAppend(c, p, op);

                } else {
                    compoundAppend(c, p, op);
                }
                break;
        }

    }

    private static void fragAppend(Compound c, Appendable p) throws IOException {
        p.append(FRAG.ch);
        appendSubterms(c.subterms(), p);
        p.append(FRAG.ch);
    }

//    static void sectAppend(Compound c, Appendable p) throws IOException {
//        Op o = c.op();
//        Subterms cs = c.subterms();
//        if (cs.subs() == 2) {
//            Term subracted = cs.sub(0), from;
//            //negated subterm will be in the 0th position, if anywhere due to target sorting
//            if (subracted.op() == NEG && (from=cs.sub(1)).op()!=NEG) {
//                p.append('(');
//                from.appendTo(p);
//                p.append(o == SECTe ? DIFFi : DIFFe);
//                subracted.unneg().appendTo(p);
//                p.append(')');
//                return;
//            }
//
//            statementAppend(c, p, o);
//        } else {
//            compoundAppend(c, p, o);
//        }
//    }

    static void negAppend(Compound neg, Appendable p) throws IOException {
        /**
         * detect a negated conjunction of negated subterms:
         * (--, (&&, --A, --B, .., --Z) )
         */

        Term sub = neg.unneg();

        if ((sub.opID() == (int) CONJ.id) && sub.hasAny(NEG.bit)) {
            int dt;
            if ((((dt = sub.dt()) == DTERNAL) || (dt == XTERNAL))) {
                Subterms cxx = sub.subterms();
                if ((cxx.hasAny(NEG) ? cxx.count(new Predicate<Term>() {
                    @Override
                    public boolean test(Term x) {
                        return x instanceof Neg && !x.hasAny(CONJ);
                    }
                }) : 0) >= cxx.subs() / 2) {
                    disjAppend(cxx, dt, p);
                    return;
                }
            }
        }

        p.append("(--,");
        sub.appendTo(p);
        p.append(')');
    }

    private static void disjAppend(Subterms cxx, int dt, Appendable p) throws IOException {
        compoundAppend(disjStr(dt), cxx, p, Term::neg);
    }

    private static String disjStr(int dt) {
        String s;
        switch (dt) {
            case XTERNAL:
                s = Op.DISJstr + "+- ";
                break;
            case DTERNAL:
                s = Op.DISJstr;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return s;
    }


    static void statementAppend(Term c, Appendable p, Op op  /**/) throws IOException {


        p.append(Op.COMPOUND_TERM_OPENER);

        int dt = c.dt();

        boolean reversedDT = dt != DTERNAL && /*dt != XTERNAL && */ dt < 0 && op.commutative;

        Subterms cs = c.subterms();
        cs.sub(reversedDT ? 1 : 0).appendTo(p);

        op.append(dt, p, reversedDT);

        cs.sub(reversedDT ? 0 : 1).appendTo(p);

        p.append(Op.COMPOUND_TERM_CLOSER);
    }


    static void productAppend(Subterms product, Appendable p) throws IOException {

        p.append(Op.COMPOUND_TERM_OPENER);
        appendSubterms(product, p);
        p.append(Op.COMPOUND_TERM_CLOSER);
    }

    private static void appendSubterms(Subterms x, Appendable p) throws IOException {
        int s = x.subs();
        for (int i = 0; i < s; i++) {
            x.sub(i).appendTo(p);
            if (i < s - 1)
                p.append(',');
        }
    }


    static void setAppend(Compound set, Appendable p) throws IOException {

        int len = set.subs();


        char opener, closer;
        if (set.op() == Op.SETe) {
            opener = Op.SETe.ch;
            closer = Op.SET_EXT_CLOSER;
        } else {
            opener = Op.SETi.ch;
            closer = Op.SET_INT_CLOSER;
        }

        p.append(opener);

        Subterms setsubs = set.subterms();
        for (int i = 0; i < len; i++) {
            if (i != 0) p.append(Op.ARGUMENT_SEPARATOR);
            setsubs.sub(i).appendTo(p);
        }
        p.append(closer);
    }

    static void operationAppend(Compound argsProduct, Atomic operator, Appendable w) throws IOException {

        operator.appendTo(w);

        w.append(Op.COMPOUND_TERM_OPENER);


        argsProduct.forEachI(new ObjectIntProcedure<Term>() {
            @Override
            public void value(Term t, int n) {
                try {
                    if (n != 0)
                        w.append(Op.ARGUMENT_SEPARATOR);

                    t.appendTo(w);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        w.append(Op.COMPOUND_TERM_CLOSER);

    }


}

package nars.term.util;

import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.*;
import nars.term.var.ellipsis.Ellipsis;
import nars.time.Tense;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * statements include: inheritance -->, similarity <->, and implication ==>
 */
public class Statement {


    static final int mobiusExcept = Op.or(/*CONJ, */VAR_PATTERN);

    public static Term statement(TermBuilder B, Op op, int dt, Term subject, Term predicate) {
        return statement(B, op, dt, subject, predicate, 3);
    }

    public static Term statement(TermBuilder B, Op op, int dt, Term subject, Term predicate, int depth) {
        if (subject == Null || predicate == Null)
            return Null;

        if (depth <= 0) {
            if (NAL.DEBUG) {
                throw new TermException("statement recursion limit", op, dt, subject, predicate);
            }
            return Null; //TODO
        }


        if (op == IMPL && dt == DTERNAL)
            dt = 0; //temporarily use dt=0

        boolean dtConcurrent = dt != XTERNAL && Conj.concurrent(dt);

        if (dtConcurrent) {
            if (subject.equals(predicate))
                return True;
            else if (subject.equalsNeg(predicate))
                return False;


            if (op == INH || op == SIM) {
                if (subject.unneg().equalsRoot(predicate.unneg()))
                    return Null; //dont support non-temporal statements where the root is equal because they cant be conceptualized
            }
        }


        if (op == IMPL) {

            if (subject == True)
                return predicate;
            if (subject == False)
                return Null;
            if (!NAL.IMPLICATION_SUBJECT_CAN_CONTAIN_IMPLICATION && subject.hasAny(IMPL)) {
                return Null; //throw new TODO();
            }
            if (!subject.op().eventable)
                return Null;
        }

        if (dtConcurrent) {
            if (subject instanceof Bool || predicate instanceof Bool)
                return Null;
        }

        boolean negate = false;

        if (op == IMPL) {
            if (predicate.op() == NEG) {
                predicate = predicate.unneg();
                negate = !negate;
            }

            switch (predicate.op()) {
                case BOOL:
                    //reduce to the subject as a general condition for the superclass to utilize
                    if (predicate == True)
                        return subject;
                    else if (predicate == False)
                        return subject.neg();
                    else
                        return Null;

                case IMPL: {
                    Term newSubj, inner = predicate.sub(0);
                    if (dt == DTERNAL || dt == XTERNAL) {
                        newSubj = CONJ.the(B, dt, subject, inner);
                    } else {
                        newSubj = ConjSeq.sequence(subject, 0, inner, subject.eventRange() + dt, B);
                    }
                    int newDT = predicate.dt();
                    if (newDT == DTERNAL)
                        newDT = 0; //temporary
                    Term newPred = predicate.sub(1);
                    if (dt!=newDT || !newSubj.equals(subject) || !newPred.equals(predicate))
                        return statement(B, IMPL, newDT, newSubj, newPred, depth-1).negIf(negate); //recurse
                    break;
                }

            }

            if (!predicate.op().eventable)
                return Null;


            //TODO simple case when no CONJ or IMPL are present

            if (dt != XTERNAL) {

                if (!(subject instanceof Compound) && !(predicate instanceof Compound)) {
                    //no validity test necessary
                } else if (subject.dt() == XTERNAL || predicate.dt() == XTERNAL) { // && !subject.OR(x->x instanceof Ellipsis) && !predicate.OR(x->x instanceof Ellipsis) ) {
                    //do not reduce
                } else if (subject.unneg().op()!=CONJ && predicate.unneg().op()!=CONJ) {
                    //both simple terms
                } else if (!Term.commonStructure(subject, predicate)) {
                    //no validity test necessary
//                    } else if (subject instanceof Compound && !(predicate instanceof Compound) && !subject.containsRecursively(predicate)) {
//                        //no validity test necessary
//                    } else if (predicate instanceof Compound && !(subject instanceof Compound) && !predicate.containsRecursively(subject)) {
//                        //no validity test necessary
                } else if (Ellipsis.firstEllipsis(subject) != null || Ellipsis.firstEllipsis(predicate) != null) {
                    //do not reduce
                } else {

                    int subjRange = subject.eventRange();
                    long po = subjRange + dt; //predicate occurrence

                    //subtract any common subject components from predicate
                    Term newPred;
                    ConjLazy newPredConj = ConjLazy.events(predicate, po);
                    int removed =
                        newPredConj.removeAll(subject.unneg(), 0, subject.op()!=NEG);
                    switch (removed) {
                        case -1: return False;
                        case +1: newPred = newPredConj.term(B); break;
                        default: newPred = null; break;
                    }

//                    ConjBuilder newPredConj = ConjDiff.the(predicate, po, subject, 0);
//                    Term newPred = newPredConj.term(B);


                    boolean predChange = newPred!=null && !predicate.equals(newPred);

                    if (predChange) {

                        if (newPred instanceof Bool)
                            return newPred.negIf(negate); //collapse


                        if (dt != DTERNAL) {
                            long shift = newPredConj.shift();
                            if (shift == ETERNAL) {
                                //??
                                dt = 0;
                            } else {
//

                                dt = Tense.occToDT(shift - subjRange);

                                if (newPred.dt() == 0 && predicate.dt() == DTERNAL && predicate.subterms().equals(newPred.subterms())) {
                                    //HACK return to dternal
                                    if (newPred instanceof Compound)
                                        newPred = ((Compound) newPred).dt(DTERNAL, B);

                                }
                            }

                        }

                        if (newPred.op()==NEG) { //attempt to exit infinite loop of negations
                            newPred = newPred.unneg();
                            negate = !negate;
                        }

                        if (!newPred.equals(predicate)) { //HACK check again
                            return statement(B, IMPL, dt, subject, newPred, depth-1).negIf(negate); //recurse
                        }

                    }

                }
            }

        }


        assert(op!=IMPL || dt!=DTERNAL); //HACK dt should ==0

        if ((op != IMPL)
                //|| (dt == 0) /* allow parallel IMPL unless there is a sequence that could separate the events from overlap */
                || (dt == 0 && !Conj.isSeq(subject) && !Conj.isSeq(predicate))
        ) {
            if ((statementLoopy(subject.unneg(), predicate.unneg())))
                return Null;
        }

//            boolean sa = subject instanceof AliasConcept.AliasAtom;
//            if (sa) {
//                Term sd = ((AliasConcept.AliasAtom) subject).target;
//                if (sd.equals(predicate) || containEachOther(sd, predicate, delim))
//                    return Null;
//            }
//            boolean pa = predicate instanceof AliasConcept.AliasAtom;
//            if (pa) {
//                Term pd = ((AliasConcept.AliasAtom) predicate).target;
//                if (pd.equals(subject) || containEachOther(pd, subject, delim))
//                    return Null;
//            }
//            if (sa && pa) {
//                if (containEachOther(((AliasConcept.AliasAtom) subject).target, ((AliasConcept.AliasAtom) predicate).target, delim))
//                    return Null;
//            }
//            }

        if (NAL.term.INH_CLOSED_BOOLEAN_DUALITY_MOBIUS_PARADIGM) {
            if (op == INH /*|| op == SIM*/) {
                boolean sn = subject.op() == NEG && !subject.unneg().op().isAny(mobiusExcept);
                boolean pn = predicate.op() == NEG && !predicate.unneg().op().isAny(mobiusExcept);
//                if (!sn && !pn) {
//                    //normal
//                } else if (sn && pn) {
//                    //double-negative
//
////                    subject = subject.unneg();
////                    predicate = predicate.unneg();
//
//                } else
                    if (sn) {
                        negate = !negate;
                        subject = subject.unneg();
                    }
//                } else /* pn */ {
                if (pn) {
                    //(a --> --b) |- --(a --> b)   but (--a --> b) != --(a --> b)
                    negate = !negate;
                    predicate = predicate.unneg();
                }
            }
        }

        if (op == SIM) {
            if (subject.compareTo(predicate) > 0) {
                //swap to natural order
                Term x = predicate;
                predicate = subject;
                subject = x;
            }
        }


        if (op == INH && !NAL.term.INH_IMAGE_RECURSION) {
            Term inhCollapsed = Image.recursionFilter(subject, predicate);
            if (inhCollapsed instanceof Bool)
                return inhCollapsed;
        }


        if (op == IMPL && dt == 0)
            dt = DTERNAL; //generalize to DTERNAL ==>

        Term t = B.theCompound(op, dt, subject, predicate);


        return t.negIf(negate);
    }

    public static boolean statementLoopy(Term x, Term y) {
        if (!(x instanceof Atomic) && !(y instanceof Atomic))
            return false;

//        boolean xByComponnet = x instanceof Compound && x.op()==CONJ;
//        boolean yByComponent = y instanceof Compound && y.op()==CONJ;
//        if (!xByComponnet && !yByComponent) {
            return _statementLoopy(x, y);
//        } else if (xByComponnet && !yByComponent) {
//            return x.subterms().ORwith(Op::_statementLoopy, y);
//        } else if (yByComponent && !xByComponnet) {
//            return y.subterms().ORwith(Op::_statementLoopy, x);
//        } else {
//            if (x.volume() >= y.volume())
//                return x.subterms().ORwith((xx,Y) -> Y.subterms().ORwith(Op::_statementLoopy, xx), y);
//            else
//                return y.subterms().ORwith((yy,X) -> X.subterms().ORwith(Op::_statementLoopy, yy), x);
//        }

    }
    private static boolean _statementLoopy(Term x, Term y) {

        int xv = x.volume(), yv = y.volume();
        boolean root = false;
        if (xv == yv) {
            return x.equals(y);
            //probably impossible:
//            boolean z = Term.commonStructure(x, y) &&
//                    (x.containsRecursively(y, root, delim) || y.containsRecursively(x, root, delim));
//            if (z)
//                throw new WTF();
//            return z;
        } else if (xv > yv)
            return x.containsRecursively(y, root, statementLoopyContainer);
        else
            return y.containsRecursively(x, root, statementLoopyContainer);
    }
}
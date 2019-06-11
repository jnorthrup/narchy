package nars.term.util;

import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjList;
import nars.term.var.ellipsis.Ellipsis;
import nars.time.Tense;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * statements include: inheritance -->, similarity <->, and implication ==>
 */
public class Statement {


    private static final int mobiusExcept = Op.or(/*CONJ, */VAR_PATTERN);

    public static Term statement(TermBuilder B, Op op, int dt, Term subject, Term predicate) {
        return statement(B, op, dt, subject, predicate, 3);
    }

    public static Term statement(TermBuilder B, Op op, int dt, Term subject, Term predicate, int depth) {
        if (subject == Null || predicate == Null)
            return Null;



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


        if (depth <= 0) {
            if (NAL.DEBUG) {
                throw new TermException("statement recursion limit", op, dt, subject, predicate);
            }
            return Null; //TODO
        }

        if (op == IMPL) {

            if (subject == True)
                return Null; //return predicate;
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
            if (predicate instanceof Neg) {
                predicate = predicate.unneg();
                negate = !negate;
            }

            switch (predicate.op()) {
                case BOOL:
//                    //reduce to the subject as a general condition for the superclass to utilize
//                    if (predicate == True)
//                        return subject;
//                    else if (predicate == False)
//                        return subject.neg();
//                    else
                        return Null;

                case IMPL: {
                    Term newSubj, inner = predicate.sub(0);
                    if (dt == DTERNAL || dt == XTERNAL) {
                        newSubj = CONJ.the(B, dt, subject, inner);
                    } else {
                        newSubj = B.conjAppend(subject, dt, inner);
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
                    ConjList newPredConj = ConjList.events(predicate, po);
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

                        if (newPred instanceof Neg) { //attempt to exit infinite loop of negations
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
                boolean sn = subject instanceof Neg && !subject.unneg().op().isAny(mobiusExcept);
                boolean pn = predicate instanceof Neg && !predicate.unneg().op().isAny(mobiusExcept);
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

        if ((op != IMPL)
                //|| (dt == 0) /* allow parallel IMPL unless there is a sequence that could separate the events from overlap */
                || (dt == 0 && !Conj.isSeq(subject) && !Conj.isSeq(predicate))
        ) {
            if ((Terms.eqRCom(subject.unneg(), predicate.unneg())))
                return Null;
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
            Term inhCollapsed = Image.recursionFilter(subject, predicate, B);
            if (inhCollapsed instanceof Bool)
                return inhCollapsed;
        }


        if (op == IMPL && dt == 0)
            dt = DTERNAL; //generalize to DTERNAL ==>

        Term t = B.newCompound(op, dt, subject, predicate);

        return t.negIf(negate);
    }

}
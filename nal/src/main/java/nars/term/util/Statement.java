package nars.term.util;

import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.*;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjList;
import nars.term.var.ellipsis.Ellipsis;
import nars.time.Tense;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * statements include: inheritance -->, similarity <->, and implication ==>
 */
public class Statement {


//    private static final int mobiusExcept = Op.or(/*CONJ, */VAR_PATTERN);

    public static Term statement(TermBuilder B, Op op, int dt, Term subject, Term predicate) {
        return statement(B, op, dt, subject, predicate, 6);
    }

    public static Term statement(TermBuilder B, Op op, int dt, Term subject, Term predicate, int depth) {
        if (subject == Null || predicate == Null)
            return Null;
        if (subject instanceof Img || predicate instanceof Img)
            throw new TermException("statement can not have image subterm", new TermList(subject, predicate));

        if (op == IMPL) {
            if (subject == True)
                return predicate;
            else if (subject == False)
                return predicate.neg();
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
                    return Null; //HACK dont support non-temporal statements where the root is equal because they arent conceptualized
            }
        }


        if (depth <= 0) {
            if (NAL.DEBUG) {
                throw new TermException("statement recursion limit", op, dt, subject, predicate);
            }
            return Null; //TODO
        }

        if (op == IMPL) {


            if (predicate == True)
                return subject;
            if (predicate == False)
                return subject.neg();

//            if (subject == True)
//                return Null; //return predicate;
//            if (subject == False)
//                return Null;

            if (!NAL.IMPLICATION_SUBJECT_CAN_CONTAIN_IMPLICATION && subject.hasAny(IMPL)) {
                return Null; //throw new TODO();
            }
            if (!subject.unneg().op().eventable)
                return Null;
            if (!predicate.unneg().op().eventable)
                return Null;
        }

        if (subject instanceof Bool || predicate instanceof Bool)
            return Null;

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
                    boolean predXternal = predicate.dt() == XTERNAL;
                    boolean xternal = dt == XTERNAL;
                    boolean bothXternal = xternal && predXternal;
                    if (!xternal && !predXternal || bothXternal || predXternal) {

                        //not when only inner xternal, since that transformation destroys temporal information

                        Term inner = predicate.sub(0);
                        Term newPred = predicate.sub(1);
                        Term newSubj;
                        int newDT;
                        if (bothXternal) {
                            newSubj = B.conj(XTERNAL, subject, inner);
                            newDT = XTERNAL;
                        } else {
                            newSubj = B.conjAppend(subject, dt, inner);
                            if (newSubj == Null)
                                return Null;
                            newDT = predicate.dt();
                            if (newDT == DTERNAL) newDT = 0; //HACK temporary
                        }

                        if (newPred instanceof Neg) {
                            newPred = newPred.unneg();
                            negate = !negate;
                        }
                        if (dt != newDT || !newSubj.equals(subject) || !newPred.equals(predicate))
                            return statement(B, IMPL, newDT, newSubj, newPred, depth - 1).negIf(negate); //recurse
                    }
                    break;
                }

            }

            if (!predicate.op().eventable)
                return Null;
        }

        if (NAL.term.INH_CLOSED_BOOLEAN_DUALITY_MOBIUS_PARADIGM) {
            if (op == INH /*|| op == SIM*/) {
                boolean sn = subject instanceof Neg;// && !subject.unneg().op().isAny(mobiusExcept);
                boolean pn = predicate instanceof Neg;// && !predicate.unneg().op().isAny(mobiusExcept);

                if (sn) {
                    negate = !negate;
                    subject = subject.unneg();
                }
                if (pn) {
                    negate = !negate;
                    predicate = predicate.unneg();
                }
            }
        }

        {

            //TODO simple case when no CONJ or IMPL are present

            if (dt != XTERNAL) {

                if (!(subject instanceof Compound) && !(predicate instanceof Compound)) {
                    //no validity test necessary
                } else if (subject.dt() == XTERNAL || predicate.dt() == XTERNAL) { // && !subject.OR(x->x instanceof Ellipsis) && !predicate.OR(x->x instanceof Ellipsis) ) {
                    //do not reduce
                } else if (subject.unneg().op()!=CONJ && predicate.unneg().op()!=CONJ) {
                    //both simple terms
                } else if (!Term.commonStructure(subject.structure() & (~CONJ.bit), predicate.structure() & (~CONJ.bit))) {
                    //no validity test necessary
//                    } else if (subject instanceof Compound && !(predicate instanceof Compound) && !subject.containsRecursively(predicate)) {
//                        //no validity test necessary
//                    } else if (predicate instanceof Compound && !(subject instanceof Compound) && !predicate.containsRecursively(subject)) {
//                        //no validity test necessary
                } else if (Ellipsis.firstEllipsis(subject) != null || Ellipsis.firstEllipsis(predicate) != null) {
                    //do not reduce
                } else {

                    if (op == IMPL) {
                        int subjRange = subject.eventRange();
                        long po = subjRange + dt; //predicate occurrence

                        //subtract any common subject components from predicate
                        ConjList newPredConj = ConjList.events(predicate, po);
                        int removed = newPredConj.removeAll(subject.unneg(), 0, !(subject instanceof Neg));
                        Term newPred;
                        switch (removed) {
                            case -1:
                                return False.negIf(negate);
                            case +1:
                                newPred = newPredConj.term(B);
                                break;
                            default:
                                newPred = null;
                                break;
                        }

                        if (newPred != null && !predicate.equals(newPred)) {

                            if (newPred instanceof Bool)
                                return newPred.negIf(negate); //collapse


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

                            if (newPred instanceof Neg) { //attempt to exit infinite loop of negations
                                newPred = newPred.unneg();
                                negate = !negate;
                            }

                            if (!newPred.equals(predicate)) { //HACK check again
                                return statement(B, IMPL, dt, subject, newPred, depth - 1).negIf(negate); //recurse
                            }

                        }
                    } else {
                        if (subject.dt()==DTERNAL && predicate.dt() == DTERNAL) { //TODO test earlier
                            //INH,SIM
                            //TODO swap order for optimal comparison
                            boolean sc = subject.op() == CONJ;
                            boolean pc = predicate.op() == CONJ;
                            if (sc && pc) {
                                Subterms ssub = subject.subterms();
                                Subterms psub = predicate.subterms();
                                Term[] common = ssub.subsIncluding(psub::contains);
                                if (common != null && common.length > 0) {
                                    int cn = common.length;
                                    if (cn == ssub.subs() || cn == psub.subs()) {
                                        //contained entirely by the other
                                        //True; //TODO negate
                                        return Null;
                                    }
                                    Predicate<Term> notCommon = common.length > 1 ? z -> ArrayUtil.indexOf(common, z) == -1 : z -> !common[0].equals(z);
                                    subject = CONJ.the(ssub.subsIncluding(notCommon));
                                    predicate = CONJ.the(psub.subsIncluding(notCommon));
                                    return statement(B, op, dt, subject, predicate, depth-1);
                                }
                            } else if (sc) {
                                Subterms ssub = subject.subterms();
//                                if (ssub.contains(predicate)) return True; //TODO negate
//                                if (ssub.containsNeg(predicate)) return False; //TODO negate
                                if (ssub.containsPosOrNeg(predicate)) return Null;
                            } else if (pc) {
                                Subterms psub = predicate.subterms();
                                //if (psub.contains(subject)) return True; //TODO negate
                                //if (psub.containsNeg(subject)) return False; //TODO negate
                                if (psub.containsPosOrNeg(subject)) return Null;
                            }
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


        if (recursive(op, dt, subject, predicate))
            return Null;

        if (op == SIM) {
            if (subject.compareTo(predicate) > 0) {
                //swap to natural order
                Term x = predicate;
                predicate = subject;
                subject = x;
            }
        }



        if (op == INH && (subject.hasAny(Op.IMG) || predicate.hasAny(Op.IMG))) {
            Term inhCollapsed = Image.recursionFilter(subject, predicate, B);
            if (inhCollapsed instanceof Bool)
                return inhCollapsed;
        }



        if (op == IMPL) {
            if (dt == 0)
                dt = DTERNAL; //HACK generalize to DTERNAL ==>
        }

        Term t = B.newCompound(op, dt, subject, predicate);

        return t.negIf(negate);
    }

    private static boolean recursive(Op op, int dt, Term subject, Term predicate) {

        if ((op != IMPL)
                //|| (dt == 0) /* allow parallel IMPL unless there is a sequence that could separate the events from overlap */
                || (dt == 0 && !Conj.isSeq(subject) && !Conj.isSeq(predicate))
        ) {
            //more fine grained inh/sim recursion test
//            final int InhOrSim = INH.bit | SIM.bit;
//            if (op.isAny(InhOrSim)) {
//                if (subject instanceof Compound && subject.isAny(InhOrSim)) {
//                    if (predicate instanceof Compound && predicate.op()==CONJ) {
//                        if (((Compound) predicate).containsPosOrNeg(subject.sub(0)) || ((Compound) predicate).containsPosOrNeg(subject.sub(1)))
//                            return true;
//                    }
//                    if (predicate.equals(subject.sub(0)) || predicate.equals(subject.sub(1)))
//                        return true;
//                }
//                if (predicate instanceof Compound && predicate.isAny(InhOrSim)) {
//                    if (subject instanceof Compound && subject.op()==CONJ) {
//                        if (((Compound) subject).containsPosOrNeg(predicate.sub(0)) || ((Compound) subject).containsPosOrNeg(predicate.sub(1)))
//                            return true;
//                    }
//                    if (subject.equals(predicate.sub(0)) || subject.equals(predicate.sub(1)))
//                        return true;
//
//                }
//            }

            if ((Terms.eqRCom(subject, predicate)))
                return true;
        }
        return false;
    }

}
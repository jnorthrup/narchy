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
        Term result = Null;
        boolean finished = false;
        if (subject != Null && predicate != Null) {
            if (subject instanceof Img || predicate instanceof Img)
                throw new TermException("statement can not have image subterm", new TermList(subject, predicate));
            if (op == IMPL) {
                if (subject == True) {
                    result = predicate;
                    finished = true;
                } else if (subject == False) {
                    result = predicate.neg();
                    finished = true;
                }
            }
            if (!finished) {
                if (op == IMPL && dt == DTERNAL)
                    dt = 0; //temporarily use dt=0
                boolean dtConcurrent = dt != XTERNAL && Conj.concurrent(dt);
                if (dtConcurrent) {
                    if (subject.equals(predicate)) {
                        result = True;
                        finished = true;
                    } else if (subject.equalsNeg(predicate)) {
                        result = False;
                        finished = true;
                    } else if (op == INH || op == SIM) {
                        if (subject.unneg().equalsRoot(predicate.unneg())) {
                            finished = true;//HACK dont support non-temporal statements where the root is equal because they arent conceptualized
                        }
                    }

                }
                if (!finished) {
                    if (depth <= 0) {
                        if (NAL.DEBUG) {
                            throw new TermException("statement recursion limit", op, dt, subject, predicate);
                        }
                        //TODO
                    } else {
                        if (op == IMPL) {


                            if (predicate == True) {
                                result = subject;
                                finished = true;
                            } else if (predicate == False) {
                                result = subject.neg();
                                finished = true;
                            } else if (!NAL.IMPLICATION_SUBJECT_CAN_CONTAIN_IMPLICATION && subject.hasAny(IMPL)) {
                                finished = true;//throw new TODO();
                            } else if (!subject.unneg().op().eventable) {
                                finished = true;
                            } else if (!predicate.unneg().op().eventable) {
                                finished = true;
                            }

                        }
                        if (!finished) {
                            if (!(subject instanceof Bool) && !(predicate instanceof Bool)) {
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
                                            finished = true;
                                            break;

                                        case IMPL: {
                                            boolean predXternal = predicate.dt() == XTERNAL;
                                            boolean xternal = dt == XTERNAL;
                                            boolean bothXternal = xternal && predXternal;
                                            if (!xternal && !predXternal || bothXternal || predXternal) {

                                                //not when only inner xternal, since that transformation destroys temporal information

                                                Term inner = predicate.sub(0);
                                                Term newPred = predicate.sub(1);
                                                Term newSubj = null;
                                                int newDT = 0;
                                                if (bothXternal) {
                                                    newSubj = B.conj(XTERNAL, subject, inner);
                                                    newDT = XTERNAL;
                                                } else {
                                                    newSubj = B.conjAppend(subject, dt, inner);
                                                    if (newSubj == Null) {
                                                        finished = true;
                                                        break;
                                                    }
                                                    newDT = predicate.dt();
                                                    if (newDT == DTERNAL) newDT = 0; //HACK temporary
                                                }

                                                if (newPred instanceof Neg) {
                                                    newPred = newPred.unneg();
                                                    negate = !negate;
                                                }
                                                if (dt != newDT || !newSubj.equals(subject) || !newPred.equals(predicate)) {
                                                    result = statement(B, IMPL, newDT, newSubj, newPred, depth - 1).negIf(negate);
                                                    finished = true;
                                                    break;//recurse
                                                }
                                            }
                                            break;
                                        }

                                    }
                                    if (!finished) {
                                        if (!predicate.op().eventable) {
                                            finished = true;
                                        }
                                    }

                                }
                                if (!finished) {
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

                                            boolean scomp = subject instanceof Compound;
                                            boolean pcomp = predicate instanceof Compound;
                                            if (!scomp && !pcomp) {
                                                //no validity test necessary
                                            } else if (subject.dt() == XTERNAL || predicate.dt() == XTERNAL) { // && !subject.OR(x->x instanceof Ellipsis) && !predicate.OR(x->x instanceof Ellipsis) ) {
                                                //do not reduce
                                            } else if (subject.unneg().opID() != CONJ.id && predicate.unneg().opID() != CONJ.id) {
                                                //both simple terms
                                            } else if (!Term.commonStructure(subject.structure() & (~CONJ.bit), predicate.structure() & (~CONJ.bit))) {
                                                //no validity test necessary
//                    } else if (subject instanceof Compound && !(predicate instanceof Compound) && !subject.containsRecursively(predicate)) {
//                        //no validity test necessary
//                    } else if (predicate instanceof Compound && !(subject instanceof Compound) && !predicate.containsRecursively(subject)) {
//                        //no validity test necessary
                                            } else if (scomp && Ellipsis.firstEllipsis(((Compound) subject).subtermsDirect()) != null) {

                                            } else if (pcomp && Ellipsis.firstEllipsis(((Compound) predicate).subtermsDirect()) != null) {
                                                //do not reduce
                                            } else if (op == IMPL) {
                                                int subjRange = subject.eventRange();
                                                long po = subjRange + dt; //predicate occurrence

                                                //subtract any common subject components from predicate
                                                ConjList newPredConj = ConjList.events(predicate, po);
                                                int removed = newPredConj.removeAll(subject.unneg(), 0, !(subject instanceof Neg));
                                                Term newPred = null;
                                                switch (removed) {
                                                    case -1:
                                                        result = False.negIf(negate);
                                                        finished = true;
                                                        break;
                                                    case +1:
                                                        newPred = newPredConj.term(B);
                                                        break;
                                                    default:
                                                        newPred = null;
                                                        break;
                                                }
                                                if (!finished) {
                                                    if (newPred != null && !predicate.equals(newPred)) {

                                                        if (newPred instanceof Bool) {
                                                            result = newPred.negIf(negate);
                                                            finished = true;//collapse
                                                        } else {
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
                                                                result = statement(B, IMPL, dt, subject, newPred, depth - 1).negIf(negate);
                                                                finished = true;//recurse
                                                            }
                                                        }


                                                    }
                                                }

                                            } else if (subject.dt() == DTERNAL && predicate.dt() == DTERNAL) { //TODO test earlier
                                                //INH,SIM
                                                //TODO swap order for optimal comparison
                                                boolean sConj = subject instanceof Compound && subject.opID() == CONJ.id;
                                                boolean pConj = predicate instanceof Compound && predicate.opID() == CONJ.id;
                                                if (sConj && pConj) {
                                                    Subterms ssub = subject.subterms();
                                                    Subterms psub = predicate.subterms();
                                                    Term[] common = ssub.subsIncluding(psub::contains);
                                                    if (common != null && common.length > 0) {
                                                        int cn = common.length;
                                                        if (cn == ssub.subs() || cn == psub.subs()) {
                                                            //contained entirely by the other
                                                            //True; //TODO negate
                                                            finished = true;
                                                        } else {
                                                            Predicate<Term> notCommon = common.length > 1 ? z -> ArrayUtil.indexOf(common, z) == -1 : z -> !common[0].equals(z);
                                                            subject = CONJ.the(ssub.subsIncluding(notCommon));
                                                            predicate = CONJ.the(psub.subsIncluding(notCommon));
                                                            result = statement(B, op, dt, subject, predicate, depth - 1);
                                                            finished = true;
                                                        }
                                                    }
                                                } else if (sConj) {
                                                    Subterms ssub = subject.subterms();
//                                if (ssub.contains(predicate)) return True; //TODO negate
//                                if (ssub.containsNeg(predicate)) return False; //TODO negate
                                                    if (ssub.containsPosOrNeg(predicate)) {
                                                        finished = true;
                                                    }
                                                } else if (pConj) {
                                                    Subterms psub = predicate.subterms();
                                                    //if (psub.contains(subject)) return True; //TODO negate
                                                    //if (psub.containsNeg(subject)) return False; //TODO negate
                                                    if (psub.containsPosOrNeg(subject)) {
                                                        finished = true;
                                                    }
                                                }
                                            }

                                        }

                                    }
                                    if (!finished) {
                                        assert (op != IMPL || dt != DTERNAL); //HACK dt should ==0
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
                                        if (!recursive(op, subject, predicate)) {
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
                                                if (inhCollapsed instanceof Bool) {
                                                    result = inhCollapsed;
                                                    finished = true;
                                                }
                                            }
                                            if (!finished) {
                                                if (op == IMPL) {
                                                    if (dt == 0)
                                                        dt = DTERNAL; //HACK generalize to DTERNAL ==>
                                                }
                                                Term t = B.newCompound(op, dt, subject, predicate);
                                                result = t.negIf(negate);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        return result;
    }

    private static boolean recursive(Op op, Term subject, Term predicate) {

        return op != IMPL && (Terms.eqRCom(subject, predicate));

        //|| (dt == 0) /* allow parallel IMPL unless there is a sequence that could separate the events from overlap */
        //|| (dt == 0 && !Conj.isSeq(subject) && !Conj.isSeq(predicate)
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


    }

}
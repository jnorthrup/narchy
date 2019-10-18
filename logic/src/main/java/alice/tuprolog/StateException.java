package alice.tuprolog;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Matteo Iuliani
 */
public class StateException extends State {

    public static final State the = new StateException();

    final Term catchTerm = Term.term("catch(Goal, Catcher, Handler)");
    final Term javaCatchTerm = Term.term("java_catch(Goal, List, Finally)");

    private StateException() {
        stateName = "Exception";
    }

    @Override
    State run(PrologSolve e) {
        String errorType = e.currentContext.currentGoal.name();
        if ("throw".equals(errorType))
            prologError(e);
        else
            javaException(e);
        return null;
    }

    private void prologError(PrologSolve e) {
        Term errorTerm = e.currentContext.currentGoal.sub(0);
        e.currentContext = e.currentContext.fatherCtx;
        if (e.currentContext == null) {
            
            
            e.nextState = PrologRun.END_HALT;
            return;
        }

        PrologRun c = e.run;
        while (true) {
            
            
            
            if (e.currentContext.currentGoal.unifiable(catchTerm)
                    && e.currentContext.currentGoal.sub(1).unifiable(errorTerm)) {
                
                

                
                c.cut();



                Collection<Var> unifiedVars = e.currentContext.trailingVars.head;
                e.currentContext.currentGoal.sub(1).unify(unifiedVars,
                        unifiedVars, errorTerm);

                
                
                
                
                
                
                Term handlerTerm = e.currentContext.currentGoal.sub(2);
                Term curHandlerTerm = handlerTerm.term();
                if (!(curHandlerTerm instanceof Struct)) {
                    e.nextState = PrologRun.END_FALSE;
                    return;
                }
                
                
                
                
                
                if (handlerTerm != curHandlerTerm)
                    handlerTerm = new Struct("call", curHandlerTerm);
                Struct handler = (Struct) handlerTerm;
                c.identify(handler);
                SubGoalTree sgt = new SubGoalTree();
                sgt.add(handler);
                c.pushSubGoal(sgt);
                e.currentContext.currentGoal = handler;

                
                e.nextState = PrologRun.GOAL_SELECTION;
                return;
            } else {
                
                e.currentContext = e.currentContext.fatherCtx;
                if (e.currentContext == null) {
                    
                    
                    e.nextState = PrologRun.END_HALT;
                    return;
                }
            }
        }
    }

    private void javaException(PrologSolve e) {
        PrologRun c = e.run;
        Struct cg = e.currentContext.currentGoal;
        Term exceptionTerm = cg.subs() > 0 ? cg.sub(0) : null;
        e.currentContext = e.currentContext.fatherCtx;
        if (e.currentContext == null) {
            
            
            e.nextState = PrologRun.END_HALT;
            return;
        }
        while (true) {
            
            
            
            if (e.currentContext.currentGoal.unifiable(javaCatchTerm)
                    && javaMatch(e.currentContext.currentGoal.sub(1),
                            exceptionTerm)) {
                
                

                
                c.cut();



                Collection<Var> unifiedVars = e.currentContext.trailingVars.head;
                Term handlerTerm = javaUnify(e.currentContext.currentGoal
                        .sub(1), exceptionTerm, unifiedVars);
                if (handlerTerm == null) {
                    e.nextState = PrologRun.END_FALSE;
                    return;
                }

                
                
                
                
                
                Term curHandlerTerm = handlerTerm.term();
                if (!(curHandlerTerm instanceof Struct)) {
                    e.nextState = PrologRun.END_FALSE;
                    return;
                }
                Term finallyTerm = e.currentContext.currentGoal.sub(2);
                Term curFinallyTerm = finallyTerm.term();
                
                boolean isFinally = true;
                if (curFinallyTerm instanceof NumberTerm.Int) {
                    NumberTerm.Int finallyInt = (NumberTerm.Int) curFinallyTerm;
                    if (finallyInt.intValue() == 0)
                        isFinally = false;
                    else {
                        
                        e.nextState = PrologRun.END_FALSE;
                        return;
                    }
                } else if (!(curFinallyTerm instanceof Struct)) {
                    e.nextState = PrologRun.END_FALSE;
                    return;
                }
                
                
                
                
                
                if (handlerTerm != curHandlerTerm)
                    handlerTerm = new Struct("call", curHandlerTerm);
                if (finallyTerm != curFinallyTerm)
                    finallyTerm = new Struct("call", curFinallyTerm);

                Struct handler = (Struct) handlerTerm;
                c.identify(handler);
                SubGoalTree sgt = new SubGoalTree();
                sgt.add(handler);
                if (isFinally) {
                    Struct finallyStruct = (Struct) finallyTerm;
                    c.identify(finallyStruct);
                    sgt.add(finallyStruct);
                }
                c.pushSubGoal(sgt);
                e.currentContext.currentGoal = handler;

                
                e.nextState = PrologRun.GOAL_SELECTION;
                return;

            } else {
                
                e.currentContext = e.currentContext.fatherCtx;
                if (e.currentContext == null) {
                    
                    
                    e.nextState = PrologRun.END_HALT;
                    return;
                }
            }
        }
    }

    
    
    private static boolean javaMatch(Term arg1, Term exceptionTerm) {
        if (!arg1.isList())
            return false;
        Struct list = (Struct) arg1;
        if (list.isEmptyList())
            return false;
        Iterator<? extends Term> it = list.listIterator();
        while (it.hasNext()) {
            Term nextTerm = it.next();
            if (!nextTerm.isCompound())
                continue;
            Struct element = (Struct) nextTerm;
            if (!",".equals(element.name()))
                continue;
            if (element.subs() != 2)
                continue;
            if (element.sub(0).unifiable(exceptionTerm)) {
                return true;
            }
        }
        return false;
    }

    
    
    private static Term javaUnify(Term arg1, Term exceptionTerm, Collection<Var> unifiedVars) {
        Struct list = (Struct) arg1;
        Iterator<? extends Term> it = list.listIterator();
        while (it.hasNext()) {
            Term nextTerm = it.next();
            if (!nextTerm.isCompound())
                continue;
            Struct element = (Struct) nextTerm;
            if (!",".equals(element.name()))
                continue;
            if (element.subs() != 2)
                continue;
            if (element.sub(0).unifiable(exceptionTerm)) {
                element.sub(0)
                        .unify(unifiedVars, unifiedVars, exceptionTerm);
                return element.sub(1);
            }
        }
        return null;
    }
}
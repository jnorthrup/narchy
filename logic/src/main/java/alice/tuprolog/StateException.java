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
        var errorType = e.currentContext.currentGoal.name();
        if ("throw".equals(errorType))
            prologError(e);
        else
            javaException(e);
        return null;
    }

    private void prologError(PrologSolve e) {
        var errorTerm = e.currentContext.currentGoal.sub(0);
        e.currentContext = e.currentContext.fatherCtx;
        if (e.currentContext == null) {
            
            
            e.nextState = PrologRun.END_HALT;
            return;
        }

        var c = e.run;
        while (true) {
            
            
            
            if (e.currentContext.currentGoal.unifiable(catchTerm)
                    && e.currentContext.currentGoal.sub(1).unifiable(errorTerm)) {
                
                

                
                c.cut();


                var unifiedVars = e.currentContext.trailingVars.head;
                e.currentContext.currentGoal.sub(1).unify(unifiedVars,
                        unifiedVars, errorTerm);


                var handlerTerm = e.currentContext.currentGoal.sub(2);
                var curHandlerTerm = handlerTerm.term();
                if (!(curHandlerTerm instanceof Struct)) {
                    e.nextState = PrologRun.END_FALSE;
                    return;
                }
                
                
                
                
                
                if (handlerTerm != curHandlerTerm)
                    handlerTerm = new Struct("call", curHandlerTerm);
                var handler = (Struct) handlerTerm;
                c.identify(handler);
                var sgt = new SubGoalTree();
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
        var c = e.run;
        var cg = e.currentContext.currentGoal;
        var exceptionTerm = cg.subs() > 0 ? cg.sub(0) : null;
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


                var unifiedVars = e.currentContext.trailingVars.head;
                var handlerTerm = javaUnify(e.currentContext.currentGoal
                        .sub(1), exceptionTerm, unifiedVars);
                if (handlerTerm == null) {
                    e.nextState = PrologRun.END_FALSE;
                    return;
                }


                var curHandlerTerm = handlerTerm.term();
                if (!(curHandlerTerm instanceof Struct)) {
                    e.nextState = PrologRun.END_FALSE;
                    return;
                }
                var finallyTerm = e.currentContext.currentGoal.sub(2);
                var curFinallyTerm = finallyTerm.term();

                var isFinally = true;
                if (curFinallyTerm instanceof NumberTerm.Int) {
                    var finallyInt = (NumberTerm.Int) curFinallyTerm;
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

                var handler = (Struct) handlerTerm;
                c.identify(handler);
                var sgt = new SubGoalTree();
                sgt.add(handler);
                if (isFinally) {
                    var finallyStruct = (Struct) finallyTerm;
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
        var list = (Struct) arg1;
        if (list.isEmptyList())
            return false;
        Iterator<? extends Term> it = list.listIterator();
        while (it.hasNext()) {
            var nextTerm = it.next();
            if (!nextTerm.isCompound())
                continue;
            var element = (Struct) nextTerm;
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
        var list = (Struct) arg1;
        Iterator<? extends Term> it = list.listIterator();
        while (it.hasNext()) {
            var nextTerm = it.next();
            if (!nextTerm.isCompound())
                continue;
            var element = (Struct) nextTerm;
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
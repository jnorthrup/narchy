/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Alex Benini
 */
public class StateGoalEvaluation extends State {

    public static final State the = new StateGoalEvaluation();

    private StateGoalEvaluation() {
        stateName = "Eval";
    }

    /*
     * (non-Javadoc)
     *
     * @see alice.tuprolog.AbstractRunState#doJob()
     */
    @Override
    State run(PrologSolve e) {
        PrologRun c = e.run;
        State nextState;
        if (e.currentContext.currentGoal.isPrimitive()) {

            PrologPrim primitive = e.currentContext.currentGoal
                    .getPrimitive();
            try {
                nextState = primitive
                        .evalAsPredicate(e.currentContext.currentGoal) ?
                        PrologRun.GOAL_SELECTION
                        : PrologRun.BACKTRACK;
            } catch (HaltException he) {
                nextState = PrologRun.END_HALT;
            } catch (Throwable t) {

                if (t instanceof InvocationTargetException) {
                    t = t.getCause();
                }

                if (t instanceof PrologError) {

                    PrologError error = (PrologError) t;
                    
                    e.currentContext.currentGoal = new Struct("throw", error.getError());
                    e.run.prolog.exception(error);

                } else if (t instanceof JavaException) {

                    JavaException exception = (JavaException) t;

                    e.currentContext.currentGoal = new Struct("java_throw", exception.getException());
                    e.run.prolog.exception(exception); //exception.getException());

                    //System.err.println(((JavaException) t).getException());
                }
                
                nextState = PrologRun.EXCEPTION;
            }
            e.nDemoSteps++;
        } else {
            nextState = PrologRun.RULE_SELECTION;
        }

        return nextState;
    }

}
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

/**
 * @author Alex Benini
 */
public class StateGoalEvaluation extends State {

    public StateGoalEvaluation(EngineRunner c) {
        this.c = c;
        stateName = "Eval";
    }

    /*
     * (non-Javadoc)
     *
     * @see alice.tuprolog.AbstractRunState#doJob()
     */
    @Override
    void run(Engine e) {
        if (e.currentContext.currentGoal.isPrimitive()) {
            
            PrologPrimitive primitive = e.currentContext.currentGoal
                    .getPrimitive();
            try {
                e.nextState = (primitive
                        .evalAsPredicate(e.currentContext.currentGoal)) ?
                        c.GOAL_SELECTION
                        : c.BACKTRACK;
            } catch (HaltException he) {
                e.nextState = c.END_HALT;
            } catch (Throwable t) {

                if (t instanceof PrologError) {
                    
                    PrologError error = (PrologError) t;
                    
                    
                    e.currentContext.currentGoal = new Struct("throw", error.getError());
                    /*Castagna 06/2011*/
                    e.manager.exception(error);
					/**/
                } else if (t instanceof JavaException) {
                    
                    JavaException exception = (JavaException) t;

                    
                    
                    e.currentContext.currentGoal = new Struct("java_throw", exception.getException());
					/*Castagna 06/2011*/
                    e.manager.exception(exception); //exception.getException());
					/**/

                    
                    System.err.println(((JavaException) t).getException());
                }

                
                e.nextState = c.EXCEPTION;
            }
            
            e.nDemoSteps++;
        } else {
            e.nextState = c.RULE_SELECTION;
        }
    }

}
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
public class StateGoalSelection extends State {


    public static final StateGoalSelection the = new StateGoalSelection();

    private StateGoalSelection() {
        stateName = "Call";
    }


    /* (non-Javadoc)
     * @see alice.tuprolog.AbstractRunState#doJob()
     */
    @Override
    State run(PrologSolve e) {
        PrologRun c = e.run;
        Term curGoal = null;
        while (curGoal == null) {
            curGoal = e.currentContext.goalsToEval.fetch();
            if (curGoal == null) {

                if (e.currentContext.fatherCtx == null) {

                    return e.choicePointSelector.existChoicePoint() ? PrologRun.END_TRUE_CP : PrologRun.END_TRUE;
                }

                e.currentContext = e.currentContext.fatherCtx;

            } else {

                Term goal_app = curGoal.term();
                if (!(goal_app instanceof Struct))
                    return PrologRun.END_FALSE;

                if (curGoal != goal_app)
                    curGoal = new Struct("call", goal_app);

                e.currentContext.currentGoal = (Struct) curGoal;
                return PrologRun.GOAL_EVALUATION;
            }
        }

        return null;
    }

}
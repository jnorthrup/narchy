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

import alice.util.OneWayList;
import jcog.data.list.FasterList;

import java.util.Collection;
import java.util.List;

/**
 * @author Alex Benini
 */
public class StateRuleSelection extends State {


    public static StateRuleSelection the = new StateRuleSelection();


    private StateRuleSelection() {
        stateName = "Init";
    }

    /* (non-Javadoc)
     * @see alice.tuprolog.AbstractRunState#doJob()
     */
    @Override
    State run(PrologSolve e) {
        var c = e.run;

        /*----------------------------------------------------
         * Individuo compatibleGoals e
         * stabilisco se derivo da Backtracking.
         */
        var alternative = e.currentAlternative;
        e.currentAlternative = null;
        ClauseStore clauseStore;
        boolean fromBacktracking;
        ClauseInfo clause;
        if (alternative == null) {
            /* from normal evaluation */

            List<Var> varsList = new FasterList<>();
            e.currentContext.trailingVars = OneWayList.add(e.currentContext.trailingVars, varsList);

            var goal = e.currentContext.currentGoal;
            clauseStore = ClauseStore.match(goal, c.prolog.theories.find(goal), varsList);
            if (clauseStore == null) { //g.isEmpty() || (clauseStore = ClauseStore.build(goal, g, varsList))==null) {
                return PrologRun.BACKTRACK;
            }

            fromBacktracking = false;
            clause = clauseStore.fetchFirst();
        } else {
            clauseStore = alternative.compatibleGoals;
            clause = clauseStore.fetchNext(true, false);  assert(clause!=null);
            fromBacktracking = true;
        }

        /*-----------------------------------------------------
         * Scelgo una regola fra quelle potenzialmente compatibili.
         */


        /*-----------------------------------------------------
         * Build ExecutionContext and ChoicePointContext
         */
        var ec = new PrologContext(e.nDemoSteps++);
        ec.clause = clause.clause;


        clause.copyTo(ec.getId(), ec);



        if (alternative != null) {
            var choicePoint = alternative;
            var depth = alternative.executionContext.depth;
            ec.choicePointAfterCut = choicePoint.prevChoicePointContext;
            var currentGoal = choicePoint.executionContext.currentGoal;
            while (currentGoal.subs() == 2 && ";".equals(currentGoal.name())) {
                if (choicePoint.prevChoicePointContext != null) {
                    int distance;
                    while ((distance = depth - choicePoint.prevChoicePointContext.executionContext.depth) == 0 && choicePoint.prevChoicePointContext != null) {
                        ec.choicePointAfterCut = choicePoint.prevChoicePointContext.prevChoicePointContext;
                        choicePoint = choicePoint.prevChoicePointContext;
                    }
                    if (distance == 1 && choicePoint.prevChoicePointContext != null) {
                        var cppp = choicePoint.prevChoicePointContext;
                        ec.choicePointAfterCut = cppp.prevChoicePointContext;
                        currentGoal = cppp.executionContext.currentGoal;
                        choicePoint = cppp;
                    } else
                        break;
                } else
                    break;
            }
        } else {
            ec.choicePointAfterCut = e.choicePointSelector.getPointer();
        }

        var curCtx = e.currentContext;
        var curGoal = curCtx.currentGoal;
        var unifiedVars = e.currentContext.trailingVars.head;
        curGoal.unify(unifiedVars, unifiedVars, ec.headClause);


        if ((ec.haveAlternatives = clauseStore.haveAlternatives()) && !fromBacktracking) {
            var cpc = new ChoicePointContext();
            cpc.compatibleGoals = clauseStore;
            cpc.executionContext = curCtx;
            cpc.indexSubGoal = curCtx.goalsToEval.current();
            cpc.varsToDeunify = e.currentContext.trailingVars;
            e.choicePointSelector.add(cpc);
        }

        if (fromBacktracking && !ec.haveAlternatives) {
            e.choicePointSelector.removeUnusedChoicePoints();
        }

        ec.tailCallOptimize(e);
        ec.saveParentState();
        e.currentContext = ec;
        return PrologRun.GOAL_SELECTION;
    }

}
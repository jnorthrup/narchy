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

import java.util.Deque;
import java.util.List;

/**
 * @author Alex Benini
 *
 */
public class StateRuleSelection extends State {
    
    
    
    public StateRuleSelection(EngineRunner c) {
        this.c = c;
        stateName = "Init";
    }
    
    /* (non-Javadoc)
     * @see alice.tuprolog.AbstractRunState#doJob()
     */
    @Override
    void run(Engine e) {
        /*----------------------------------------------------
         * Individuo compatibleGoals e
         * stabilisco se derivo da Backtracking.
         */
        Struct goal = e.currentContext.currentGoal;
        ChoicePointContext alternative = e.currentAlternative;
        e.currentAlternative = null;
        ClauseStore clauseStore;
        boolean fromBacktracking = true;
        if (alternative == null) {
            /* from normal evaluation */
            fromBacktracking = false;
            

            List<Var> varsList = new FasterList<>();
            e.currentContext.trailingVars = new OneWayList<>(varsList, e.currentContext.trailingVars);

            Deque<ClauseInfo> g = c.find(goal);
            clauseStore = g!=null ? ClauseStore.build(goal, varsList, g) : null;

            if (clauseStore == null){
                e.nextState = c.BACKTRACK;
                return;
            }
        } else
            clauseStore = alternative.compatibleGoals;
        
        /*-----------------------------------------------------
         * Scelgo una regola fra quelle potenzialmente compatibili.
         */
        ClauseInfo clause = clauseStore.fetch();
        
        /*-----------------------------------------------------
         * Build ExecutionContext and ChoicePointContext
         */
        ExecutionContext ec = new ExecutionContext(e.nDemoSteps++);
        ExecutionContext curCtx = e.currentContext;
        ec.clause = clause.clause;

        
        clause.copyTo(ec.getId(), ec);


        
        
        
        ec.choicePointAfterCut = e.choicePointSelector.getPointer();
        if (alternative != null) {
            ChoicePointContext choicePoint = alternative;
            int depth = alternative.executionContext.depth;
            ec.choicePointAfterCut = choicePoint.prevChoicePointContext;
            Struct currentGoal = choicePoint.executionContext.currentGoal;
            while (currentGoal.subs() == 2 && currentGoal.name().equals(";")) {
                if (choicePoint.prevChoicePointContext != null) {
                    int distance = depth - choicePoint.prevChoicePointContext.executionContext.depth;
                    while (distance == 0 && choicePoint.prevChoicePointContext != null) {
                        ec.choicePointAfterCut = choicePoint.prevChoicePointContext.prevChoicePointContext;
                        choicePoint = choicePoint.prevChoicePointContext;
                    }
                    if (distance == 1 && choicePoint.prevChoicePointContext != null) {
                        ec.choicePointAfterCut = choicePoint.prevChoicePointContext.prevChoicePointContext;
                        currentGoal = choicePoint.prevChoicePointContext.executionContext.currentGoal;
                        choicePoint = choicePoint.prevChoicePointContext;
                    } else
                        break;
                } else
                    break;
            }
        }
            
        Struct curGoal = curCtx.currentGoal;
        List<Var> unifiedVars = e.currentContext.trailingVars.head;
        curGoal.unify(unifiedVars,unifiedVars,ec.headClause);
        
        ec.haveAlternatives = clauseStore.haveAlternatives();
        
        
        if (ec.haveAlternatives && !fromBacktracking) {
            ChoicePointContext cpc = new ChoicePointContext();
            cpc.compatibleGoals = clauseStore;

            cpc.executionContext = curCtx;
            cpc.indexSubGoal = curCtx.goalsToEval.getCurrentGoalId();
            cpc.varsToDeunify = e.currentContext.trailingVars;
            e.choicePointSelector.add(cpc);
        }
        
        if (!ec.haveAlternatives && fromBacktracking) {            
                    e.choicePointSelector.removeUnusedChoicePoints();
                }
        
        ec.performTailRecursionOptimization(e);
        
        ec.saveParentState();
        e.currentContext = ec;
        e.nextState = c.GOAL_SELECTION;
    }
    
}
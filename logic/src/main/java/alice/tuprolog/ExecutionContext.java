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

import java.util.List;


/**
 * @author Alex Benini
 */
public class ExecutionContext {

    private final int id;
    int depth;
    Struct currentGoal;
    ExecutionContext fatherCtx;
    SubGoal fatherGoalId;
    Struct clause;
    Struct headClause;
    SubGoalStore goalsToEval;
    OneWayList<List<Var>> trailingVars;
    OneWayList<List<Var>> fatherVarsList;
    ChoicePointContext choicePointAfterCut;
    boolean haveAlternatives;

    ExecutionContext(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }


    public String toString() {
        return "         id: " + id + '\n' + "     currentGoal:  " + currentGoal + '\n' + "     clause:       " + clause + '\n' + "     subGoalStore: " + goalsToEval + '\n' + "     trailingVars: " + trailingVars + '\n';
    }


    /*
     * Methods for spyListeners
     */

    public int getDepth() {
        return depth;
    }

    public Struct getCurrentGoal() {
        return currentGoal;
    }

//    public SubGoal getFatherGoalId() {
//        return fatherGoalId;
//    }

    public Struct getClause() {
        return clause;
    }

//    public Struct getHeadClause() {
//        return headClause;
//    }

    public SubGoalStore getSubGoalStore() {
        return goalsToEval;
    }

//    public List<List<Var>> getTrailingVars() {
//        OneWayList<List<Var>> t = trailingVars;
//        List<List<Var>> l = new FasterList<>();
//
//        while (t != null) {
//            l.add(t.head);
//            t = t.tail;
//        }
//        return l;
//    }

    /**
     * Save the state of the parent context to later bring the ExectutionContext
     * objects tree in a consistent state after a backtracking step.
     */
    void saveParentState() {
        if (fatherCtx != null) {
            fatherGoalId = fatherCtx.goalsToEval.getCurrentGoalId();
            fatherVarsList = fatherCtx.trailingVars;
        }
    }


    /**
     * If no open alternatives, no other term to execute and
     * current context doesn't contain as current goal a catch or java_catch predicate ->
     * current context no more needed ->
     * reused to execute g subgoal =>
     * got TAIL RECURSION OPTIMIZATION!
     */

    void performTailRecursionOptimization(Engine e) {

        ExecutionContext ctx = e.currentContext;
        if (!haveAlternatives) {
            SubGoalStore ge = ctx.goalsToEval;
            if (ge.getCurSGId() == null) {
                if (!ge.haveSubGoals()) {
                    String gn = ctx.currentGoal.name();
                    switch (gn) {
                        case "catch":
                        case "java_catch":
                            break; //continue below

                        default: {
                            fatherCtx = ctx.fatherCtx;
                            //position of the new context in the list
                            depth = ctx.depth;
                            return;

                        }
                    }
                }
            }
        }

        fatherCtx = ctx;
        depth = ctx.depth + 1;


    }
}
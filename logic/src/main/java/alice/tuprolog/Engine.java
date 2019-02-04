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


import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Alex Benini
 */
public class Engine {

	
	State  nextState;
	final Term   query;
	Struct startGoal;
	Collection<Var> goalVars;
	int    nDemoSteps;
	ExecutionContext currentContext; 
	
	ChoicePointContext currentAlternative;
	ChoicePointStore choicePointSelector;
	boolean mustStop;
	final EngineRunner manager;


	public Engine(EngineRunner manager, Term query) {
		this.manager = manager;        
		this.nextState = manager.INIT;
		this.query = query;
		this.mustStop = false;
	}


	public String toString() {
		try {
			return
					"ExecutionStack: \n"+currentContext+ '\n' +
					"ChoicePointStore: \n"+choicePointSelector+"\n\n";
		} catch(Exception ex) { return ""; }
	}

	void mustStop() {
		mustStop = true;
	}

	/**
	 * Core of engine. Finite State Machine
	 */
	StateEnd run() {

		State nextState = null;
		do {

			if (mustStop) {
				nextState = manager.END_FALSE;
				break;
			}

			State state = this.nextState;

			nextState = state.run(this);

			if (nextState == null)
				nextState = this.nextState; //load in case HALTed from outside the loop
			else
				this.nextState = nextState;

			manager.on(state, this);

		} while (!(nextState instanceof StateEnd));

		nextState.run(this);

		return (StateEnd)nextState;
	}


	/*
	 * Methods for spyListeners
	 */

	public Term getQuery() {
		return query;
	}

//	public int getNumDemoSteps() {
//		return nDemoSteps;
//	}

	public List<ExecutionContext> getExecutionStack() {
		ArrayList<ExecutionContext> l = new ArrayList<>();
		ExecutionContext t = currentContext;
		while (t != null) {
			l.add(t);
			t = t.fatherCtx;
		}
		return l;
	}



	void prepareGoal() {
		LinkedHashMap<Var,Var> goalVars = new LinkedHashMap<>();
		startGoal = (Struct)(query).copyGoal(goalVars,0);
		this.goalVars = goalVars.values();
	}

	
		
		

	void initialize(ExecutionContext eCtx) {
		currentContext = eCtx;
		choicePointSelector = new ChoicePointStore();
		nDemoSteps = 1;
		currentAlternative = null;
	}
	
	public String getNextStateName()
	{
		return nextState.stateName;
	}

}

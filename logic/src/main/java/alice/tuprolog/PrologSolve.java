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
public class PrologSolve {

	
	State  nextState;
	final Term   query;
	Struct startGoal;
	Collection<Var> goalVars;
	int    nDemoSteps;
	PrologContext currentContext;
	
	ChoicePointContext currentAlternative;
	ChoicePointStore choicePointSelector;
	boolean mustStop;
	final PrologRun run;


	public PrologSolve(PrologRun run, Term query) {
		this.run = run;
		this.nextState = PrologRun.INIT;
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

		State nextState;
		do {

			if (mustStop) {
				nextState = PrologRun.END_FALSE;
				break;
			}

			State state = this.nextState;

			nextState = state.run(this);

			if (nextState == null)
				nextState = this.nextState; //load in case HALTed from outside the loop
			else
				this.nextState = nextState;

			run.on(state, this);

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

	public List<PrologContext> getExecutionStack() {
		ArrayList<PrologContext> l = new ArrayList<>();
		PrologContext t = currentContext;
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

	
		
		

	void initialize(PrologContext eCtx) {
		currentContext = eCtx;
		choicePointSelector = new ChoicePointStore();
		nDemoSteps = 1;
		currentAlternative = null;
	}
	
	public String getNextStateName()
	{
		return nextState.stateName;
	}

	public static class ChoicePointStore {


		private ChoicePointContext pointer;

		public ChoicePointStore() {
			pointer = null;
		}

		public void add(ChoicePointContext cpc) {
			if (pointer != null) {
				cpc.prevChoicePointContext = pointer;
			}
			pointer = cpc;
		}

		public void cut(ChoicePointContext pointerAfterCut) {
			pointer = pointerAfterCut;
		}

		/**
		 * Return the correct choice-point
		 */
		public ChoicePointContext fetch() {
			return (existChoicePoint()) ? pointer : null;
		}

		/**
		 * Return the actual choice-point store
		 * @return
		 */
		public ChoicePointContext getPointer() {
			return pointer;
		}
















		/**
		 * Check if a choice point exists in the store.
		 * As a side effect, removes choice points which have been already used and are now empty.
		 * @return
		 */
		protected boolean existChoicePoint() {
			ChoicePointContext pointer = this.pointer;
			while (pointer!=null) {
				if (pointer.compatibleGoals.unifiesMore())
					return true;
				this.pointer = pointer = pointer.prevChoicePointContext;
			}
			return false;
		}


		/**
		 * Removes choice points which have been already used and are now empty.
		 */
		protected void removeUnusedChoicePoints() {

			existChoicePoint();
		}

		/**
		 * Cut at defined depth (toDepth)
		 */






		public String toString(){
			return pointer + "\n";
		}

		/*
		 * Methods for spyListeners
		 */

	//    public List<ChoicePointContext> getChoicePoints() {
	//        ArrayList<ChoicePointContext> l = new ArrayList<>();
	//        ChoicePointContext t = pointer;
	//        while (t != null) {
	//            l.addAt(t);
	//            t = t.prevChoicePointContext;
	//        }
	//        return l;
	//    }

	}
}

/*
 *
 *
 */
package alice.tuprolog;

import jcog.data.list.FasterList;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static alice.tuprolog.PrologPrim.PREDICATE;

/**
 * @author Alex Benini
 * <p>
 * Core engine
 */
public class PrologRun implements java.io.Serializable, Runnable {



    public static final int HALT = -1;
    public static final int FALSE = 0;
    public static final int TRUE = 1;
    public static final int TRUE_CP = 2;

    PrologSolve solve;

    Prolog prolog;

    private boolean relinkVar;
    private List<Term> bagOFres;
    private List<String> bagOFresString;
    private Term bagOFvarSet;
    private Term bagOfgoal;
    private Term bagOfBag;

    public final int id;


    private boolean solving;
    private Term query;
    private BooleanArrayList next;
    private int countNext;
    private Lock lockVar;
    private Condition cond;
    private final Object semaphore = new Object();

    /* Current environment */
    /* Last environment used */
    private PrologSolve last_env;
    /* Stack environments of nidicate solving */
    private final FasterList<PrologSolve> stackEnv = new FasterList<>();
    protected Solution sinfo;
    private String sinfoSetOf;


    static final State INIT = StateInit.the;
    static final State GOAL_EVALUATION = StateGoalEvaluation.the;
    static final State EXCEPTION = StateException.the;
    static final State RULE_SELECTION = StateRuleSelection.the;
    static final State GOAL_SELECTION = StateGoalSelection.the;
    static final State BACKTRACK = StateBacktrack.the;
    static final State END_FALSE = new StateEnd(FALSE);
    static final State END_TRUE = new StateEnd(TRUE);
    static final State END_TRUE_CP = new StateEnd(TRUE_CP);
    static final State END_HALT = new StateEnd(HALT);

    public PrologRun(int id) {
        this.id = id;
    }


    /**
     * Config this Manager
     */
    public PrologRun initialize(Prolog vm) {
        prolog = vm;
        solving = false;
        sinfo = null;
        next = new BooleanArrayList();
        countNext = 0;
        lockVar = new ReentrantLock();
        cond = lockVar.newCondition();
        return this;
    }

    void on(State action, PrologSolve env) {
        prolog.spy(action, env);
    }

    /**
     * Solves a query
     *
     * @param g the term representing the goal to be demonstrated
     * @return the result of the demonstration
     * @see Solution
     **/
    private void threadSolve() {
        sinfo = solve();
        solving = false;

        lockVar.lock();
        try {
            cond.signalAll();
        } finally {
            lockVar.unlock();
        }

        if (sinfo.hasOpenAlternatives()) {
            if (next.isEmpty() || !next.get(countNext)) {
                synchronized (semaphore) {
                    try {
                        semaphore.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Solution solve() {
        try {
            query.resolveTerm();

            prolog.libs.onSolveBegin(query);
            prolog.prims.identify(query, PREDICATE);


            freeze();

            StateEnd result = (solve = new PrologSolve(this, query)).run();

            defreeze();

            sinfo = new Solution(
                    query,
                    result.goal,
                    result.endState,
                    result.vars
            );
            if (this.sinfoSetOf != null)
                sinfo.setSetOfSolution(sinfoSetOf);
            if (!sinfo.hasOpenAlternatives())
                solveEnd();
            return sinfo;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Solution(query);
        }
    }

    /**
     * Gets next solution
     *
     * @return the result of the demonstration
     * @throws NoMoreSolutionException if no more solutions are present
     * @see Solution
     **/
    private void threadSolveNext() throws NoMoreSolutionException {
        solving = true;
        next.set(countNext, false);
        countNext++;

        sinfo = solveNext();

        solving = false;

        lockVar.lock();
        try {
            cond.signalAll();
        } finally {
            lockVar.unlock();
        }

        if (sinfo.hasOpenAlternatives()) {
            if (countNext > (next.size() - 1) || !next.get(countNext)) {
                try {
                    synchronized (semaphore) {
                        semaphore.wait();
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public Solution solveNext() throws NoMoreSolutionException {
        if (hasOpenAlternatives()) {
            refreeze();
            solve.nextState = BACKTRACK;
            StateEnd result = solve.run();
            defreeze();
            sinfo = new Solution(
                    solve.query,
                    result.goal,
                    result.endState,
                    result.vars
            );
            if (this.sinfoSetOf != null)
                sinfo.setSetOfSolution(sinfoSetOf);

            if (!sinfo.hasOpenAlternatives()) {
                solveEnd();
            }
            return sinfo;

        } else
            throw new NoMoreSolutionException();
    }


    /**
     * Halts current solve computation
     */
    public void solveHalt() {
        solve.mustStop();
        prolog.libs.onSolveHalt();
    }

    /**
     * Accepts current solution
     */
    public void solveEnd() {


        prolog.libs.onSolveEnd();
    }


    private void freeze() {
        if (solve == null)
            return;

        if (!stackEnv.isEmpty() && stackEnv.getLast() == solve)
            return;

        stackEnv.add(solve);
    }

    private void refreeze() {
        freeze();
        solve = last_env;
    }

    private void defreeze() {
        last_env = solve;
        PrologSolve last = stackEnv.poll();
        if (last!=null)
            solve = last;
    }


    void identify(Term t) {
        prolog.prims.identify(t, PREDICATE);
    }


    void pushSubGoal(SubGoalTree goals) {
        solve.currentContext.goalsToEval.pushSubGoal(goals);
    }


    void cut() {
        solve.choicePointSelector.cut(solve.currentContext.choicePointAfterCut);
    }


//    ExecutionContext getCurrentContext() {
//        return (env == null) ? null : env.currentContext;
//    }


    /**
     * Asks for the presence of open alternatives to be explored
     * in current demostration process.
     *
     * @return true if open alternatives are present
     */
    public boolean hasOpenAlternatives() {
        if (sinfo == null) return false;
        return sinfo.hasOpenAlternatives();
    }


    /**
     * Checks if the demonstration process was stopped by an halt command.
     *
     * @return true if the demonstration was stopped
     */
    boolean isHalted() {
        if (sinfo == null) return false;
        return sinfo.isHalted();
    }


    @Override
    public void run() {


        solving = true;

        if (sinfo == null) {
            threadSolve();
        }

        try {
            while (hasOpenAlternatives())
                if (next.get(countNext))
                    threadSolveNext();
        } catch (NoMoreSolutionException e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    public Solution getSolution() {
        return sinfo;
    }

    public void setGoal(Term goal) {
        this.query = goal;
    }

    public boolean nextSolution() {
        solving = true;
        next.add(true);

        synchronized (semaphore) {
            semaphore.notify();
        }
        return true;
    }

    public Solution read() {
        lockVar.lock();
        try {
            while (solving || sinfo == null)
                try {
                    cond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        } finally {
            lockVar.unlock();
        }

        return sinfo;
    }

    public void setSolving(boolean solved) {
        solving = solved;
    }


    public boolean getRelinkVar() {
        return this.relinkVar;
    }

    public void setRelinkVar(boolean b) {
        this.relinkVar = b;
    }

    public List<Term> getBagOFres() {
        return this.bagOFres;
    }

    public void setBagOFres(List<Term> l) {
        this.bagOFres = l;
    }

    public List<String> getBagOFresString() {
        return this.bagOFresString;
    }

    public void setBagOFresString(List<String> l) {
        this.bagOFresString = l;
    }

    public Term getBagOFvarSet() {
        return this.bagOFvarSet;
    }

    public void setBagOFvarSet(Term l) {
        this.bagOFvarSet = l;
    }

    public Term getBagOFgoal() {
        return this.bagOfgoal;
    }

    public void setBagOFgoal(Term l) {
        this.bagOfgoal = l;
    }

    public Term getBagOFBag() {
        return this.bagOfBag;
    }

    public void setBagOFBag(Term l) {
        this.bagOfBag = l;
    }


    public void setSetOfSolution(String s) {
        if (sinfo != null)
            sinfo.setSetOfSolution(s);
        this.sinfoSetOf = s;
    }

    public void clearSinfoSetOf() {
        this.sinfoSetOf = null;
    }

    public final Solution solve(Term query) {
        setGoal(query);
        return solve();
    }


}
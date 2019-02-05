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

import static alice.tuprolog.PrologPrimitive.PREDICATE;

/**
 * @author Alex Benini
 * <p>
 * Core engine
 */
public class PrologRun implements java.io.Serializable, Runnable {



    Prolog prolog;

    Theories theories;

    private PrologPrimitives prims;
    private Libraries libs;


    private boolean relinkVar;
    private List<Term> bagOFres;
    private List<String> bagOFresString;
    private Term bagOFvarSet;
    private Term bagOfgoal;
    private Term bagOfBag;

    public final int id;

    private boolean detached;
    private boolean solving;
    private Term query;
    private BooleanArrayList next;
    private int countNext;
    private Lock lockVar;
    private Condition cond;
    private final Object semaphore = new Object();

    /* Current environment */
    Engine env;
    /* Last environment used */
    private Engine last_env;
    /* Stack environments of nidicate solving */
    private final FasterList<Engine> stackEnv = new FasterList<>();
    protected Solution sinfo;
    private String sinfoSetOf;

    /**
     * States
     */
    final State INIT;
    final State GOAL_EVALUATION;
    final State EXCEPTION;
    final State RULE_SELECTION;
    final State GOAL_SELECTION;
    final State BACKTRACK;
    final State END_FALSE;
    final State END_TRUE;
    final State END_TRUE_CP;
    final State END_HALT;

    public static final int HALT = -1;
    public static final int FALSE = 0;
    public static final int TRUE = 1;
    public static final int TRUE_CP = 2;


    public PrologRun(int id) {
        /* Istanzio gli stati */
        INIT = new StateInit(this);
        GOAL_EVALUATION = new StateGoalEvaluation(this);
        EXCEPTION = new StateException(this);
        RULE_SELECTION = new StateRuleSelection(this);
        GOAL_SELECTION = new StateGoalSelection(this);
        BACKTRACK = new StateBacktrack(this);
        END_FALSE = new StateEnd(this, FALSE);
        END_TRUE = new StateEnd(this, TRUE);
        END_TRUE_CP = new StateEnd(this, TRUE_CP);
        END_HALT = new StateEnd(this, HALT);

        this.id = id;
    }


    /**
     * Config this Manager
     */
    public PrologRun initialize(Prolog vm) {
        prolog = vm;
        theories = vm.theories;
        prims = vm.prims;
        libs = vm.libs;

        detached = false;
        solving = false;
        sinfo = null;
        next = new BooleanArrayList();
        countNext = 0;
        lockVar = new ReentrantLock();
        cond = lockVar.newCondition();
        return this;
    }

    void on(State action, Engine env) {
        prolog.spy(action, env);
    }



    public void detach() {
        detached = true;
    }

    public boolean isDetached() {
        return detached;
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

            libs.onSolveBegin(query);
            prims.identify(query, PREDICATE);


            freeze();
            env = new Engine(this, query);
            StateEnd result = env.run();
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
            env.nextState = BACKTRACK;
            StateEnd result = env.run();
            defreeze();
            sinfo = new Solution(
                    env.query,
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
        env.mustStop();
        libs.onSolveHalt();
    }

    /**
     * Accepts current solution
     */
    public void solveEnd() {


        libs.onSolveEnd();
    }


    private void freeze() {
        if (env == null)
            return;

        if (!stackEnv.isEmpty() && stackEnv.getLast() == env)
            return;

        stackEnv.add(env);
    }

    private void refreeze() {
        freeze();
        env = last_env;
    }

    private void defreeze() {
        last_env = env;
        Engine last = stackEnv.poll();
        if (last!=null)
            env = last;
    }


    void identify(Term t) {
        prims.identify(t, PREDICATE);
    }


    void pushSubGoal(SubGoalTree goals) {
        env.currentContext.goalsToEval.pushSubGoal(goals);
    }


    void cut() {
        env.choicePointSelector.cut(env.currentContext.choicePointAfterCut);
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

    public final synchronized Solution solve(Term query) {
        setGoal(query);
        return solve();
    }


}
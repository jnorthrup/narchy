package alice.tuprolog;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class EngineManager implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final Prolog vm;


    private final EngineRunner root = new EngineRunner(0);

    private final AtomicInteger id = new AtomicInteger();

    public static final ThreadLocal<EngineRunner> threads = new ThreadLocal<>();

    private final ConcurrentHashMap<Integer, EngineRunner> runners
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, TermQueue> queues
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks
            = new ConcurrentHashMap<>();

    public EngineManager(Prolog vm) {
        this.vm = vm;
    }

    public void initialize() {
        root.initialize(vm);
        threads.set(root);
    }

    public boolean threadCreate(Term threadID, Term goal) {

        if (goal == null)
            return false;


        int id = this.id.incrementAndGet();

        if (goal instanceof Var)
            goal = goal.term();

        EngineRunner er = new EngineRunner(id);
        er.initialize(vm);

        if (!threadID.unify(vm, new NumberTerm.Int(id)))
            return false;

        er.setGoal(goal);


        runners.put(id, er);


        Thread t = new Thread(er);


        t.start();
        return true;
    }


    public Solution join(int id) {
        EngineRunner er = runner(id);
        if (er == null || er.isDetached()) return null;
        /*toSPY
         * System.out.println("Thread id "+runnerId()+" - prelevo la soluzione (join)");*/
        Solution solution = er.read();
        /*toSPY
         * System.out.println("Soluzione: "+solution);*/
        removeRunner(id);
        return solution;
    }

    public Solution read(int id) {
        EngineRunner er = runner(id);
        if (er == null || er.isDetached()) return null;
        /*toSPY
         * System.out.println("Thread id "+runnerId()+" - prelevo la soluzione (read) del thread di id: "+er.getId());
         */
        /*toSPY
         * System.out.println("Soluzione: "+solution);
         */
        return er.read();
    }

    public boolean hasNext(int id) {
        EngineRunner er = runner(id);
        return !(er == null || er.isDetached()) && er.hasOpenAlternatives();
    }

    public boolean nextSolution(int id) {
        EngineRunner er = runner(id);
        /*toSPY
         * System.out.println("Thread id "+runnerId()+" - next_solution: risveglio il thread di id: "+er.getId());
         */
        return !(er == null || er.isDetached()) && er.nextSolution();
    }

    public void detach(int id) {
        EngineRunner er = runner(id);
        if (er != null)
            er.detach();
    }

    public boolean sendMsg(int dest, Term msg) {
        EngineRunner er = runner(dest);
        if (er == null) return false;
        Term msgcopy = msg.copy(new LinkedHashMap<>(), 0);
        er.sendMsg(msgcopy);
        return true;
    }

    public boolean sendMsg(String name, Term msg) {
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        Term msgcopy = msg.copy(new LinkedHashMap<>(), 0);
        queue.store(msgcopy);
        return true;
    }

    public boolean getMsg(int id, Term msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.getMsg(msg);
    }

    public boolean getMsg(String name, Term msg) {
        EngineRunner er = runner();
        if (er == null) return false;
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.get(msg, vm, er);
    }

    public boolean waitMsg(int id, Term msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.waitMsg(msg);
    }

    public boolean waitMsg(String name, Term msg) {
        EngineRunner er = runner();
        if (er == null) return false;
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.wait(msg, vm, er);
    }

    public boolean peekMsg(int id, Term msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.peekMsg(msg);
    }

    public boolean peekMsg(String name, Term msg) {
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.peek(msg, vm);
    }

    public boolean removeMsg(int id, Term msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.removeMsg(msg);
    }

    public boolean removeMsg(String name, Term msg) {
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.remove(msg, vm);
    }

    private void removeRunner(int id) {
        runners.remove(id);
        
        
            
        
		/*int pid = er.getPid();
		synchronized (threads) {
			threads.setAt(null);
		}*/
    }

    void cut() {
        runner().cut();
    }

    ExecutionContext getCurrentContext() {
        return runner().getCurrentContext();
    }

    boolean hasOpenAlternatives() {
        return runner().hasOpenAlternatives();
    }

    boolean isHalted() {
        return runner().isHalted();
    }

    void pushSubGoal(SubGoalTree goals) {
        runner().pushSubGoal(goals);
    }


    public Solution solve(Term query) {
        this.clearSinfoSetOf();

        return root.solve(query);
    }

    public void solveEnd() {
        synchronized (root) {

            root.solveEnd();

            if (!runners.isEmpty()) {

                runners.values().removeIf(current -> {
                    current.solveEnd();
                    return true;
                });

                queues.clear();
                locks.clear();
                id.set(0);
            }
        }
    }

    public void solveHalt() {
        synchronized (root) {
            root.solveHalt();
            if (!runners.isEmpty()) {
                java.util.Enumeration<EngineRunner> ers = runners.elements();
                while (ers.hasMoreElements()) {
                    EngineRunner current = ers.nextElement();
                    current.solveHalt();
                }
            }
        }
    }


    public Solution solveNext() throws NoMoreSolutionException {
        synchronized (root) {
            return root.solveNext();
        }
    }


    /**
     * @return L'EngineRunner associato al thread di id specificato.
     */

    private EngineRunner runner(int id) {


        return runners.get(id);


    }

    public final EngineRunner runner() {

        return threads.get();

//        Integer id = threads.get();
//
//        return id != null ? runner(id) : root;

    }

    public int runnerId() {
        return runner().getId();
    }

    public boolean createQueue(String name) {

        queues.computeIfAbsent(name, (n) -> new TermQueue());

        return true;
    }

    public void destroyQueue(String name) {

        queues.remove(name);

    }

    public int queueSize(int id) {
        return runner(id).msgQSize();
    }

    public int queueSize(String name) {
        TermQueue q = queues.get(name);
        return q == null ? -1 : q.size();
    }

    public ReentrantLock createLock(String name) {
        return locks.computeIfAbsent(name, (n) -> new ReentrantLock());
    }

    public boolean destroyLock(String name) {
        return locks.remove(name)!=null;
    }

    public void mutexLock(String name) {
        //while (true) {
        ReentrantLock mutex = createLock(name);

        mutex.lock();
        /*toSPY
         * System.out.println("Thread id "+runnerId()+ " - mi sono impossessato del lock");
         */
    }


    public boolean mutexTryLock(String name) {
        ReentrantLock mutex = locks.get(name);
        return mutex != null && mutex.tryLock();
        /*toSPY
         * System.out.println("Thread id "+runnerId()+ " - provo ad impossessarmi del lock");
         */
    }

    public boolean mutexUnlock(String name) {
        ReentrantLock mutex = locks.get(name);
        if (mutex == null) return false;
        try {
            mutex.unlock();
            /*toSPY
             * System.out.println("Thread id "+runnerId()+ " - Ho liberato il lock");
             */
            return true;
        } catch (IllegalMonitorStateException e) {
            return false;
        }
    }

    public boolean isLocked(String name) {
        ReentrantLock mutex = locks.get(name);
        return mutex != null && mutex.isLocked();
    }

    public void unlockAll() {

        locks.forEach((k, mutex) -> {
            boolean unlocked = false;
            while (!unlocked) {
                try {
                    mutex.unlock();
                } catch (IllegalMonitorStateException e) {
                    unlocked = true;
                }
            }
        });
    }

    Engine getEnv() {
        return runner().env;
    }

    public void identify(Term t) {
        runner().identify(t);
    }

    public boolean getRelinkVar() {
        return this.runner().getRelinkVar();
    }

    public void setRelinkVar(boolean b) {
        this.runner().setRelinkVar(b);
    }

    public ArrayList<Term> getBagOFres() {
        return this.runner().getBagOFres();
    }

    public void setBagOFres(ArrayList<Term> l) {
        this.runner().setBagOFres(l);
    }

    public ArrayList<String> getBagOFresString() {
        return this.runner().getBagOFresString();
    }

    public void setBagOFresString(ArrayList<String> l) {
        this.runner().setBagOFresString(l);
    }

    public Term getBagOFvarSet() {
        return this.runner().getBagOFvarSet();
    }

    public void setBagOFvarSet(Term l) {
        this.runner().setBagOFvarSet(l);
    }

    public Term getBagOFgoal() {
        return this.runner().getBagOFgoal();
    }

    public void setBagOFgoal(Term l) {
        this.runner().setBagOFgoal(l);
    }

    public Term getBagOFbag() {
        return this.runner().getBagOFBag();
    }

    public void setBagOFbag(Term l) {
        this.runner().setBagOFBag(l);
    }

    public String getSetOfSolution() {
        return this.runner().getSetOfSolution();
    }

    public void setSetOfSolution(String s) {
        this.runner().setSetOfSolution(s);
    }

    public void clearSinfoSetOf() {
        this.runner().clearSinfoSetOf();
    }

    public void endFalse(String s) {
        setSetOfSolution(s);
        setRelinkVar(false);
        setBagOFres(null);
        setBagOFgoal(null);
        setBagOFvarSet(null);
        setBagOFbag(null);
    }
}


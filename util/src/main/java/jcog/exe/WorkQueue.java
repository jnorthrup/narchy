package jcog.exe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;

/**
 * abstractions based on JDK's ForkJoinPool WorkQueue
 * UNTESTED
 *
 * Queues supporting work-stealing as well as external task
 * submission. See above for descriptions and algorithms.
 */
//@jdk.internal.vm.annotation.Contended
abstract class WorkQueue<X> {
    private static final short INITIAL_QUEUE_CAPACITY = 8192;
    static final int MAXIMUM_QUEUE_CAPACITY = 67108864;

    volatile int source;       // source queue id, or sentinel
    int id;                    // pool index, mode, tag
    int base;                  // index of next slot for poll
    int top;                   // index of next slot for push
    volatile int phase;        // versioned, negative: queued, 1: locked
    int stackPred;             // pool stack (ctl) predecessor link
    int nsteals;               // number of steals
    Object[] array;   // the queued tasks; power of 2 size
    final ForkJoinPool pool;   // the containing pool (may be null)
    final Thread owner; // owning thread or null if shared


    private static final VarHandle QA;
    static {
        QA = MethodHandles.arrayElementVarHandle(Object[].class);
    }

    WorkQueue(ForkJoinPool pool, Thread owner) {
        this.pool = pool;
        this.owner = owner;
        // Place indices in the center of array (that is not yet allocated)
        base = top = INITIAL_QUEUE_CAPACITY >>> 1;
    }

    /**
     * Tries to lock shared queue by CASing phase field.
     */
    final boolean tryLockPhase() {
        return PHASE.compareAndSet(this, 0, 1);
    }

    final void releasePhaseLock() {
        PHASE.setRelease(this, 0);
    }

    /**
     * Returns an exportable index (used by Thread).
     */
    final int getPoolIndex() {
        return (id & 0xffff) >>> 1; // ignore odd/even tag bit
    }

    /**
     * Returns the approximate number of tasks in the queue.
     */
    final int queueSize() {
        int n = (int)BASE.getAcquire(this) - top;
        return (n >= 0) ? 0 : -n; // ignore transient negative
    }

    /**
     * Provides a more accurate estimate of whether this queue has
     * any tasks than does queueSize, by checking whether a
     * near-empty queue has at least one unclaimed task.
     */
    final boolean isEmpty() {
        Object[] a; int n, cap, b;
        VarHandle.acquireFence(); // needed by external callers
        return ((n = (b = base) - top) >= 0 || // possibly one task
                (n == -1 && ((a = array) == null ||
                             (cap = a.length) == 0 ||
                             a[(cap - 1) & b] == null)));
    }

    /**
     * Pushes a task. Call only by owner in unshared queues.
     *
     * @param task the task. Caller must ensure non-null.
     * @throws RejectedExecutionException if array cannot be resized
     */
    final void push(X task) {
        Object[] a;
        int s = top, d = s - base, cap, m;
        if ((a = array) != null && (cap = a.length) > 0) {
            QA.setRelease(a, (m = cap - 1) & s, task);
            top = s + 1;
            if (d == m)
                growArray(false);
            else if (QA.getAcquire(a, m & (s - 1)) == null) {
                VarHandle.fullFence();  // was empty
                available(task);
            }
        }
    }

    abstract protected void available(X task); //p.signalWork(null);
    abstract protected void execute(X t); //t.doExec();
    abstract protected void afterWork(Thread thread); //thread.afterTopLevelExec();


    /**
     * Version of push for shared queues. Call only with phase lock held.
     * @return true if should signal work
     */
    final boolean lockedPush(X task) {
        Object[] a;
        boolean signal = false;
        int s = top, d = s - base, cap, m;
        if ((a = array) != null && (cap = a.length) > 0) {
            a[(m = (cap - 1)) & s] = task;
            top = s + 1;
            if (d == m)
                growArray(true);
            else {
                phase = 0; // full volatile unlock
                if (a[m & (s - 1)] == null)
                    signal = true;   // was empty
            }
        }
        return signal;
    }

    /**
     * Doubles the capacity of array. Call either by owner or with
     * lock held -- it is OK for base, but not top, to move while
     * resizings are in progress.
     */
    final void growArray(boolean locked) {
        Object[] newA = null;
        try {
            Object[] oldA; int oldSize, newSize;
            if ((oldA = array) != null && (oldSize = oldA.length) > 0 &&
                (newSize = oldSize << 1) <= MAXIMUM_QUEUE_CAPACITY &&
                newSize > 0) {
                try {
                    newA = new Object[newSize];
                } catch (OutOfMemoryError ex) {
                }
                if (newA != null) { // poll from old array, push to new
                    int oldMask = oldSize - 1, newMask = newSize - 1;
                    for (int s = top - 1, k = oldMask; k >= 0; --k) {
                        X x = (X)
                            QA.getAndSet(oldA, s & oldMask, null);
                        if (x != null)
                            newA[s-- & newMask] = x;
                        else
                            break;
                    }
                    array = newA;
                    VarHandle.releaseFence();
                }
            }
        } finally {
            if (locked)
                phase = 0;
        }
        if (newA == null)
            throw new RejectedExecutionException("Queue capacity exceeded");
    }

    /**
     * Takes next task, if one exists, in FIFO order.
     */
    final X poll() {
        int b, k, cap; Object[] a;
        while ((a = array) != null && (cap = a.length) > 0 &&
               top - (b = base) > 0) {
            X t = (X)
                QA.getAcquire(a, k = (cap - 1) & b);
            if (base == b++) {
                if (t == null)
                    Thread.yield(); // await index advance
                else if (QA.compareAndSet(a, k, t, null)) {
                    BASE.setOpaque(this, b);
                    return t;
                }
            }
        }
        return null;
    }
    static final int FIFO = 65536;

    /**
     * Takes next task, if one exists, in order specified by mode.
     */
    final X nextLocalTask() {
        X t = null;
        int md = id, b, s, d, cap; Object[] a;
        if ((a = array) != null && (cap = a.length) > 0 &&
            (d = (s = top) - (b = base)) > 0) {
            if ((md & FIFO) == 0 || d == 1) {
                if ((t = (X)
                     QA.getAndSet(a, (cap - 1) & --s, null)) != null)
                    TOP.setOpaque(this, s);
            }
            else if ((t = (X)
                      QA.getAndSet(a, (cap - 1) & b++, null)) != null) {
                BASE.setOpaque(this, b);
            }
            else // on contention in FIFO mode, use regular poll
                t = poll();
        }
        return t;
    }

    /**
     * Returns next task, if one exists, in order specified by mode.
     */
    final X peek() {
        int cap; Object[] a;
        return ((a = array) != null && (cap = a.length) > 0) ?
                (X) a[(cap - 1) & ((id & FIFO) != 0 ? base : top - 1)] : null;
    }

    /**
     * Pops the given task only if it is at the current top.
     */
    final boolean tryUnpush(X task) {
        boolean popped = false;
        int s, cap; Object[] a;
        if ((a = array) != null && (cap = a.length) > 0 &&
            (s = top) != base &&
            (popped = QA.compareAndSet(a, (cap - 1) & --s, task, null)))
            TOP.setOpaque(this, s);
        return popped;
    }

    /**
     * Shared version of tryUnpush.
     */
    final boolean tryLockedUnpush(X task) {
        boolean popped = false;
        int s = top - 1, k, cap; Object[] a;
        if ((a = array) != null && (cap = a.length) > 0 &&
            a[k = (cap - 1) & s] == task && tryLockPhase()) {
            if (top == s + 1 && array == a &&
                (popped = QA.compareAndSet(a, k, task, null)))
                top = s;
            releasePhaseLock();
        }
        return popped;
    }

    /**
     * Removes and cancels all known tasks, ignoring any exceptions.
     */
    public final void cancelAll() {
        for (X t; (t = poll()) != null; ) {
            cancel(t);
        }
    }

    protected abstract void cancel(X t); //X.cancelIgnoringExceptions(t); //t.cancel(true);

    // Specialized execution methods

    /**
     * Runs the given (stolen) task if nonnull, as well as
     * remaining local tasks and others available from the given
     * queue, up to bound n (to avoid infinite unfairness).
     */
    final void work(X t, WorkQueue q, int n) {
        int nstolen = 1;
        for (int j = 0;;) {
            if (t != null) {
                execute(t);
            } 
            if (j++ <= n)
                t = nextLocalTask();
            else {
                j = 0;
                t = null;
            }
            if (t == null) {
                if (q != null && (t = (X) q.poll()) != null) {
                    ++nstolen;
                    j = 0;
                }
                else if (j != 0)
                    break;
            }
        }
        Thread thread = owner;
        nsteals += nstolen;
        source = 0;
        if (thread != null) {
            afterWork(thread);

        }
    }



    /**
     * If present, removes task from queue and executes it.
     */
    final void tryRemoveAndExec(X task) {
        Object[] a; int s, cap;
        if ((a = array) != null && (cap = a.length) > 0 &&
            (s = top) - base > 0) { // traverse from top
            for (int m = cap - 1, ns = s - 1, i = ns; ; --i) {
                int index = i & m;
                X t = (X) QA.get(a, index);
                if (t == null)
                    break;
                else if (t == task) {
                    if (QA.compareAndSet(a, index, t, null)) {
                        top = ns;   // safely shift down
                        for (int j = i; j != ns; ++j) {
                            X f;
                            int pindex = (j + 1) & m;
                            f = (X) QA.get(a, pindex);
                            QA.setVolatile(a, pindex, null);
                            int jindex = j & m;
                            QA.setRelease(a, jindex, f);
                        }
                        VarHandle.releaseFence();
                        execute(t);
                    }
                    break;
                }
            }
        }
    }

//    /**
//     * Tries to pop and run tasks within the target's computation
//     * until done, not found, or limit exceeded.
//     *
//     * @param task root of CountedCompleter computation
//     * @param limit max runs, or zero for no limit
//     * @param shared true if must lock to extract task
//     * @return task status on exit
//     */
//    final int helpCC(CountedCompleter<?> task, int limit, boolean shared) {
//        int status = 0;
//        if (task != null && (status = task.status) >= 0) {
//            int s, k, cap; Object[] a;
//            while ((a = array) != null && (cap = a.length) > 0 &&
//                   (s = top) - base > 0) {
//                CountedCompleter<?> v = null;
//                Object o = a[k = (cap - 1) & (s - 1)];
//                if (o instanceof CountedCompleter) {
//                    CountedCompleter<?> t = (CountedCompleter<?>)o;
//                    for (CountedCompleter<?> f = t;;) {
//                        if (f != task) {
//                            if ((f = f.completer) == null)
//                                break;
//                        }
//                        else if (shared) {
//                            if (tryLockPhase()) {
//                                if (top == s && array == a &&
//                                    QA.compareAndSet(a, k, t, null)) {
//                                    top = s - 1;
//                                    v = t;
//                                }
//                                releasePhaseLock();
//                            }
//                            break;
//                        }
//                        else {
//                            if (QA.compareAndSet(a, k, t, null)) {
//                                top = s - 1;
//                                v = t;
//                            }
//                            break;
//                        }
//                    }
//                }
//                if (v != null)
//                    v.doExec();
//                if ((status = task.status) < 0 || v == null ||
//                    (limit != 0 && --limit == 0))
//                    break;
//            }
//        }
//        return status;
//    }

    /**
     * Tries to poll and run AsynchronousCompletionTasks until
     * none found or blocker is released
     *
     * @param blocker the blocker
     */
    final void helpAsyncBlocker(ForkJoinPool.ManagedBlocker blocker) {
        if (blocker != null) {
            int b, k, cap; Object[] a; X t;
            while ((a = array) != null && (cap = a.length) > 0 &&
                   top - (b = base) > 0) {
                t = (X) QA.getAcquire(a, k = (cap - 1) & b);
                if (blocker.isReleasable())
                    break;
                else if (base == b++ && t != null) {
                    if (!(t instanceof CompletableFuture.
                          AsynchronousCompletionTask))
                        break;
                    else if (QA.compareAndSet(a, k, t, null)) {
                        BASE.setOpaque(this, b);
                        execute(t);
                    }
                }
            }
        }
    }

    /**
     * Returns true if owned and not known to be blocked.
     */
    final boolean isApparentlyUnblocked() {
        Thread wt; Thread.State s;
        return ((wt = owner) != null &&
                (s = wt.getState()) != Thread.State.BLOCKED &&
                s != Thread.State.WAITING &&
                s != Thread.State.TIMED_WAITING);
    }

    // VarHandle mechanics.
    static final VarHandle PHASE;
    static final VarHandle BASE;
    static final VarHandle TOP;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            PHASE = l.findVarHandle(WorkQueue.class, "phase", int.class);
            BASE = l.findVarHandle(WorkQueue.class, "base", int.class);
            TOP = l.findVarHandle(WorkQueue.class, "top", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

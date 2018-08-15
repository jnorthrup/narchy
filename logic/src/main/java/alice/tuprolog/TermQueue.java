package alice.tuprolog;


import java.util.Iterator;
import java.util.LinkedList;

public class TermQueue {

    private final LinkedList<Term> queue = new LinkedList<>();

    public boolean get(Term t, Prolog engine, EngineRunner er) {
        synchronized (queue) {
            return searchLoop(t, engine, true, true, er);
        }
    }

    private boolean searchLoop(Term t, Prolog engine, boolean block, boolean remove, EngineRunner er) {
        synchronized (queue) {
            boolean found;
            do {
                found = search(t, engine, remove);
                if (found) return true;
                er.setSolving(false);
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    break;
                }
            } while (block);
            return false;
        }
    }


    private boolean search(Term t, Prolog engine, boolean remove) {
        synchronized (queue) {
            Iterator<Term> it = queue.iterator();
            while (it.hasNext()) {
                if (engine.unify(t, it.next())) {
                    
                    if (remove)
                        it.remove();
                    return true;
                }
            }
            return false;
        }
    }


    public boolean peek(Term t, Prolog engine) {
        synchronized (queue) {
            return search(t, engine, false);
        }
    }

    public boolean remove(Term t, Prolog engine) {
        synchronized (queue) {
            return search(t, engine, true);
        }
    }

    public boolean wait(Term t, Prolog engine, EngineRunner er) {
        synchronized (queue) {
            return searchLoop(t, engine, true, false, er);
        }
    }

    public void store(Term t) {
        synchronized (queue) {
            queue.addLast(t);
            queue.notifyAll();
        }
    }

    public int size() {
        synchronized (queue) {
            return queue.size();
        }
    }

    public void clear() {
        synchronized (queue) {
            queue.clear();
        }
    }
}

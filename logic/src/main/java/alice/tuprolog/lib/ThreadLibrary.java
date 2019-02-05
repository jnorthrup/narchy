/**
 * @author Eleonora Cau
 *
 */

package alice.tuprolog.lib;

import alice.tuprolog.*;

import java.util.LinkedHashMap;


public class ThreadLibrary extends Library {
	private static final long serialVersionUID = 1L;

	@Override
	public void setProlog(Prolog p) {
        prolog = p;
	}
	
	
	public boolean thread_id_1 (Term t) {
		int id = prolog.runner().getId();
        unify(t,new NumberTerm.Int(id));
		return true;
	}
	
	
	public boolean thread_create_2 (Term id, Term goal){
		return prolog.threadCreate(id, goal);
	}
	
	/*Aspetta la terminazione del thread di identificatore id e ne raccoglie il risultato, 
	unificando il goal risolto a result. Il thread viene eliminato dal sistema*/
	public boolean thread_join_2(Term id, Term result) throws PrologError{
		id = id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(prolog, 1,
                    "integer", id);
		Solution res = prolog.join(((NumberTerm.Int)id).intValue());
		if (res == null) return false;
		Term status;
		try {
			status = res.getSolution();
		} catch (NoSolutionException e) {
			
			return false;
		}
		try{
			unify (result, status);
		} catch (InvalidTermException e) {
			throw PrologError.syntax_error(prolog,-1, e.line, e.pos, result);
		}
		return true;
	}
		
	public boolean thread_read_2(Term id, Term result) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(prolog, 1,
                    "integer", id);
		Solution res= prolog.read( ((NumberTerm.Int)id).intValue());
		if (res==null) return false;
		Term status;
		try {
			status = res.getSolution();
		} catch (NoSolutionException e) {
			
			return false;
		}
		try{
			unify (result, status);
		} catch (InvalidTermException e) {
			throw PrologError.syntax_error(prolog,-1, e.line, e.pos, result);
		}
		return true;
	}
	
	public boolean thread_has_next_1(Term id) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(prolog, 1,
                    "integer", id);
		return prolog.hasNext(((NumberTerm.Int)id).intValue());
	}
	
	
	public boolean thread_next_sol_1(Term id) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(prolog, 1,
                    "integer", id);
		return prolog.nextSolution(((NumberTerm.Int)id).intValue());
	}
	
	public boolean thread_detach_1 (Term id) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(prolog, 1,
                    "integer", id);
		prolog.detach(((NumberTerm.Int)id).intValue());
		return true;
	}
	
	public boolean thread_sleep_1(Term millisecs) throws PrologError{
		millisecs=millisecs.term();
		if (!(millisecs instanceof NumberTerm.Int))
			throw PrologError.type_error(prolog, 1,
                    "integer", millisecs);
		long time=((NumberTerm.Int)millisecs).intValue();
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			System.out.println("ERRORE SLEEP");
			return false;
		}
		return true;
	}
	
	public boolean thread_send_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return sendMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtom() || !id.isAtomic())
			throw PrologError.type_error(prolog, 1,
                    "atom, atomic or integer", id);
		return sendMsg(id.toString(), msg);
	}
	
	public  boolean  thread_get_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return getMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom, atomic or integer", id);
		return getMsg(id.toString(), msg);
	}

	public  boolean  thread_peek_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return peekMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom, atomic or integer", id);
		return peekMsg(id.toString(), msg);
	}

	public  boolean  thread_wait_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return waitMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom, atomic or integer", id);
		return waitMsg(id.toString(), msg);
	}

	public  boolean  thread_remove_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return removeMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom, atomic or integer", id);
		return removeMsg(id.toString(), msg);
	}
	
	public boolean msg_queue_create_1(Term q) throws PrologError{
		q= q.term();
		if (!q.isAtom() || !q.isAtomic())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", q);
		return prolog.createQueue(q.toString());
	}
	
	public boolean msg_queue_destroy_1 (Term q) throws PrologError{
		q=q.term();
		if (!q.isAtom() || !q.isAtomic())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", q);
		prolog.destroyQueue(q.toString());
		return true;
	}
	
	public boolean msg_queue_size_2(Term id, Term n) throws PrologError{
		id=id.term();
		int size;
		if (id instanceof NumberTerm.Int)
			size= prolog.queueSize(((NumberTerm.Int)id).intValue());
		else{
			if (!id.isAtomic() || !id.isAtom())
				throw PrologError.type_error(prolog, 1,
	                    "atom, atomic or integer", id);
			size= prolog.queueSize(id.toString());
		}
		if (size<0) return false;
		return unify(n, new NumberTerm.Int(size));
	}	
	
	public boolean mutex_create_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", mutex);
		prolog.createLock(mutex.toString());
		return true;
	}
	
	public boolean mutex_destroy_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", mutex);
		return prolog.destroyLock(mutex.toString());
	}
	
	public boolean mutex_lock_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", mutex);
		prolog.mutexLock(mutex.toString());
		return true;
	}
	
	public boolean mutex_trylock_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", mutex);
		return prolog.mutexTryLock(mutex.toString());
	}
	
	public boolean mutex_unlock_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", mutex);
		return prolog.mutexUnlock(mutex.toString());
	}
	
	public boolean mutex_isLocked_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(prolog, 1,
                    "atom or atomic", mutex);
		return prolog.isLocked(mutex.toString());
	}
	
	public boolean mutex_unlock_all_0(){
		prolog.unlockAll();
		return true;
	}
	
	@Override
	public String getTheory(){
		return 
		"thread_execute(ID, GOAL):- thread_create(ID, GOAL), '$next'(ID). \n" +
		"'$next'(ID). \n"+
		"'$next'(ID) :- '$thread_execute2'(ID). \n"+
		"'$thread_execute2'(ID) :- not thread_has_next(ID),!,false. \n" +
		"'$thread_execute2'(ID) :- thread_next_sol(ID). \n" +
		"'$thread_execute2'(ID) :- '$thread_execute2'(ID). \n" +
	
		"with_mutex(MUTEX,GOAL):-mutex_lock(MUTEX), call(GOAL), !, mutex_unlock(MUTEX).\n" +
		"with_mutex(MUTEX,GOAL):-mutex_unlock(MUTEX), fail."		
		;
	
	}



	public boolean getMsg(String name, Term msg) {
		EngineRunner er = prolog.runner();
		if (er == null) return false;
		TermQueue queue = prolog.queues.get(name);
		if (queue == null) return false;
		return queue.get(msg, prolog, er);
	}
	public boolean sendMsg(int dest, Term msg) {
		EngineRunner er = prolog.runner(dest);
		if (er == null) return false;
		Term msgcopy = msg.copy(new LinkedHashMap<>(), 0);
		er.sendMsg(msgcopy);
		return true;
	}

	public boolean sendMsg(String name, Term msg) {
		TermQueue queue = prolog.queues.get(name);
		if (queue == null) return false;
		Term msgcopy = msg.copy(new LinkedHashMap<>(), 0);
		queue.store(msgcopy);
		return true;
	}

	public boolean getMsg(int id, Term msg) {
		EngineRunner er = prolog.runner(id);
		if (er == null) return false;
		return er.getMsg(msg);
	}


	public boolean waitMsg(int id, Term msg) {
		EngineRunner er = prolog.runner(id);
		if (er == null) return false;
		return er.waitMsg(msg);
	}

	public boolean waitMsg(String name, Term msg) {
		EngineRunner er = prolog.runner();
		if (er == null) return false;
		TermQueue queue = prolog.queues.get(name);
		if (queue == null) return false;
		return queue.wait(msg, prolog, er);
	}

	public boolean peekMsg(int id, Term msg) {
		EngineRunner er = prolog.runner(id);
		if (er == null) return false;
		return er.peekMsg(msg);
	}

	public boolean peekMsg(String name, Term msg) {
		TermQueue queue = prolog.queues.get(name);
		if (queue == null) return false;
		return queue.peek(msg, prolog);
	}

	public boolean removeMsg(String name, Term msg) {
		TermQueue queue = prolog.queues.get(name);
		if (queue == null) return false;
		return queue.remove(msg, prolog);
	}

	public boolean removeMsg(int id, Term msg) {
		EngineRunner er = prolog.runner(id);
		if (er == null) return false;
		return er.removeMsg(msg);
	}

}

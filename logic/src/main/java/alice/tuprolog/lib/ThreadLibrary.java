/**
 * @author Eleonora Cau
 *
 */

package alice.tuprolog.lib;

import alice.tuprolog.*;




public class ThreadLibrary extends Library {
	private static final long serialVersionUID = 1L;
	protected EngineManager engineManager;
	
	@Override
	public void setEngine(Prolog en) {
        engine = en;
		engineManager = en.engine;
	}
	
	
	public boolean thread_id_1 (Term t) {
        int id = engineManager.runnerId();
        unify(t,new NumberTerm.Int(id));
		return true;
	}
	
	
	public boolean thread_create_2 (Term id, Term goal){
		return engineManager.threadCreate(id, goal);
	}
	
	/*Aspetta la terminazione del thread di identificatore id e ne raccoglie il risultato, 
	unificando il goal risolto a result. Il thread viene eliminato dal sistema*/
	public boolean thread_join_2(Term id, Term result) throws PrologError{
		id = id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(engine.engine, 1,
                    "integer", id);
		Solution res = engineManager.join(((NumberTerm.Int)id).intValue());
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
			throw PrologError.syntax_error(engine.engine,-1, e.line, e.pos, result);
		}
		return true;
	}
		
	public boolean thread_read_2(Term id, Term result) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(engine.engine, 1,
                    "integer", id);
		Solution res=engineManager.read( ((NumberTerm.Int)id).intValue());
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
			throw PrologError.syntax_error(engine.engine,-1, e.line, e.pos, result);
		}
		return true;
	}
	
	public boolean thread_has_next_1(Term id) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(engine.engine, 1,
                    "integer", id);
		return engineManager.hasNext(((NumberTerm.Int)id).intValue());
	}
	
	
	public boolean thread_next_sol_1(Term id) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(engine.engine, 1,
                    "integer", id);
		return engineManager.nextSolution(((NumberTerm.Int)id).intValue());
	}
	
	public boolean thread_detach_1 (Term id) throws PrologError{
		id=id.term();
		if (!(id instanceof NumberTerm.Int))
			throw PrologError.type_error(engine.engine, 1,
                    "integer", id);
		engineManager.detach(((NumberTerm.Int)id).intValue());
		return true;
	}
	
	public boolean thread_sleep_1(Term millisecs) throws PrologError{
		millisecs=millisecs.term();
		if (!(millisecs instanceof NumberTerm.Int))
			throw PrologError.type_error(engine.engine, 1,
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
			return engineManager.sendMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtom() || !id.isAtomic())
			throw PrologError.type_error(engine.engine, 1,
                    "atom, atomic or integer", id);
		return engineManager.sendMsg(id.toString(), msg);
	}
	
	public  boolean  thread_get_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return engineManager.getMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom, atomic or integer", id);
		return engineManager.getMsg(id.toString(), msg);
	}	
	
	public  boolean  thread_peek_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return engineManager.peekMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom, atomic or integer", id);
		return engineManager.peekMsg(id.toString(), msg);
	}

	public  boolean  thread_wait_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return engineManager.waitMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom, atomic or integer", id);
		return engineManager.waitMsg(id.toString(), msg);
	}

	public  boolean  thread_remove_msg_2(Term id, Term msg) throws PrologError{
		id=id.term();
		if (id instanceof NumberTerm.Int)
			return engineManager.removeMsg(((NumberTerm.Int)id).intValue(), msg);
		if (!id.isAtomic() || !id.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom, atomic or integer", id);
		return engineManager.removeMsg(id.toString(), msg);
	}
	
	public boolean msg_queue_create_1(Term q) throws PrologError{
		q= q.term();
		if (!q.isAtom() || !q.isAtomic())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", q);
		return engineManager.createQueue(q.toString());
	}
	
	public boolean msg_queue_destroy_1 (Term q) throws PrologError{
		q=q.term();
		if (!q.isAtom() || !q.isAtomic())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", q);
		engineManager.destroyQueue(q.toString());
		return true;
	}
	
	public boolean msg_queue_size_2(Term id, Term n) throws PrologError{
		id=id.term();
		int size;
		if (id instanceof NumberTerm.Int)
			size=engineManager.queueSize(((NumberTerm.Int)id).intValue());
		else{
			if (!id.isAtomic() || !id.isAtom())
				throw PrologError.type_error(engine.engine, 1,
	                    "atom, atomic or integer", id);
			size=engineManager.queueSize(id.toString());
		}
		if (size<0) return false;
		return unify(n, new NumberTerm.Int(size));
	}	
	
	public boolean mutex_create_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", mutex);
		engineManager.createLock(mutex.toString());
		return true;
	}
	
	public boolean mutex_destroy_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", mutex);
		return engineManager.destroyLock(mutex.toString());
	}
	
	public boolean mutex_lock_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", mutex);
		engineManager.mutexLock(mutex.toString());
		return true;
	}
	
	public boolean mutex_trylock_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", mutex);
		return engineManager.mutexTryLock(mutex.toString());
	}
	
	public boolean mutex_unlock_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", mutex);
		return engineManager.mutexUnlock(mutex.toString());
	}
	
	public boolean mutex_isLocked_1(Term mutex) throws PrologError{
		mutex=mutex.term();
		if (!mutex.isAtomic() || !mutex.isAtom())
			throw PrologError.type_error(engine.engine, 1,
                    "atom or atomic", mutex);
		return engineManager.isLocked(mutex.toString());
	}
	
	public boolean mutex_unlock_all_0(){
		engineManager.unlockAll();
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
}

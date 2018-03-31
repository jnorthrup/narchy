package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Matteo Iuliani
 * 
 *         Test del funzionamento dei predicati java_throw/1 e java_catch/3
 */
public class JavaThrowCatchTestCase {

	// verifico che il gestore venga eseguito con le sostituzioni effettuate
	// durante il processo di unificazione tra l'eccezione e il catcher, e che
	// successivamente venga eseguito il finally
	@Test
	public void test_java_catch_3_1() throws Exception {
		Prolog engine = new Prolog();
		String goal = "atom_length(err, 3), java_catch(java_object('Counter', ['MyCounter'], c), [('java.lang.ClassNotFoundException'(Cause, Message, StackTrace), ((X is Cause+2, 5 is X+3)))], Y is 2+3), Z is X+5.";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		NumberTerm.Int cause = (NumberTerm.Int) info.getTerm("Cause");
		assertEquals(0, cause.intValue());
		Struct message = (Struct) info.getTerm("Message");
		assertTrue(message.isEqual(new Struct("Counter")));
		Struct stackTrace = (Struct) info.getTerm("StackTrace");
		assertTrue(stackTrace.isList());
		NumberTerm.Int x = (NumberTerm.Int) info.getTerm("X");
		assertEquals(2, x.intValue());
		NumberTerm.Int y = (NumberTerm.Int) info.getTerm("Y");
		assertEquals(5, y.intValue());
		NumberTerm.Int z = (NumberTerm.Int) info.getTerm("Z");
		assertEquals(7, z.intValue());
	}

	// verifico che venga eseguito il piu' vicino antenato java_catch/3
	// nell'albero di risoluzione che abbia un catcher unificabile con
	// l'argomento di java_throw/1
	@Test public void test_java_catch_3_2() throws Exception {
		Prolog engine = new Prolog();
		String goal = "java_catch(java_object('Counter', ['MyCounter'], c), [('java.lang.ClassNotFoundException'(Cause, Message, StackTrace), true)], true), java_catch(java_object('Counter', ['MyCounter2'], c2), [('java.lang.ClassNotFoundException'(C, M, ST), X is C+2)], true).";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		NumberTerm.Int cause = (NumberTerm.Int) info.getTerm("Cause");
		assertEquals(0, cause.intValue());
		Struct message = (Struct) info.getTerm("Message");
		assertTrue(message.isEqual(new Struct("Counter")));
		Struct stackTrace = (Struct) info.getTerm("StackTrace");
		assertTrue(stackTrace.isList());
		NumberTerm.Int x = (NumberTerm.Int) info.getTerm("X");
		assertEquals(2, x.intValue());
	}

	// verifico che l'esecuzione fallisce se si verifica un errore durante
	// l'esecuzione di un goal e non viene trovato nessun nodo java_catch/3
	// avente un catcher unificabile con l'argomento dell'eccezione lanciata
	@Test public void test_java_catch_3_3() throws Exception {
		Prolog engine = new Prolog();
		String goal = "java_catch(java_object('Counter', ['MyCounter'], c), [('java.lang.Exception'(Cause, Message, StackTrace), true)], true).";
		Solution info = engine.solve(goal);
		assertFalse(info.isSuccess());
		assertTrue(info.isHalted());
	}

	// verifico che catch/3 fallisce se il gestore e' falso
	@Test public void test_java_catch_3_4() throws Exception {
		Prolog engine = new Prolog();
		String goal = "java_catch(java_object('Counter', ['MyCounter'], c), [('java.lang.ClassNotFoundException'(Cause, Message, StackTrace), false)], true).";
		Solution info = engine.solve(goal);
		assertFalse(info.isSuccess());
	}

	// verifico che il finally venga eseguito in caso di successo di JavaGoal
	@Test public void test_java_catch_3_5() throws Exception {
		Prolog engine = new Prolog();
		String goal = "java_catch(java_object('java.util.ArrayList', [], l), [(E, true)], (X is 2+3, Y is 3+5)).";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		Term e = info.getTerm("E");
		assertTrue(e instanceof Var);
		NumberTerm.Int x = (NumberTerm.Int) info.getTerm("X");
		assertEquals(5, x.intValue());
		NumberTerm.Int y = (NumberTerm.Int) info.getTerm("Y");
		assertEquals(8, y.intValue());
	}

	// verifico che catch/3 fallisce se si verifica un'eccezione durante
	// l'esecuzione del gestore
	@Test public void test_java_catch_3_6() throws Exception {
		Prolog engine = new Prolog();
		String goal = "java_catch(java_object('Counter', ['MyCounter'], c), [('java.lang.ClassNotFoundException'(Cause, Message, StackTrace), java_object('Counter', ['MyCounter2'], c2))], true).";
		Solution info = engine.solve(goal);
		assertFalse(info.isSuccess());
		assertTrue(info.isHalted());
	}

	// verifico la correttezza della ricerca del catcher all'interno della lista
	@Test public void test_java_catch_3_7() throws Exception {
		Prolog engine = new Prolog();
		String goal = "java_catch(java_object('Counter', ['MyCounter'], c), [('java.lang.Exception'(Cause, Message, StackTrace), X is 2+3), ('java.lang.ClassNotFoundException'(Cause, Message, StackTrace), Y is 3+5)], true).";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		Term x = info.getTerm("X");
		assertTrue(x instanceof Var);
		Term y = info.getTerm("Y");
		assertTrue(y instanceof NumberTerm.Int);
		assertEquals(8, ((NumberTerm.Int) y).intValue());
	}

}
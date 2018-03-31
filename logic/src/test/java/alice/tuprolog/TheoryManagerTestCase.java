package alice.tuprolog;


import org.junit.jupiter.api.Test;

import java.util.Deque;

import static org.junit.jupiter.api.Assertions.*;

public class TheoryManagerTestCase {

//	@Test public void testUnknownDirective() throws InvalidTheoryException {
//		String theory = ":- unidentified_directive(unknown_argument).";
//		Prolog engine = new Prolog();
//		TestWarningListener warningListener = new TestWarningListener();
//		engine.addWarningListener(warningListener);
//		engine.setTheory(new Theory(theory));
//		assertTrue(warningListener.warning.indexOf("unidentified_directive/1") > 0);
//		assertTrue(warningListener.warning.indexOf("is unknown") > 0);
//	}
//
//	@Test public void testFailedDirective() throws InvalidTheoryException {
//		String theory = ":- load_library('UnknownLibrary').";
//		Prolog engine = new Prolog();
//		TestWarningListener warningListener = new TestWarningListener();
//		engine.addWarningListener(warningListener);
//		engine.setTheory(new Theory(theory));
//		assertTrue(warningListener.warning.indexOf("load_library/1") > 0);
//		assertTrue(warningListener.warning.indexOf("InvalidLibraryException") > 0);
//	}

	@Test
	public void testAssertNotBacktrackable() throws PrologException {
		Prolog engine = new Prolog();
		Solution firstSolution = engine.solve("assertz(a(z)).");
		assertTrue(firstSolution.isSuccess());
		assertFalse(firstSolution.hasOpenAlternatives());
	}

	@Test public void testAbolish() throws PrologException {
		Prolog engine = new Prolog();
		String theory = "test(A, B) :- A is 1+2, B is 2+3.";
		engine.setTheory(new Theory(theory));
		TheoryManager manager = engine.theories;
		Struct testTerm = new Struct("test", new Struct("a"), new Struct("b"));
		Deque<ClauseInfo> testClauses = manager.find(testTerm);
		assertEquals(1, testClauses.size());
		manager.abolish(new Struct("/", new Struct("test"), new NumberTerm.Int(2)));
		testClauses = manager.find(testTerm);
		// The predicate should also disappear completely from the clause
		// database, i.e. ClauseDatabase#get(f/a) should return null
		assertEquals(0, testClauses.size());
	}

	@Test public void testAbolish2() throws InvalidTheoryException, MalformedGoalException{
		Prolog engine = new Prolog();
		engine.setTheory(new Theory("fact(new).\n" +
									"fact(other).\n"));

		Solution info = engine.solve("abolish(fact/1).");
		assertTrue(info.isSuccess());
		info = engine.solve("fact(V).");
		assertFalse(info.isSuccess());
	}
	
	// Based on the bugs 65 and 66 on sourceforge
	@Test public void testRetractall() throws Exception {
		Prolog engine = new Prolog();
		Solution info = engine.solve("assert(takes(s1,c2)), assert(takes(s1,c3)).");
		assertTrue(info.isSuccess());
		info = engine.solve("takes(s1, N).");
		assertTrue(info.isSuccess());
		assertTrue(info.hasOpenAlternatives());
		assertEquals("c2", info.getVarValue("N").toString());
		info = engine.solveNext();
		assertTrue(info.isSuccess());
		assertEquals("c3", info.getVarValue("N").toString());

		info = engine.solve("retractall(takes(s1,c2)).");

		assertTrue(info.isSuccess());
		info = engine.solve("takes(s1, N).");
		assertTrue(info.isSuccess());
		if (info.hasOpenAlternatives())
			System.err.println(engine.solveNext());
//		assertFalse(info.hasOpenAlternatives());
		assertEquals("c2", info.getVarValue("N").toString());
	}

	// TODO test retractall: ClauseDatabase#get(f/a) should return an
	// empty list
	
	@Test public void testRetract() throws InvalidTheoryException, MalformedGoalException {
		Prolog engine = new Prolog();
		TestOutputListener listener = new TestOutputListener();
		engine.addOutputListener(listener);
		engine.setTheory(new Theory("insect(ant). insect(bee)."));
		Solution info = engine.solve("retract(insect(I)), write(I), retract(insect(bee)), fail.");
		assertFalse(info.isSuccess());
		assertEquals("antbee", listener.output);
		
	}

}

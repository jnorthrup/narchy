package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrologLibTestCase {
	
	@Test
	public void testLibraryFunctor() throws PrologException {
		Prolog engine = new Prolog();
		engine.addLibrary(new TestLibrary());
		Solution goal = engine.solve("N is sum(1, 3).");
		assertTrue(goal.isSuccess());
		assertEquals(new NumberTerm.Int(4), goal.getVarValue("N"));
	}
	
	@Test public void testLibraryPredicate() throws PrologException {
		Prolog engine = new Prolog();
		engine.addLibrary(new TestLibrary());
		TestOutputListener l = new TestOutputListener();
		engine.addOutputListener(l);
		engine.solve("println(sum(5)).");
		assertEquals("sum(5)", l.output);
	}

}

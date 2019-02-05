package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Matteo Iuliani
 * 
 *         Test del funzionamento delle eccezioni lanciate dai predicati della
 *         DCGLibrary
 */
public class DCGPrologLibExceptionsTestCase {

	
	@Test
	public void test_phrase_2_1() throws Exception {
		Prolog engine = new Prolog();
		engine.addLibrary("alice.tuprolog.lib.DCGLibrary");
		String goal = "catch(phrase(X, []), error(instantiation_error, instantiation_error(Goal, ArgNo)), true).";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		assertTrue(g.isEqual(new Struct("phrase_guard", new Var("X"),
                Struct.emptyList())));
		NumberTerm.Int argNo = (NumberTerm.Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
	}

	
	@Test public void test_phrase_3_1() throws Exception {
		Prolog engine = new Prolog();
		engine.addLibrary("alice.tuprolog.lib.DCGLibrary");
		String goal = "catch(phrase(X, [], []), error(instantiation_error, instantiation_error(Goal, ArgNo)), true).";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		assertTrue(g.isEqual(new Struct("phrase_guard", new Var("X"),
                Struct.emptyList(), Struct.emptyList())));
		NumberTerm.Int argNo = (NumberTerm.Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
	}

}
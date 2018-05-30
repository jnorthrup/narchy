package alice.tuprolog;

import alice.tuprolog.lib.IOLibrary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IOLibraryTestCase {
	
	@Test public void testGetPrimitives() {
		Library library = new IOLibrary();
		Map<Integer, List<PrologPrimitive>> primitives = library.primitives();
		assertEquals(3, primitives.size());
		assertEquals(0, primitives.get(PrologPrimitive.DIRECTIVE).size());
		assertTrue(primitives.get(PrologPrimitive.PREDICATE).size() > 0);
		assertEquals(0, primitives.get(PrologPrimitive.FUNCTOR).size());
	}
	
	@Test
	public void testTab1() throws MalformedGoalException {
		Prolog engine = new Prolog();
		TestOutputListener l = new TestOutputListener();
		engine.addOutputListener(l);
		engine.solve("tab(5).");
		assertEquals("     ", l.output);
	}

}

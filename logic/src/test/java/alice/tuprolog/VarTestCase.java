package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class VarTestCase {
	
	@Test
	public void testIsAtomic() {
		assertFalse(new Var("X").isAtom());
	}
	
	@Test public void testIsAtom() {
		assertFalse(new Var("X").isAtomic());
	}
	
	@Test public void testIsCompound() {
		assertFalse(new Var("X").isCompound());
	}

}

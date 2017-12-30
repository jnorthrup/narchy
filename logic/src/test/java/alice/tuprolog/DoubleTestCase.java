package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DoubleTestCase {
	
	@Test
	public void testIsAtomic() {
		assertTrue(new alice.tuprolog.Double(0).isAtom());
	}
	
	@Test public void testIsAtom() {
		assertFalse(new alice.tuprolog.Double(0).isAtomic());
	}
	
	@Test public void testIsCompound() {
		assertFalse(new alice.tuprolog.Double(0).isCompound());
	}
	
	@Test public void testEqualsToStruct() {
		alice.tuprolog.Double zero = new alice.tuprolog.Double(0);
		Struct s = Struct.emptyList();
		assertFalse(zero.equals(s));
	}
	
	@Test public void testEqualsToVar() throws InvalidTermException {
		alice.tuprolog.Double one = new alice.tuprolog.Double(1);
		Var x = new Var("X");
		assertFalse(one.equals(x));
	}
	
	@Test public void testEqualsToDouble() {
		alice.tuprolog.Double zero = new alice.tuprolog.Double(0);
		alice.tuprolog.Double one = new alice.tuprolog.Double(1);
		assertFalse(zero.equals(one));
		alice.tuprolog.Double anotherZero = new alice.tuprolog.Double(0.0);
        assertEquals(anotherZero, zero);
	}
	
	@Test public void testEqualsToFloat() {
		// TODO Test Double numbers for equality with Float numbers
	}
	
	@Test public void testEqualsToInt() {
		alice.tuprolog.Double doubleOne = new alice.tuprolog.Double(1.0);
		Int integerOne = new Int(1);
		assertFalse(doubleOne.equals(integerOne));
	}
	
	@Test public void testEqualsToLong() {
		// TODO Test Double numbers for equality with Long numbers
	}

}

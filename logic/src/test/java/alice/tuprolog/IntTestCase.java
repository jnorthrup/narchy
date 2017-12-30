package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntTestCase {
	
	@Test
	public void testIsAtomic() {
		assertTrue(new Int(0).isAtom());
	}
	
	@Test public void testIsAtom() {
		assertFalse(new Int(0).isAtomic());
	}
	
	@Test public void testIsCompound() {
		assertFalse(new Int(0).isCompound());
	}
	
	@Test public void testEqualsToStruct() {
		Struct s = Struct.emptyList();
		Int zero = new Int(0);
		assertFalse(zero.equals(s));
	}
	
	@Test public void testEqualsToVar() throws InvalidTermException {
		Var x = new Var("X");
		Int one = new Int(1);
		assertFalse(one.equals(x));
	}
	
	@Test public void testEqualsToInt() {
		Int zero = new Int(0);
		Int one = new Int(1);
		assertFalse(zero.equals(one));
		Int anotherZero = new Int(1-1);
		assertEquals(anotherZero, zero);
	}
	
	@Test public void testEqualsToLong() {
		// TODO Test Int numbers for equality with Long numbers
	}
	
	@Test public void testEqualsToDouble() {
		Int integerOne = new Int(1);
		alice.tuprolog.Double doubleOne = new alice.tuprolog.Double(1);
		assertFalse(integerOne.equals(doubleOne));
	}
	
	@Test public void testEqualsToFloat() {
		// TODO Test Int numbers for equality with Float numbers
	}

}

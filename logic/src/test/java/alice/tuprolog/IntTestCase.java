package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntTestCase {
	
	@Test
	public void testIsAtomic() {
		assertTrue(new NumberTerm.Int(0).isAtom());
	}
	
	@Test public void testIsAtom() {
		assertFalse(new NumberTerm.Int(0).isAtomic());
	}
	
	@Test public void testIsCompound() {
		assertFalse(new NumberTerm.Int(0).isCompound());
	}
	
	@Test public void testEqualsToStruct() {
		Struct s = Struct.emptyList();
		NumberTerm.Int zero = new NumberTerm.Int(0);
		assertFalse(zero.equals(s));
	}
	
	@Test public void testEqualsToVar() throws InvalidTermException {
		Var x = new Var("X");
		NumberTerm.Int one = new NumberTerm.Int(1);
		assertFalse(one.equals(x));
	}
	
	@Test public void testEqualsToInt() {
		NumberTerm.Int zero = new NumberTerm.Int(0);
		NumberTerm.Int one = new NumberTerm.Int(1);
		assertFalse(zero.equals(one));
		NumberTerm.Int anotherZero = new NumberTerm.Int(1-1);
		assertEquals(anotherZero, zero);
	}
	
	@Test public void testEqualsToLong() {
		// TODO Test Int numbers for equality with Long numbers
	}
	
	@Test public void testEqualsToDouble() {
		NumberTerm.Int integerOne = new NumberTerm.Int(1);
		NumberTerm.Double doubleOne = new NumberTerm.Double(1);
		assertFalse(integerOne.equals(doubleOne));
	}
	
	@Test public void testEqualsToFloat() {
		// TODO Test Int numbers for equality with Float numbers
	}

}

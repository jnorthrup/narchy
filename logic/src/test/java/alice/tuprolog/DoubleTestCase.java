package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DoubleTestCase {
	
	@Test
	public void testIsAtomic() {
		assertTrue(new NumberTerm.Double(0).isAtom());
	}
	
	@Test public void testIsAtom() {
		assertFalse(new NumberTerm.Double(0).isAtomic());
	}
	
	@Test public void testIsCompound() {
		assertFalse(new NumberTerm.Double(0).isCompound());
	}
	
	@Test public void testEqualsToStruct() {
		NumberTerm.Double zero = new NumberTerm.Double(0);
		Struct s = Struct.emptyList();
		assertFalse(zero.equals(s));
	}
	
	@Test public void testEqualsToVar() throws InvalidTermException {
		NumberTerm.Double one = new NumberTerm.Double(1);
		Var x = new Var("X");
		assertFalse(one.equals(x));
	}
	
	@Test public void testEqualsToDouble() {
		NumberTerm.Double zero = new NumberTerm.Double(0);
		NumberTerm.Double one = new NumberTerm.Double(1);
		assertFalse(zero.equals(one));
		NumberTerm.Double anotherZero = new NumberTerm.Double(0.0);
        assertEquals(anotherZero, zero);
	}
	
	@Test public void testEqualsToFloat() {
		
	}
	
	@Test public void testEqualsToInt() {
		NumberTerm.Double doubleOne = new NumberTerm.Double(1.0);
		NumberTerm.Int integerOne = new NumberTerm.Int(1);
		assertFalse(doubleOne.equals(integerOne));
	}
	
	@Test public void testEqualsToLong() {
		
	}

}

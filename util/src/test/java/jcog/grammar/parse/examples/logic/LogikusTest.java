package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.examples.engine.Atom;
import jcog.grammar.parse.examples.engine.Axiom;
import jcog.grammar.parse.examples.engine.Structure;
import jcog.grammar.parse.examples.engine.Term;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class LogikusTest {
	
	@Test
    void atom() {
		Axiom axiom = LogikusFacade.axiom("johannes");
		assertTrue(axiom.head() instanceof Atom);
		assertEquals("johannes", axiom.head().eval());
	}

	@Test
    void fact() {
		Axiom axiom = LogikusFacade.axiom("male(johannes)");
		Structure structure = axiom.head();
		assertEquals("male(johannes)", structure.toString());
	}

	@Test
    void list() {
		Axiom axiom = LogikusFacade.axiom("members([johannes, frank])");
		Structure structure = axiom.head();
		Term list = structure.terms()[0];
		assertEquals("[johannes, frank]", list.toString());
	}

	@Test
    void listWithTail() {
		Axiom axiom = LogikusFacade.axiom("members([johannes | [frank]])");
		Structure structure = axiom.head();
		Term list = structure.terms()[0];
		assertEquals("[johannes, frank]", list.toString());
	}
}

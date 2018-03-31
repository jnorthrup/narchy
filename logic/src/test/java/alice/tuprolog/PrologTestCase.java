package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrologTestCase  {
	
	@Test
	public void testEngineInitialization() {
		Prolog engine = new Prolog();
		//assertEquals(4, engine.getCurrentLibraries().length);
		assertNotNull(engine.library("alice.tuprolog.lib.BasicLibrary"));
		assertNotNull(engine.library("alice.tuprolog.lib.ISOLibrary"));
		assertNotNull(engine.library("alice.tuprolog.lib.IOLibrary"));
		assertNotNull(engine.library("alice.tuprolog.lib.OOLibrary"));
	}
	
	@Test public void testLoadLibraryAsString() throws InvalidLibraryException {
		Prolog engine = new Prolog();
		engine.addLibrary("alice.tuprolog.StringLibrary");
		assertNotNull(engine.library("alice.tuprolog.StringLibrary"));
	}

//	@Disabled
//	@Test public void testLoadLibraryAsObject() throws InvalidLibraryException {
//		Prolog engine = new Prolog();
//		Library stringLibrary = new StringLibrary();
//		engine.loadLibrary(stringLibrary);
//		assertNotNull(engine.getLibrary("alice.tuprolog.StringLibrary"));
//		Library javaLibrary = new alice.tuprolog.lib.OOLibrary();
//		engine.loadLibrary(javaLibrary);
//		assertSame(javaLibrary, engine.getLibrary("alice.tuprolog.lib.JavaLibrary"));
//	}
	
	@Test public void testGetLibraryWithName() throws InvalidLibraryException {
		Prolog engine = new Prolog("alice.tuprolog.TestLibrary");
		assertNotNull(engine.library("TestLibraryName"));
	}
	
	@Test public void testUnloadLibraryAfterLoadingTheory() throws Exception {
		Prolog engine = new Prolog();
		assertNotNull(engine.library("alice.tuprolog.lib.IOLibrary"));
		Theory t = new Theory("a(1).\na(2).\n");
		engine.setTheory(t);
		engine.removeLibrary("alice.tuprolog.lib.IOLibrary");
		assertNull(engine.library("alice.tuprolog.lib.IOLibrary"));
	}
	
	@Test public void testAddTheory() throws InvalidTheoryException {
		Prolog engine = new Prolog();
		Theory t = new Theory("test :- notx existing(s).");
		try {
			engine.input(t);
			fail("");
		} catch (InvalidTheoryException expected) {
			assertEquals("", engine.getTheory().toString());
		}
	}
	
//	@Test public void testSpyListenerManagement() {
//		Prolog engine = new Prolog();
//		SpyListener listener1 = new SpyListener() {
//			public void onSpy(SpyEvent e) {}
//		};
//		SpyListener listener2 = new SpyListener() {
//			public void onSpy(SpyEvent e) {}
//		};
//		engine.addSpyListener(listener1);
//		engine.addSpyListener(listener2);
//		assertEquals(2, engine.getSpyListenerList().size());
//	}
	
	@Test public void testLibraryListener() throws InvalidLibraryException {
		Prolog engine = new Prolog(new String[]{});
		engine.addLibrary("alice.tuprolog.lib.BasicLibrary");
		engine.addLibrary("alice.tuprolog.lib.IOLibrary");
		TestPrologEventAdapter a = new TestPrologEventAdapter();
		engine.addLibraryListener(a);
		engine.addLibrary("alice.tuprolog.lib.OOLibrary");
		assertEquals("alice.tuprolog.lib.OOLibrary", a.firstMessage);
		engine.removeLibrary("alice.tuprolog.lib.OOLibrary");
		assertEquals("alice.tuprolog.lib.OOLibrary", a.firstMessage);
	}
	
	@Test public void testTheoryListener() throws InvalidTheoryException {
		Prolog engine = new Prolog();
		TestPrologEventAdapter a = new TestPrologEventAdapter();
		engine.addTheoryListener(a);
		Theory t = new Theory("a(1).\na(2).\n");
		engine.setTheory(t);
		assertEquals("", a.firstMessage);
		assertEquals("a(1).\n\na(2).\n\n", a.secondMessage);
		t = new Theory("a(3).\na(4).\n");
		engine.input(t);
		assertEquals("a(1).\n\na(2).\n\n", a.firstMessage);
		assertEquals("a(1).\n\na(2).\n\na(3).\n\na(4).\n\n", a.secondMessage);
	}
	
	@Test public void testQueryListener() throws Exception {
		Prolog engine = new Prolog();
		TestPrologEventAdapter a = new TestPrologEventAdapter();
		engine.addQueryListener(a);
		engine.setTheory(new Theory("a(1).\na(2).\n"));
		engine.solve("a(X).");
		assertEquals("a(X)", a.firstMessage);
		assertEquals("yes.\nX / 1", a.secondMessage);
		engine.solveNext();
		assertEquals("a(X)", a.firstMessage);
		assertEquals("yes.\nX / 2", a.secondMessage);
	}

}

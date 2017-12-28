/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import alice.tuprolog.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


/**
 * The Prolog class represents a tuProlog engine.
 */
public class Prolog {

    /*  manager of current theory */
    public final TheoryManager theories;
    /*  component managing primitive  */
    public final PrimitiveManager prims;
    /* component managing operators */
    public final OperatorManager ops;
    /* component managing flags */
    public final Flags flags;
    /* component managing libraries */
    public final LibraryManager libs;
    /* component managing engine */
    public final EngineManager engine;

    /*  spying activated ?  */
    private boolean spy;

//	/*  warning activated ?  */
//	private boolean warning;

    /* listeners registrated for virtual machine output events */
    /*Castagna 06/2011*/
    /* exception activated ? */
    private boolean exception;
    /**/
    private final CopyOnWriteArrayList<OutputListener> outputListeners;
    /* listeners registrated for virtual machine internal events */
    private final CopyOnWriteArrayList<SpyListener> spyListeners;

    /* listeners registrated for virtual machine state change events */
    //private final ArrayList<WarningListener> warningListeners;
    public final static Logger logger = LoggerFactory.getLogger(Prolog.class);

    /*Castagna 06/2011*/
    /* listeners registrated for virtual machine state exception events */
    @Deprecated
    private final CopyOnWriteArrayList<ExceptionListener> exceptionListeners;
    /**/

    /* listeners to theory events */
    private final ArrayList<TheoryListener> theoryListeners;
    /* listeners to library events */
    private final ArrayList<LibraryListener> libraryListeners;
    /* listeners to query events */
    private final ArrayList<Consumer<QueryEvent>> queryListeners;

    /* path history for including documents */
    private ArrayList<String> absolutePathList;
    private String lastPath;
    private boolean warning;


    public Prolog() {
        this(new MutableClauseIndex());
    }

    /**
     * Builds a prolog engine with default libraries loaded.
     * <p>
     * The default libraries are BasicLibrary, ISOLibrary,
     * IOLibrary, and  JavaLibrary
     */
    public Prolog(ClauseIndex dynamics) {
        this(false, dynamics);
        try {
            addLibrary("alice.tuprolog.lib.BasicLibrary");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            addLibrary("alice.tuprolog.lib.ISOLibrary");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            addLibrary("alice.tuprolog.lib.IOLibrary");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (System.getProperty("java.vm.name").equals("IKVM.NET"))
                addLibrary("OOLibrary.OOLibrary, OOLibrary");
            else
                addLibrary("alice.tuprolog.lib.OOLibrary");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Builds a tuProlog engine with loaded
     * the specified libraries
     *
     * @param libs the (class) name of the libraries to be loaded
     */
    public Prolog(String... libs) {
        this(false, new MutableClauseIndex());
        if (libs != null) {
            for (int i = 0; i < libs.length; i++) {
                try {
                    addLibrary(libs[i]);
                } catch (InvalidLibraryException e) {
                    logger.error("loading {}: {}", libs[i], e);
                }
            }
        }
    }


    /**
     * Initialize basic engine structures.
     *
     * @param spy spying activated
     */
    private Prolog(boolean spy, ClauseIndex dynamics) {

        outputListeners = new CopyOnWriteArrayList<>();
        spyListeners = new CopyOnWriteArrayList<>();
        /*Castagna 06/2011*/
        exceptionListeners = new CopyOnWriteArrayList<>();
        /**/
        this.spy = spy;

        /*Castagna 06/2011*/
        exception = true;
        /**/
        theoryListeners = new ArrayList<>();
        queryListeners = new ArrayList<>();
        libraryListeners = new ArrayList<>();
        absolutePathList = new ArrayList<>();
        flags = new Flags();
        libs = new LibraryManager();
        ops = new OperatorManager();
        prims = new PrimitiveManager();
        engine = new EngineManager(this);
        //config managers
        theories = new TheoryManager(this, dynamics);
        libs.start(this);
        prims.start(this);
        engine.initialize();
    }


    /**
     * Gets the current version of the tuProlog system
     */
    public static String getVersion() {
        return alice.util.VersionInfo.getEngineVersion();
    }

    /**
     * Gets the last Element of the path list
     */
    public String getCurrentDirectory() {
        String directory = "";
        if (absolutePathList.isEmpty()) {
            directory = this.lastPath != null ? this.lastPath : System.getProperty("user.dir");
        } else {
            directory = absolutePathList.get(absolutePathList.size() - 1);
        }

        return directory;
    }

//	/**
//	 * Sets the last Element of the path list
//	 */
//	public void setCurrentDirectory(String s) {
//		this.lastPath=s;
//	}


    // theory management interface

    /**
     * Sets a new theory
     *
     * @param th is the new theory
     * @throws InvalidTheoryException if the new theory is not valid
     * @see Theory
     */
    public Prolog setTheory(Theory th) throws InvalidTheoryException {    //no syn
        theories.clear();
        input(th);
        return this;
    }


    /**
     * Adds (appends) a theory
     *
     * @param th is the theory to be added
     * @throws InvalidTheoryException if the new theory is not valid
     * @see Theory
     */
    public Prolog input(Theory th) throws InvalidTheoryException {    //no syn

        Consumer<Theory> ifSuccessful;
        if (!theoryListeners.isEmpty()) {
            Theory oldTh = getTheory();
            ifSuccessful = (newTheory) -> {
                for (TheoryListener tl : theoryListeners) {
                    tl.theoryChanged(new TheoryEvent(this, oldTh, newTheory));
                }
            };
        } else {
            ifSuccessful = null;
        }

        theories.consult(th, true, null);
        theories.solveTheoryGoal();
        Theory newTh = getTheory();

        if (ifSuccessful != null)
            ifSuccessful.accept(newTh);

        return this;
    }

    /**
     * Gets current theory
     *
     * @return current(dynamic) theory
     */
    public Theory getTheory() {    //no syn
        try {
            return new Theory(theories.getTheory(true));
        } catch (Exception ex) {
            return null;
        }
    }


    /**
     * Gets last consulted theory, with the original textual format
     *
     * @return theory
     */
    public Theory getLastConsultedTheory() {    //no syn
        return theories.getLastConsultedTheory();
    }


    /**
     * Clears current theory
     */
    public void clearTheory() {    //no syn
        try {
            setTheory(Theory.Null);
        } catch (InvalidTheoryException e) {
            // this should never happen
        }
    }


    // libraries management interface

    /**
     * Loads a library.
     * <p>
     * If a library with the same name is already present,
     * a warning event is notified and the request is ignored.
     *
     * @param className name of the Java class containing the library to be loaded
     * @return the reference to the Library just loaded
     * @throws InvalidLibraryException if name is not a valid library
     */
    public Library addLibrary(String className) throws InvalidLibraryException {    //no syn
        return libs.loadClass(className);
    }

    /**
     * Loads a library.
     * <p>
     * If a library with the same name is already present,
     * a warning event is notified and the request is ignored.
     *
     * @param className name of the Java class containing the library to be loaded
     * @param paths     The path where is contained the library.
     * @return the reference to the Library just loaded
     * @throws InvalidLibraryException if name is not a valid library
     */
    public Library addLibrary(String className, String... paths) throws InvalidLibraryException {    //no syn
        return libs.loadClass(className, paths);
    }


    /**
     * Loads a specific instance of a library
     * <p>
     * If a library with the same name is already present,
     * a warning event is notified
     *
     * @param lib the (Java class) name of the library to be loaded
     * @throws InvalidLibraryException if name is not a valid library
     */
    public void addLibrary(Library lib) throws InvalidLibraryException {    //no syn
        libs.load(lib);
    }


    /**
     * Unloads a previously loaded library
     *
     * @param name of the library to be unloaded
     * @throws InvalidLibraryException if name is not a valid loaded library
     */
    public void removeLibrary(String name) throws InvalidLibraryException {        //no syn
        libs.unload(name);
    }


    /**
     * Gets the reference to a loaded library
     *
     * @param name the name of the library already loaded
     * @return the reference to the library loaded, null if the library is
     * not found
     */
    public Library library(String name) {    //no syn
        return libs.getLibrary(name);
    }


//	protected Library getLibraryPredicate(String name, int nArgs) {		//no syn
//		return prims.getLibraryPredicate(name,nArgs);
//	}


//	protected Library getLibraryFunctor(String name, int nArgs) {		//no syn
//		return prims.getLibraryFunctor(name,nArgs);
//	}


    // operators management

    /**
     * Gets the list of the operators currently defined
     *
     * @return the list of the operators
     */
    public Iterable<Operator> operators() {    //no syn
        return ops.operators();
    }


    // solve interface

    /**
     * Solves a query
     *
     * @param g the term representing the goal to be demonstrated
     * @return the result of the demonstration
     * @see Solution
     **/
    public Solution solve(Term g) {

        Solution sinfo = engine.solve(g);

        notifyNewQueryResultAvailable(this, sinfo);

        return sinfo;
    }

    public Prolog solve(String g, Consumer<Solution> eachSolution) {
        return solve(term(g), eachSolution);
    }

    public Prolog solve(Term g, Consumer<Solution> eachSolution) {
        return solve(g, eachSolution, -1);
    }

    public Prolog solve(Term g, Consumer<Solution> eachSolution, long timeoutMS) {
        //System.out.println("ENGINE SOLVE #0: "+g);
        Solution next = null;
        do {
            if (next == null) {
                next = engine.solve(g);
                if (next == null)
                    break; //no solutions
            } else {
                try {
                    next = engine.solveNext( /* TODO subdivide input time */);
                } catch (NoMoreSolutionException e) {
                    e.printStackTrace();
                }
            }
            notifyNewQueryResultAvailable(this, next);
            eachSolution.accept(next);
        } while (hasOpenAlternatives());


        return this;
    }

    /**
     * Solves a query
     *
     * @param st the string representing the goal to be demonstrated
     * @return the result of the demonstration
     * @see Solution
     **/
    @Deprecated
    public Solution solve(String st) throws MalformedGoalException {
        try {
            return solve(term(st));
        } catch (InvalidTermException ex) {
            throw new MalformedGoalException();
        }
    }

    public Term term(String toParse) throws InvalidTermException {
        return new Parser(toParse, ops).nextTerm(true);
    }

    /**
     * Gets next solution
     *
     * @return the result of the demonstration
     * @throws NoMoreSolutionException if no more solutions are present
     * @see Solution
     **/
    public Solution solveNext() throws NoMoreSolutionException {
        if (hasOpenAlternatives()) {
            Solution sinfo = engine.solveNext();
            notifyNewQueryResultAvailable(this, sinfo);
            return sinfo;
        } else
            throw new NoMoreSolutionException();
    }

    /**
     * Halts current solve computation
     */
    public void solveHalt() {
        engine.solveHalt();
    }

    /**
     * Accepts current solution
     */
    public void solveEnd() {    //no syn
        engine.solveEnd();
    }


    /**
     * Asks for the presence of open alternatives to be explored
     * in current demostration process.
     *
     * @return true if open alternatives are present
     */
    public boolean hasOpenAlternatives() {        //no syn
        return engine.hasOpenAlternatives();
    }

    /**
     * Checks if the demonstration process was stopped by an halt command.
     *
     * @return true if the demonstration was stopped
     */
    public boolean isHalted() {        //no syn
        return engine.isHalted();
    }

    /**
     * Unifies two terms using current demonstration context.
     *
     * @param t0 first term to be unified
     * @param t1 second term to be unified
     * @return true if the unification was successful
     */
    public static boolean match(Term t0, Term t1) {    //no syn
        return t0.unifiable(t1);
    }

    /**
     * Unifies two terms using current demonstration context.
     *
     * @param t0 first term to be unified
     * @param t1 second term to be unified
     * @return true if the unification was successful
     */
    public boolean unify(Term t0, Term t1) {    //no syn
        return t0.unify(this, t1);
    }


    /**
     * Gets a term from a string, using the operators currently
     * defined by the engine
     *
     * @param st the string representing a term
     * @return the term parsed from the string
     * @throws InvalidTermException if the string does not represent a valid term
     */
    public Term toTerm(String st) throws InvalidTermException {    //no syn
        return Parser.parseSingleTerm(st, ops);
    }

    /**
     * Gets the string representation of a term, using operators
     * currently defined by engine
     *
     * @param term the term to be represented as a string
     * @return the string representing the term
     */
    public String toString(Term term) {        //no syn
        return (term.toStringAsArgY(ops, OperatorManager.OP_HIGH));
    }


//	/**
//	 * Defines a new flag
//	 */
//	public boolean defineFlag(String name, Struct valueList, Term defValue, boolean modifiable, String libName) {
//		return flags.defineFlag(name,valueList,defValue,modifiable,libName);
//	}


    // spy interface ----------------------------------------------------------

    /**
     * Switches on/off the notification of spy information events
     *
     * @param state - true for enabling the notification of spy event
     */
    public synchronized void setSpy(boolean state) {
        spy = state;
    }

    /**
     * Checks the spy state of the engine
     *
     * @return true if the engine emits spy information
     */
    public boolean isSpy() {
        return spy;
    }


    /**
     * Notifies a spy information event
     */
    protected void spy(String s) {
        if (spy) {
            notifySpy(new SpyEvent(this, s));
        }
    }

    /**
     * Notifies a spy information event
     *
     * @param s TODO
     */
    protected void spy(State s, Engine e) {
        //System.out.println("spy: "+i+"  "+s+"  "+g);
        if (spy && spyListeners.size() > 0) {
            ExecutionContext ctx = e.currentContext;
            if (ctx != null) {
                int i = 0;
                String g = "-";
                if (ctx.fatherCtx != null) {
                    i = ctx.depth - 1;
                    g = ctx.fatherCtx.currentGoal.toString();
                }
                notifySpy(new SpyEvent(this, e, "spy: " + i + "  " + s + "  " + g));
            }
        }
    }

//
//	/**
//	 * Notifies a warn information event
//	 *
//	 *
//	 * @param m the warning message
//	 */
//	public void warn(String m) {
//		logger.warn(m);
////		if (warning){
////			notifyWarning(new WarningEvent(this, m));
////			//log.warn(m);
////		}
//	}

    /*Castagna 06/2011*/

    /**
     * Notifies a exception information event
     *
     * @param m the exception message
     */
    public void exception(String m) {
        if (exception) {
            notifyException(new ExceptionEvent(this, m));
        }
    }
    /**/

    /*Castagna 06/2011*/

    /**
     * Checks if exception information are notified
     *
     * @return true if the engine emits exception information
     */
    public synchronized boolean isException() {
        return exception;
    }
    /**/

    /*Castagna 06/2011*/

    /**
     * Switches on/off the notification of exception information events
     *
     * @param state - true for enabling exception information notification
     */
    public synchronized void setException(boolean state) {
        exception = state;
    }
    /**/

    /**
     * Produces an output information event
     *
     * @param m the output string
     */
    public void output(String m) {

        int outputListenersSize = outputListeners.size();
        if (outputListenersSize == 0)
            return; //ignored

        OutputEvent e = new OutputEvent(this, m);
        synchronized (outputListeners) {
            for (int i = 0; i < outputListenersSize; i++) {
                outputListeners.get(i).onOutput(e);
            }
        }
    }

    // event listeners management

    /**
     * Adds a listener to ouput events
     *
     * @param l the listener
     */
    public void addOutputListener(OutputListener l) {
        outputListeners.add(l);
    }


    /**
     * Adds a listener to theory events
     *
     * @param l the listener
     */
    public synchronized void addTheoryListener(TheoryListener l) {
        theoryListeners.add(l);
    }

    /**
     * Adds a listener to library events
     *
     * @param l the listener
     */
    public synchronized void addLibraryListener(LibraryListener l) {
        libraryListeners.add(l);
    }

    /**
     * Adds a listener to theory events
     *
     * @param l the listener
     */
    public void addQueryListener(Consumer<QueryEvent> l) {
        synchronized (queryListeners) {
            queryListeners.add(l);
        }
    }

    /**
     * Adds a listener to spy events
     *
     * @param l the listener
     */
    public void addSpyListener(SpyListener l) {
        spy = true;
        spyListeners.add(l);
    }

//	/**
//	 * Adds a listener to warning events
//	 *
//	 * @param l the listener
//	 */
//	public synchronized void addWarningListener(WarningListener l) {
//		warningListeners.add(l);
//	}

    /*Castagna 06/2011*/

    /**
     * Adds a listener to exception events
     *
     * @param l the listener
     */
    public void addExceptionListener(ExceptionListener l) {
        exceptionListeners.add(l);
    }
    /**/

    /**
     * Removes a listener to ouput events
     *
     * @param l the listener
     */
    public void removeOutputListener(OutputListener l) {
        outputListeners.remove(l);
    }

    /**
     * Removes all output event listeners
     */
    public synchronized void removeAllOutputListeners() {
        outputListeners.clear();
    }

    /**
     * Removes a listener to theory events
     *
     * @param l the listener
     */
    public synchronized void removeTheoryListener(TheoryListener l) {
        theoryListeners.remove(l);
    }

    /**
     * Removes a listener to library events
     *
     * @param l the listener
     */
    public synchronized void removeLibraryListener(LibraryListener l) {
        libraryListeners.remove(l);
    }

    /**
     * Removes a listener to query events
     *
     * @param l the listener
     */
    public void removeQueryListener(Consumer<QueryEvent> l) {
        synchronized (queryListeners) {
            queryListeners.remove(l);
        }
    }


    /**
     * Removes a listener to spy events
     *
     * @param l the listener
     */
    public synchronized void removeSpyListener(SpyListener l) {
        spyListeners.remove(l);
        spy = !(spyListeners.isEmpty());
    }

    /**
     * Removes all spy event listeners
     */
    public synchronized void removeAllSpyListeners() {
        spy = false;
        spyListeners.clear();
    }

//	/**
//	 * Removes a listener to warning events
//	 *
//	 * @param l the listener
//	 */
//	public synchronized void removeWarningListener(WarningListener l) {
//		warningListeners.remove(l);
//	}
//
//	/**
//	 * Removes all warning event listeners
//	 */
//	public synchronized void removeAllWarningListeners() {
//		warningListeners.clear();
//	}

    /* Castagna 06/2011*/

    /**
     * Removes a listener to exception events
     *
     * @param l the listener
     */
    public synchronized void removeExceptionListener(ExceptionListener l) {
        exceptionListeners.remove(l);
    }
    /**/

    /*Castagna 06/2011*/

    /**
     * Removes all exception event listeners
     */
    public synchronized void removeAllExceptionListeners() {
        exceptionListeners.clear();
    }
    /**/
//
//	/**
//	 * Gets a copy of current listener list to output events
//	 */
//	public synchronized List<OutputListener> getOutputListenerList() {
//		return new ArrayList<>(outputListeners);
//	}

//	/**
//	 * Gets a copy of current listener list to warning events
//	 *
//	 */
//	public synchronized List<WarningListener> getWarningListenerList() {
//		return new ArrayList<>(warningListeners);
//	}
//
//	/*Castagna 06/2011*/
//	/**
//	 * Gets a copy of current listener list to exception events
//	 *
//	 */
//	public synchronized List<ExceptionListener> getExceptionListenerList() {
//		return new ArrayList<>(exceptionListeners);
//	}
//	/**/
//
//	/**
//	 * Gets a copy of current listener list to spy events
//	 *
//	 */
//	public synchronized List<SpyListener> getSpyListenerList() {
//		return new ArrayList<>(spyListeners);
//	}
//
//	/**
//	 * Gets a copy of current listener list to theory events
//	 *
//	 */
//	public synchronized List<TheoryListener> getTheoryListenerList() {
//		return new ArrayList<>(theoryListeners);
//	}
//
//	/**
//	 * Gets a copy of current listener list to library events
//	 *
//	 */
//	public synchronized List<LibraryListener> getLibraryListenerList() {
//		return new ArrayList<>(libraryListeners);
//	}
//
//	/**
//	 * Gets a copy of current listener list to query events
//	 *
//	 */
//	public synchronized List<QueryListener> getQueryListenerList() {
//		return new ArrayList<>(queryListeners);
//	}
//
//	// notification

    /**
     * Notifies a spy information event
     *
     * @param e the event
     */
    private void notifySpy(SpyEvent e) {
        for (int i = 0, spyListenersSize = spyListeners.size(); i < spyListenersSize; i++) {
            spyListeners.get(i).onSpy(e);
        }
    }

    /*Castagna 06/2011*/

    /**
     * Notifies a exception information event
     *
     * @param e the event
     */
    protected void notifyException(ExceptionEvent e) {
        //
        for (int i = 0, exceptionListenersSize = exceptionListeners.size(); i < exceptionListenersSize; i++) {
            exceptionListeners.get(i).onException(e);
        }
        logger.error("{} {}", e.getSource(), e.getMsg());
    }
    /**/

    //

    /**
     * Notifies a library loaded event
     *
     * @param e the event
     */
    protected void notifyLoadedLibrary(LibraryEvent e) {
        for (LibraryListener ll : libraryListeners) {
            ll.libraryLoaded(e);
        }
    }

    /**
     * Notifies a library unloaded event
     *
     * @param e the event
     */
    protected void notifyUnloadedLibrary(LibraryEvent e) {
        for (LibraryListener ll : libraryListeners) {
            ll.libraryUnloaded(e);
        }
    }

    /**
     * Notifies a library loaded event
     *
     * @param e the event
     */
    protected void notifyNewQueryResultAvailable(Prolog source, Solution info) {

        int qls = queryListeners.size();
        if (qls > 0) {
            QueryEvent e = new QueryEvent(source, info);
            for (int i = 0, queryListenersSize = qls; i < queryListenersSize; i++) {
                queryListeners.get(i).accept(e);
            }
        }// else {
        //throw new RuntimeException("no query listeners attached");
        //}
    }


    /**
     * Append a new path to directory list
     */
    public void pushDirectoryToList(String path) {
        absolutePathList.add(path);
    }

    /**
     * Retract an element from directory list
     */
    public void popDirectoryFromList() {
        if (!absolutePathList.isEmpty()) {
            absolutePathList.remove(absolutePathList.size() - 1);
        }
    }

    /**
     * Reset directory list
     */
    public void resetDirectoryList(String path) {
        absolutePathList = new ArrayList<>();
        absolutePathList.add(path);
    }

    public Term termSolve(String st) {
        try {
            Parser p = new Parser(st, ops);
            return p.nextTerm(true);
        } catch (InvalidTermException e) {
            //TODO: Term.NULL
            return Term.term("null");
        }
    }

    public boolean isTrue(String s) {
        return isTrue(term(s));
    }

    public boolean isTrue(Term s) {
        Solution r = solve(s);
        return r.isSuccess();
//        switch (r.toString()) {
//            case Term.YES:
//                return true;
//            case Term.NO:
//                return false;
//            default:
//                throw new RuntimeException(s + " has non-boolean solution " + r);
//        }
    }

    public static void warn(String s) {
        logger.warn(s);
    }

    public boolean isWarning() {
        return warning;
    }

    public void notifyWarning(WarningEvent warningEvent) {
        if (warning)
            logger.warn("warning {}", warningEvent);
    }

    public void setWarning(boolean b) {
        this.warning = b;
    }
}
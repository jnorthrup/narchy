/*
 * Prolog.java
 *
 * Created on March 12, 2007, 2:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.engine;


import alice.tuprolog.PrologLib;
import alice.tuprolog.Solution;
import alice.tuprologx.pj.model.Term;
import alice.tuprologx.pj.model.Theory;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author maurizio
 */
public class PJProlog /*extends alice.tuprolog.Prolog*/ {
    
    protected final alice.tuprolog.Prolog engine;
    
    public PJProlog() {
        engine = new alice.tuprolog.Prolog();        
        try {
            engine.removeLibrary("alice.tuprolog.lib.OOLibrary");
            engine.addLibrary("alice.tuprologx.pj.lib.PJLibraryNew");
            engine.addLibrary("alice.tuprolog.lib.DCGLibrary");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public <G extends Term<?>, S extends Term<?>> Iterable<PrologSolution<G,S>>  solveAll(final G query) {            
            class SolutionProxy implements Iterable<PrologSolution<G,S>> {
                @Override
                public Iterator<PrologSolution<G,S>> iterator() {
                    PrologSolution<G,S> first = PJProlog.this.solve(query);
                    return new SolutionIterator<>(first);
                }
            }
        return new SolutionProxy();
        }

    public <G extends Term<?>, S extends Term<?>> PrologSolution<G,S> solve(G g) {
        Solution retValue = engine.solve(g.marshal());
        return new PrologSolution<>(retValue);
    }
    
    public <G extends Term<?>, S extends Term<?>> PrologSolution<G,S> solveNext() throws NoSolutionException {
        Solution retValue;
        try {
            retValue = engine.solveNext();
        }
        catch (Exception e) {
            throw new NoSolutionException();
        }
        return new PrologSolution<>(retValue);
    }

    public void setTheory(Theory theory) throws alice.tuprolog.InvalidTheoryException {
        engine.setTheory(new alice.tuprolog.Theory(theory.marshal()));
    }

    public void addTheory(Theory theory) throws alice.tuprolog.InvalidTheoryException {
        engine.input(new alice.tuprolog.Theory(theory.marshal()));
    }
    
    public Theory getTheory() {
        return Theory.unmarshal(engine.getTheory());
    }
        
    /**
	 * @author  ale
	 */
    class SolutionIterator<G extends Term<?>, S extends Term<?>> implements Iterator<PrologSolution<G,S>> {

        PrologSolution<G,S> current;
        PrologSolution<G,S> next;

        SolutionIterator(PrologSolution<G,S> first) {            
            this.current = first;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PrologSolution<G,S> next() {
            if (current != null) {
                hasNext();
                PrologSolution<G,S> temp = current;
                current = (next != null && next.isSuccess()) ? next : null;
                next = null;
                return temp;
            }
            else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                try {
                    next = new PrologSolution<>(engine.solveNext());
                }
                catch (alice.tuprolog.NoMoreSolutionException e) {
                    next = null;
                }
            }
            return current != null && current.isSuccess();
        }
    }
    
     public alice.tuprolog.Struct registerJavaObject(Object o) {
        return ((alice.tuprolog.lib.OOLibrary)engine.library("alice.tuprologx.pj.lib.PJLibraryNew")).register(o);
     }
     
     public Object getJavaObject(alice.tuprolog.Struct t) { 
        try {
            return ((alice.tuprolog.lib.OOLibrary)engine.library("alice.tuprologx.pj.lib.PJLibraryNew")).getRegisteredObject(t);
        }
        catch (Exception e) {
            return null;
        }        
     }
     
     public void loadLibrary(PrologLib prologLib) {
         try {
            engine.addLibrary(prologLib);
         }
         catch (Exception e) {
             throw new UnsupportedOperationException(e);
         }
     }

     public alice.tuprologx.pj.lib.PJLibraryNew getPJLibrary() {
         try {
            return (alice.tuprologx.pj.lib.PJLibraryNew)engine.library("alice.tuprologx.pj.lib.PJLibraryNew");
         }
         catch (Exception e) {
             return null;
         }
     }
}

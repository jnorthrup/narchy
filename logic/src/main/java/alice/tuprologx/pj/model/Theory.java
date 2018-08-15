/*
 * Theory.java
 *
 * Created on April 4, 2007, 10:00 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;


import java.util.Collection;
import java.util.Vector;

/**
 *
 * @author maurizio
 */
public class Theory extends List<Clause<?,?>> {
    
    private static final alice.tuprolog.Prolog engine;
      
    static {
        engine = new alice.tuprolog.Prolog();
        try {
            engine.removeLibrary("alice.tuprolog.lib.OOLibrary");
            engine.addLibrary("alice.tuprologx.pj.lib.PJLibrary");
            engine.addLibrary("alice.tuprolog.lib.DCGLibrary");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /*  
        static {
            try {
                alice.tuprolog.Prolog p = new alice.tuprolog.Prolog();
                java.lang.reflect.Method m = p.getClass().getDeclaredMethod("getTheoryManager");
                m.setAccessible(true);             
                _tm = (alice.tuprolog.TheoryManager)m.invoke(p);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    */
    /** Creates a new instance of Theory */
    public Theory(Collection<Clause<?,?>> clauses) {
        super(clauses);        
    }
    
    public Theory(String s) {
        this(parseTheory(s));
        
    }
    
    public Theory(String... s) {
        this(parseTheoryArray(s));
    }
    
    public static Theory unmarshal(alice.tuprolog.Theory t) {
        Vector<Clause<?,?>> clauses = new Vector<>();
        for (java.util.Iterator<? extends alice.tuprolog.Term> it = t.iterator(engine.ops); it.hasNext();) {
            alice.tuprolog.Struct st = (alice.tuprolog.Struct) it.next();                        
            
            Clause<?,?> clause = new Clause<>(st);
            clauses.add(clause);            
        }        
        return new Theory(clauses);
    }
    






























    
    /* This method should be removed or deprecated when (hepefully) one day tuProlog
     * will expose the clause list view over a Prolog theory. Currently this method has to deal with all
     * the strange formattings that are carried out by the tuProlog's TheoryManager!!
     */    






































    
    private static Collection<Clause<?,?>> parseTheory(String s) {
        alice.tuprolog.Theory t = null;
        try {
            t = new alice.tuprolog.Theory(s);
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
        Vector<Clause<?, ?>> clauses = new Vector<>();
        for (java.util.Iterator<? extends alice.tuprolog.Term> it = t.iterator(engine.ops); it.hasNext();) {
            alice.tuprolog.Struct st = (alice.tuprolog.Struct) it.next();                        
            
            Clause<?,?> clause = new Clause<>(st);
            clauses.add(clause);            
        }        
        return clauses;
    }
    
    private static Collection<Clause<?,?>> parseTheoryArray(String... arr) {
        String temp="";
        for (String s : arr) {
            temp+=s+ '\n';
        }
        return parseTheory(temp);
    }
    
    public Clause<?,?>[] find(String name, int arity) {
        Vector<Clause<?,?>> temp = new Vector<>();
        for (Clause<?,?> c: this) {
            if (c.match(name,arity))
                temp.add(c);
        }
        return temp.toArray(new Clause<?, ?>[0]);
    }
    
    @Override
    public alice.tuprolog.Struct marshal() {
            alice.tuprolog.Struct s = super.marshal();
            java.util.Iterator<? extends alice.tuprolog.Term> listIterator = s.listIterator();
            while (listIterator.hasNext()) {
                listIterator.next().resolveTerm();
            }
            return s;
    }
    
    public void appendTheory(Theory that) {
        for (Clause<?,?> c : that) {
            _theList.add(c);
        }
    }    
}

package alice.tuprologx.pj.engine;

import alice.tuprologx.pj.model.*;

import java.util.Vector;

/**
 *
 * @author maurizio
 */
public class TheoryFilter {
    
	protected final Theory _theory;
    protected final Theory _filter;
    protected PJProlog _engine;
    
    private static final String base_filter_string = "filter(L,R):-filter(L,[],R).\n"+
                                 "filter([],X,X).\n"+
                                 "filter([H|T],F,R):-call(H),append(F,[H],Z),filter(T,Z,R).\n"+
                                 "filter([H|T],F,R):-not call(H),filter(T,F,R).\n";
    
    private static final Theory base_filter = new Theory(base_filter_string);
    
    
    /** Creates a new instance of TheoryFilter */
    public TheoryFilter(Theory theory, Theory filter) {
        _theory = theory;
        _filter = filter;
    }
    
    public TheoryFilter(Theory theory, String filter) {
        this(theory,new Theory(filter));
    }
    
    @SuppressWarnings("unchecked")
	public Theory apply() {                
        Var<List<Clause<?,?>>> filtered_list = new Var<>("X");
        Compound2<List<Clause<?,?>>,Var<List<Clause<?,?>>>> goal = new Compound2<>("filter", _theory, filtered_list);
        try {
            PJProlog p = new PJProlog();        
            p.setTheory(_filter);
            p.addTheory(base_filter);
            
            
            
            PrologSolution<?,?> sol = p.solve(goal);
            List<Term<?>> res = sol.getTerm("X");            
            
            Vector<Clause<?,?>> filtered_clauses = new Vector<>();
            for (Term<?> t : res) {
                if (t instanceof Compound2 && ((Compound2<Term<?>,Term<?>>)t).getName().equals(":-")) {
                    filtered_clauses.add(new Clause<Term<?>,Term<?>>(((Compound2<Term<?>,Term<?>>)t).get0(),((Compound2<Term<?>,Term<?>>)t).get1()));
                }
                else {
                    filtered_clauses.add(new Clause<Term<?>,Term<?>>(t,null));
                }
            }
            return new Theory(filtered_clauses);
        }
        catch (Exception e) {
            e.printStackTrace();
            
            
            
            
            
            
            return _theory;
        }
    }    
}

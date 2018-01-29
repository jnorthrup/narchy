/*
 * Clause.java
 *
 * Created on April 4, 2007, 9:19 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import alice.tuprolog.Struct;

/**
 *
 * @author maurizio
 */
public class Clause<H extends Term<?>, B extends Term<?>> extends Compound2<H,B> {
    
    public final boolean isFact;
    
    /** Creates a new instance of Clause */
    @SuppressWarnings("unchecked")
	public Clause(H head, B body) {
        super(":-", head, body == null ? (B)new Bool(true) : body);        
        isFact = (body == null || body instanceof Bool);
    }
    
    @SuppressWarnings("unchecked")
	public Clause(Struct s) { 
        this(Term.unmarshal(s.name().equals(":-") ? s.sub(0) : s), s.name().equals(":-") ? (B)Term.unmarshal(s.sub(1)) : null);
    }
    /*
    public Clause(String s) {
        this((H)parseClause(s).get0(), (B)parseClause(s).get1());
    }
    
    private static Compound2<?,?> parseClause(String s) {
        Parser p = new Parser(s);               
        if (p.readTerm(false) != Parser.EOF) {
            if (p.getCurrentTerm()!=null && p.getCurrentTermType()== Parser.TERM && p.getCurrentTerm().isStruct()) {
                Cons c = Term.unmarshal(p.getCurrentTerm());
                if (!c.getName().equals(":-")) {                                      
                    c = new Compound2(null,c,null);
                }
                return (Compound2<?,?>)c;
            }
        }
        return null;
    }
    */
    public B getBody() {
        return get1();
    }

    public String toString() {
        return "Clause{"+getHead()+(isFact ? "" : " :- "+getBody())+ '}';
    }

    @Override
    public Struct marshal() {
        return !isFact ? super.marshal() : (Struct) getHead().marshal();
    }
    
    public boolean match(String name, int arity) {
        if (getHead() instanceof Compound<?>) {
            return ((Compound<?>)getHead()).getName().equals(name) && arity == ((Compound<?>)getHead()).arity();
        }
        return false;
    }
}

package alice.tuprologx.pj.model;

import alice.tuprolog.NumberTerm;
import alice.tuprologx.pj.annotations.Termifiable;

/**
 *
 * @author maurizio
 */
public abstract class Term<X extends Term<?>> {
	
	
	@SuppressWarnings("unchecked")
	static <S, T> T uncheckedCast(S s) {
	     return (T)s;
	}
	
	
	
	public abstract <Z> Z toJava(); 
	public static <Z extends Term<?>> Z fromJava(Object o) {
		if (o instanceof Integer) {
			
			return uncheckedCast(new Int((Integer)o));
		}
		else if (o instanceof java.lang.Double) {
			
			return uncheckedCast(new Double((java.lang.Double)o));
		}
		else if (o instanceof String) {
			
			return uncheckedCast(new Atom((String)o));
		}
		else if (o instanceof Boolean) {
			
			return uncheckedCast(new Bool((Boolean)o));
		}
		else if (o instanceof java.util.Collection<?>) {
			
			return uncheckedCast(new List<>((java.util.Collection<?>) o));
		}
        else if (o instanceof Term<?>[]) {
			
			return uncheckedCast(new Cons<>("_", (Term<?>[]) o));
		}
		else if (o instanceof Term<?>) {
			
			return uncheckedCast(o);
		}
        else if (o.getClass().isAnnotationPresent(Termifiable.class)) {
            
        	return uncheckedCast(new JavaTerm<>(o));
		}                
		/*else {
			throw new UnsupportedOperationException();
		}*/
        else {
            
        	return uncheckedCast(new JavaObject<>(o));
        }
	}
        
    public abstract alice.tuprolog.Term marshal() /*{
            throw new UnsupportedOperationException();
        }*/;
        
    public static <Z extends Term<?>> Z unmarshal(alice.tuprolog.Term t) {
		if (Int.matches(t)) {
			
			return uncheckedCast(Int.unmarshal((NumberTerm.Int)t));
		}
		else if (Double.matches(t)) {
			
			return uncheckedCast(Double.unmarshal((NumberTerm.Double)t));
		}
        else if (JavaObject.matches(t)) {
			
			return uncheckedCast(JavaObject.unmarshalObject((alice.tuprolog.Struct)t));
		}
		else if (Atom.matches(t)) {
			
			return uncheckedCast(Atom.unmarshal((alice.tuprolog.Struct)t));
		}
		else if (Bool.matches(t)) {
			
			return uncheckedCast(Bool.unmarshal((alice.tuprolog.Struct)t));
		}
		else if (List.matches(t)) {
			
			return uncheckedCast(List.unmarshal((alice.tuprolog.Struct)t));
		}
        else if (JavaTerm.matches(t)) {
			
			return uncheckedCast(JavaTerm.unmarshalObject((alice.tuprolog.Struct)t.term()));
		}                
        else if (Cons.matches(t)) {
			
			return uncheckedCast(Cons.unmarshal((alice.tuprolog.Struct)t));
		}                
        else if (Var.matches(t)) {
			
			return uncheckedCast(Var.unmarshal((alice.tuprolog.Var)t));
		}
		else {System.out.println(t);
			throw new UnsupportedOperationException();
		}
	}
}














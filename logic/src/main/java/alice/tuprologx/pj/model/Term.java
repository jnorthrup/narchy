package alice.tuprologx.pj.model;

import alice.tuprolog.NumberTerm;
import alice.tuprologx.pj.annotations.Termifiable;

/**
 *
 * @author maurizio
 */
public abstract class Term<X extends Term<?>> {
	
	// Added by ED 2013-05-21 following MC suggestion
	@SuppressWarnings("unchecked")
	static <S, T> T uncheckedCast(S s) {
	     return (T)s;
	}
	// END ADDITION
	
	
	public abstract <Z> Z toJava(); // {return null;}
	public static <Z extends Term<?>> Z fromJava(Object o) {
		if (o instanceof Integer) {
			//return (Z)new Int((Integer)o);
			return uncheckedCast(new Int((Integer)o));
		}
		else if (o instanceof java.lang.Double) {
			//return (Z)new Double((java.lang.Double)o);
			return uncheckedCast(new Double((java.lang.Double)o));
		}
		else if (o instanceof String) {
			//return (Z)new Atom((String)o);
			return uncheckedCast(new Atom((String)o));
		}
		else if (o instanceof Boolean) {
			//return (Z)new Bool((Boolean)o);
			return uncheckedCast(new Bool((Boolean)o));
		}
		else if (o instanceof java.util.Collection<?>) {
			// return (Z)new List<Term<?>>((java.util.Collection<?>)o);
			return uncheckedCast(new List<>((java.util.Collection<?>) o));
		}
        else if (o instanceof Term<?>[]) {
			// return (Z)new Cons<Term<?>, Compound<?>>("_",(Term<?>[])o);
			return uncheckedCast(new Cons<>("_", (Term<?>[]) o));
		}
		else if (o instanceof Term<?>) {
			//return (Z)o;
			return uncheckedCast(o);
		}
        else if (o.getClass().isAnnotationPresent(Termifiable.class)) {
            // return (Z)new JavaTerm<Object>(o);
        	return uncheckedCast(new JavaTerm<>(o));
		}                
		/*else {
			throw new UnsupportedOperationException();
		}*/
        else {
            // return (Z)new JavaObject<Object>(o);
        	return uncheckedCast(new JavaObject<>(o));
        }
	}
        
    public abstract alice.tuprolog.Term marshal() /*{
            throw new UnsupportedOperationException();
        }*/;
        
    public static <Z extends Term<?>> Z unmarshal(alice.tuprolog.Term t) {
		if (Int.matches(t)) {
			// return (Z)Int.unmarshal((alice.tuprolog.Int)t);
			return uncheckedCast(Int.unmarshal((NumberTerm.Int)t));
		}
		else if (Double.matches(t)) {
			//return (Z)Double.unmarshal((alice.tuprolog.Double)t);
			return uncheckedCast(Double.unmarshal((NumberTerm.Double)t));
		}
        else if (JavaObject.matches(t)) {
			//return (Z)JavaObject.unmarshalObject((alice.tuprolog.Struct)t);
			return uncheckedCast(JavaObject.unmarshalObject((alice.tuprolog.Struct)t));
		}
		else if (Atom.matches(t)) {
			//return (Z)Atom.unmarshal((alice.tuprolog.Struct)t);
			return uncheckedCast(Atom.unmarshal((alice.tuprolog.Struct)t));
		}
		else if (Bool.matches(t)) {
			//return (Z)Bool.unmarshal((alice.tuprolog.Struct)t);
			return uncheckedCast(Bool.unmarshal((alice.tuprolog.Struct)t));
		}
		else if (List.matches(t)) {
			//return (Z)List.unmarshal((alice.tuprolog.Struct)t);
			return uncheckedCast(List.unmarshal((alice.tuprolog.Struct)t));
		}
        else if (JavaTerm.matches(t)) {
			//return (Z)JavaTerm.unmarshalObject((alice.tuprolog.Struct)t.getTerm());
			return uncheckedCast(JavaTerm.unmarshalObject((alice.tuprolog.Struct)t.term()));
		}                
        else if (Cons.matches(t)) {
			//return (Z)Cons.unmarshal((alice.tuprolog.Struct)t);
			return uncheckedCast(Cons.unmarshal((alice.tuprolog.Struct)t));
		}                
        else if (Var.matches(t)) {
			//return (Z)Var.unmarshal((alice.tuprolog.Var)t);
			return uncheckedCast(Var.unmarshal((alice.tuprolog.Var)t));
		}
		else {System.out.println(t);
			throw new UnsupportedOperationException();
		}
	}
}














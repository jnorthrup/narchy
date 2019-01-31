package alice.tuprologx.pj.meta;

import alice.tuprologx.pj.annotations.PrologField;
import alice.tuprologx.pj.engine.PrologObject;
import alice.tuprologx.pj.model.Term;
import alice.tuprologx.pj.model.Theory;

import java.lang.reflect.Field;

/**
 *
 * @author Maurizio
 */
public class PrologMetaField {
    
    private final Field _theField;
    private final PrologMetaClass _enclosing;
    private final PrologField _annotation;
    private Theory _template;
    
    /** Creates a new instance of MetaPrologClass */
    public PrologMetaField(PrologMetaClass cl, Field f) {
        _theField = f;
        _enclosing = cl;
        _annotation = _theField.getAnnotation(PrologField.class);
        initTheory();
    }
    
    public PrologMetaClass getEnclosingMeta() {
        return _enclosing;
    }
    
    public <T extends Term<?>> void setValue(PrologObject o, T t) {        
        try {
            _theField.set(o,t);
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
	public <T extends Term<?>> T getValue(PrologObject o) {        
        try {
            return (T)_theField.get(o);
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    
    private void initTheory() {
        String fname = _theField.getName();
        String pname =_annotation.predicate();
        if (pname.isEmpty())
            pname = fname;
        _template = new Theory(pname + "(X):-this(Z), Z."+ fname + " <- get(X).\n" +
                             pname + " := V:-this(Z), Z."+ fname + " <- set(V).\n");
    }

    public Theory getTheory() {
        return _template;
    }
    
    public Field getJavaField() {
        return _theField;
    }
    
    public void init(PrologObject o) {
        String init = _annotation.init();
        if (init != "") {
            try {
                Term<?> t = Term.unmarshal(alice.tuprolog.Parser.parseSingleTerm(init));
                System.out.println("init field = "+t);
                setValue(o, t);
            }
            catch (Exception e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }
}

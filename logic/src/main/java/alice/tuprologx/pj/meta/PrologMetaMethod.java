package alice.tuprologx.pj.meta;

import alice.tuprologx.pj.annotations.PrologMethod;
import alice.tuprologx.pj.annotations.parser.Parser;
import alice.tuprologx.pj.annotations.parser.PrologTree.PredicateExpr;
import alice.tuprologx.pj.engine.PrologObject;
import alice.tuprologx.pj.model.Clause;
import alice.tuprologx.pj.model.Term;
import alice.tuprologx.pj.model.Theory;

import java.lang.reflect.Method;

/**
 *
 * @author Maurizio
 */
public class PrologMetaMethod {
    
    private final Method _theMethod;
    private final PrologMetaClass _enclosing;
    private final PrologMethod _annotation;
    private Theory _theory;
    private int _arity;
    private String _predicateName;
    private Clause<?,?>[] _clauses; 
    /*
    private Vector<String> variableNames;
    private Vector<String> inputVariables;
    private Vector<String> outputVariables;
    
    private boolean multipleResult;
    */
    /** Creates a new instance of MetaPrologClass */
    public PrologMetaMethod(PrologMetaClass cl, Method m) {
        _theMethod = m;        
        _enclosing = cl;
        _annotation = _theMethod.getAnnotation(PrologMethod.class);
        initTheory();
        initClauses();
    }
    
    void initClauses() {
        
        if (_annotation.predicate().isEmpty())  {
            _predicateName = _theMethod.getName();
            _arity = _theMethod.getTypeParameters().length;
        }
        else {
            PredicateExpr p = new Parser(_annotation.predicate()).parsePredicate();        
            _predicateName = p.name;        
            _arity = p.variables.size();
        }        
        _clauses = _theory.find(_predicateName, _arity);
        /*if (_clauses.length == 0) {
            _clauses = _enclosing.getTheory().find(_predicateName, _arity);
        }
        if (_clauses.length == 0) {
            for (PrologMetaField metaField : _enclosing.getPrologFields()) {
                _clauses = metaField.getTemplate().find(_predicateName, _arity);
            }
        }
        if (_clauses.length == 0) {
            throw new InvalidPredicateException(_predicateName+"/"+_arity);
        }*/
    }
    
    private void initTheory() {
        _theory = new Theory(_annotation.clauses());
    }
    
    public Theory getTheory() {
        return _theory;
    }
    
    public void setTheory(Theory t) {
        _theory = t;
        initClauses();
    }
    
    public Clause<?,?>[] getClauses() {
        return _clauses;
    }
    
    public int getArity() {
        return _arity;
    }
    
    public boolean hasMultipleOutput() {
        return _theMethod.getReturnType().equals(Iterable.class);
    }
    
    public Object invoke(PrologObject o, Term<?>... args) {
        try {
            return _theMethod.invoke(o,(Object)args);
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    public PrologMetaClass getEnclosingMeta() {
        return _enclosing;
    }    
    
    public Method getJavaMethod() {
        return _theMethod;
    }
    
    public String getPredicateName() {
        return _predicateName;
    }
}

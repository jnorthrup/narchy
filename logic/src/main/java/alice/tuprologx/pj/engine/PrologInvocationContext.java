package alice.tuprologx.pj.engine;

import alice.tuprologx.pj.annotations.PrologMethod;
import alice.tuprologx.pj.annotations.TRACE;
import alice.tuprologx.pj.annotations.parser.Parser;
import alice.tuprologx.pj.annotations.parser.PrologTree.SignatureExpr;
import alice.tuprologx.pj.annotations.parser.PrologTree.VariableExpr;
import alice.tuprologx.pj.model.Cons;
import alice.tuprologx.pj.model.Nil;
import alice.tuprologx.pj.model.Term;
import alice.tuprologx.pj.model.Var;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.Vector;


/**
 *
 * @author maurizio
 */
public class PrologInvocationContext {    
    
    private String predicateName;
    private Vector<String> variableNames;
    private Vector<String> inputVariables;
    private Vector<String> outputVariables;
    private boolean multipleResult;        
    private final boolean exceptionOnFailure;
    private boolean keepSubstitutions;
    private final boolean trace;
    
    /** Creates a new instance of InvocationObject */
    public PrologInvocationContext(Method m, Object... args) {
        PrologMethod pann = m.getAnnotation(PrologMethod.class);
        assert (pann != null);         
        keepSubstitutions = pann.keepSubstitutions();
        initPredicateName(m,pann);
        initVariableNames(m,pann);
        initInputVariables(m,pann);
        initOutputVariables(m,pann);                
        exceptionOnFailure = pann.exceptionOnFailure();                                                
        trace = m.isAnnotationPresent(TRACE.class);            
    }
    
    public Term<?> buildGoal(Object... args) {
        Term<?>[] tlist = new Term<?>[variableNames.size()];
        int i = 0;
        for (String name : variableNames) {
            int pos;
            if ((pos = inputVariables.indexOf(name)) != -1) {
                tlist[i] = (Term<?>)args[pos];
            }
            i++;
        }
        for (i=0;i<tlist.length;i++) {
            if (tlist[i] == null) {
                tlist[i] = new Var<>("PJVAR" + i);
            }
        }
        return Cons.make(predicateName,tlist);        
    }
    
    private void initPredicateName(Method m, PrologMethod pm) {
        if (pm.predicate().isEmpty()) {
            predicateName = m.getName();
        }
        else {
            Parser p = new Parser(pm.predicate());        
            predicateName = p.parsePredicate().name;
        }
    }
    
    private void initVariableNames(Method m, PrologMethod pm) {
        variableNames = new Vector<>();
        if (pm.predicate().isEmpty()) {
            for (TypeVariable<?> tv : m.getTypeParameters()) {                
                if (tv.getName().startsWith("$")) {
                    variableNames.add(tv.getName());
                }
            }
        }
        else {
            Parser p = new Parser(pm.predicate());        
            java.util.List<VariableExpr> variables = p.parsePredicate().variables;            
            for (VariableExpr v : variables) {
                variableNames.add(v.name);
            }
        }
    }
    
    private void initInputVariables(Method m, PrologMethod pm) {
        inputVariables = new Vector<>();
        if (pm.signature().isEmpty()) {
            for (Type t : m.getGenericParameterTypes()) {
                if (t instanceof TypeVariable<?>) {                    
                    inputVariables.add(((TypeVariable<?>)t).getName());                    
                }
            }
        }
        else {
            Parser p = new Parser(pm.signature());        
            java.util.List<VariableExpr> variables = p.parseSignature().inputTree.variables;            
            for (VariableExpr v : variables) {
                inputVariables.add(v.name);
            }
        }
    }
    
    private void initOutputVariables(Method m, PrologMethod pm) {
        outputVariables = new Vector<>();
        if (pm.signature().isEmpty()) {
            Type returnType = m.getGenericReturnType();
            if (Iterable.class.equals(m.getReturnType())) {
                multipleResult = true;
                returnType = ((ParameterizedType)returnType).getActualTypeArguments()[0];
            }
            else {
                multipleResult = false;
            }
            if (returnType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)returnType;
                if (Cons.class.equals(pt.getRawType())) {
                    Type t = pt;
                    while (Nil.class.isAssignableFrom((Class<?>)pt.getRawType())) {
                        pt = (ParameterizedType)t;
                        Type head = pt.getActualTypeArguments()[0];
                        outputVariables.add(((TypeVariable<?>)head).getName());
                        t = pt.getActualTypeArguments()[1];
                    }
                }
                else {
                    for (Type t : pt.getActualTypeArguments()) {
                        outputVariables.add(((TypeVariable<?>)t).getName());
                    }
                }
            }  
            else if(returnType.equals(Boolean.class)) {
                return;
            }
            else {
                outputVariables.add(((TypeVariable<?>)returnType).getName());
            }
            for (String s : outputVariables) {
                if (inputVariables.contains(s)) {
                    keepSubstitutions = true;
                }
            }
        }
        else {
            Parser p = new Parser(pm.signature());        
            SignatureExpr signature = p.parseSignature();
            java.util.List<VariableExpr> variables = signature.outputTree.variables;            
            for (VariableExpr v : variables) {
                outputVariables.add(v.name);
            }        
            multipleResult = signature.multipleResult;
        }
    }
    
    
    
    
    public Object dispatch(alice.tuprologx.pj.engine.PJProlog _engine, Object... args) {
	try {                            
            Term<?> theGoal = buildGoal(args);  
            if (trace) {
                System.out.println("theory = "+_engine.getTheory());
                System.out.println("goal = "+theGoal.marshal());
            }
            return buildSolution(theGoal ,_engine);
        }
	catch (Exception e) {e.printStackTrace();return null;}
    }
    
    /*
     * unused
     *
    private Object buildSolution2(Term<?> theGoal, PJProlog _engine) throws NoSolutionException {
        try {                    
            if (multipleResult) {
                Iterable<? extends PrologSolution<?,Cons<?,?>>> answer = _engine.solveAll(theGoal);                            
                Vector<Object> resultList = new Vector<Object>();
                if (!answer.iterator().hasNext() && exceptionOnFailure) {
                    throw new NoSolutionException();
                }
                else if (!answer.iterator().hasNext() && !exceptionOnFailure) {
                    return (outputVariables.size()==0) ? Boolean.FALSE : null;
                }
                
                for (PrologSolution<?, Cons<?, ?>> si : answer) {                        
                    Cons<?,?> res = si.getSolution();
                    int size = outputVariables.size();                    
                    if (size == 0) {
                        resultList.addAt(si.isSuccess() ? Boolean.TRUE : Boolean.FALSE);
                    }
                    else {
                        Term<?>[] termList = new Term<?>[size];
                        int i = 0;
                        int pos;
                        for (Term<?> t : res) {
                            if ((pos = outputVariables.indexOf(variableNames.get(i)))!=-1) {
                                termList[pos] = formatTerm(t);
                            }
                            i++;
                        }
                        if (termList.length > 1) {
                            resultList.addAt(Cons.make(res.getName(),termList));
                        }
                        else {
                            resultList.addAt(termList[0]);
                        }
                    }                    
                }                                
                return resultList;
            }        
            else { 
                PrologSolution<?,? extends Cons<?,?>> si = _engine.solve(theGoal);                            
                if (!si.isSuccess() && exceptionOnFailure) {
                    throw new NoSolutionException();
                }
                else if (!si.isSuccess() && !exceptionOnFailure) {
                    return (outputVariables.size()==0) ? Boolean.FALSE : null;
                }
                Object result = null;
                if (!si.isSuccess() && exceptionOnFailure) {
                    throw new NoSolutionException();
                }                                
                Cons<?,?> res = si.getSolution();
                int size = outputVariables.size();                
                if (size == 0) {
                    result = si.isSuccess() ? Boolean.TRUE : Boolean.FALSE;
                }
                else {
                    Term<?>[] termList = new Term<?>[size];
                    int i = 0;
                    int pos = 0;
                    for (Term<?> t : res) {
                        if ((pos = outputVariables.indexOf(variableNames.get(i)))!=-1) {
                            termList[pos] = formatTerm(t);
                        }
                        i++;
                    }
                    if (termList.length > 1) {
                            result = Cons.make(res.getName(),termList);                              
                        }
                        else {
                            result = termList[0];                              
                        }
                }                                    
                return result;
            }
        }
        catch (alice.tuprolog.PrologException ex) {
            return null;
        }
    }
    */
    
    private Object buildSolution(Term<?> theGoal, PJProlog _engine) throws NoSolutionException {
        try {                    
            if (multipleResult) {
                final Iterable<? extends PrologSolution<?,Cons<?,?>>> answer = _engine.solveAll(theGoal);                            
                
                final Iterator<? extends PrologSolution<?,Cons<?,?>>> _result = answer.iterator();
                if (!_result.hasNext() && exceptionOnFailure) {
                    throw new NoSolutionException();
                }
                else if (!_result.hasNext() && !exceptionOnFailure) {
                    return (outputVariables.isEmpty()) ? Boolean.FALSE : null;
                }
                class SolutionIterator implements Iterator<Object> {
                    
                    @Override
                    public void remove() {throw new UnsupportedOperationException();}
                    @Override
                    public Object next() {
                        PrologSolution<?,Cons<?,?>> si = _result.next(); 
                        Cons<?,?> res = null;
                        try {
                             res = si.getSolution();
                        }
                        catch (Exception e) {
                            throw new UnsupportedOperationException(e);
                        }
                        int size = outputVariables.size();                    
                        if (size == 0) {
                            return si.isSuccess() ? Boolean.TRUE : Boolean.FALSE;   
                        }
                        else {
                            Term<?>[] termList = new Term<?>[size];
                            int i = 0;
                            for (Term<?> t : res) {
                                int pos;
                                if ((pos = outputVariables.indexOf(variableNames.get(i)))!=-1) {
                                    termList[pos] = formatTerm(t);
                                }
                                i++;
                            }
                            return termList.length > 1 ? Cons.make(res.getName(), termList) : termList[0];
                        }
                    }
                    @Override
                    public boolean hasNext() {
                        return _result.hasNext();
                    }
                }
                return (Iterable<Object>) SolutionIterator::new;
            }        
            else { 
                PrologSolution<?,? extends Cons<?,?>> si = _engine.solve(theGoal);                            
                if (!si.isSuccess() && exceptionOnFailure) {
                    throw new NoSolutionException();
                }
                else if (!si.isSuccess() && !exceptionOnFailure) {
                    return (outputVariables.isEmpty()) ? Boolean.FALSE : null;
                }
                if (!si.isSuccess() && exceptionOnFailure) {
                    throw new NoSolutionException();
                }                                
                Cons<?,?> res = si.getSolution();
                int size = outputVariables.size();
                Object result = null;
                if (size == 0) {
                    result = si.isSuccess() ? Boolean.TRUE : Boolean.FALSE;
                }
                else {
                    Term<?>[] termList = new Term<?>[size];
                    int i = 0;
                    int pos = 0;
                    for (Term<?> t : res) {
                        if ((pos = outputVariables.indexOf(variableNames.get(i)))!=-1) {
                            termList[pos] = formatTerm(t);
                        }
                        i++;
                    }
                    result = termList.length > 1 ? Cons.make(res.getName(), termList) : termList[0];
                }                                    
                return result;
            }
        }
        catch (alice.tuprolog.PrologException ex) {
            return null;
        }
    }
    
    private Term<?> formatTerm(Term<?> t) {
        return t instanceof Var<?> && !keepSubstitutions ? ((Var<?>) t).getValue() : t;
    }
}

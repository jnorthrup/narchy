package alice.tuprologx.pj.engine;

import alice.tuprologx.pj.annotations.PrologMethod;
import alice.tuprologx.pj.annotations.Termifiable;
import alice.tuprologx.pj.annotations.WithTermifiable;
import alice.tuprologx.pj.meta.PrologMetaClass;
import alice.tuprologx.pj.meta.PrologMetaField;
import alice.tuprologx.pj.meta.PrologMetaMethod;
import alice.tuprologx.pj.model.*;
import javassist.util.proxy.MethodHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class PJ implements MethodHandler {
    static int reentrant;
    private PJ() {}

    private static final PJ pj = new PJ();

    private static final ArrayList<PJProlog> _stack = new ArrayList<>();
    private static int current = -1;

    static {pushEngine();}

    static private void pushEngine() {
        if (_stack.size() <= ++current) {
            PJProlog engine = new PJProlog();
            alice.tuprologx.pj.lib.PJLibraryNew jl = new alice.tuprologx.pj.lib.PJLibraryNew();
            engine.loadLibrary(jl);
            _stack.add(engine);
        }
    }

    static private void popEngine() {
        current--;
    }

    static private PJProlog engine() {
        return _stack.get(current);
    }
    

    public static <T> T newInstance(Class<?> cl) throws Exception {
        return PJ.newInstance(cl,null);
    }

    @SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<?> cl, Theory init) throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
            J2PProxyFactory pf = new J2PProxyFactory();
            pf.setSuperclass(cl.isInterface() ? Object.class : cl);
            pf.setInterfaces(cl.isInterface() ?
                new Class[] {cl, PrologObject.class} :
                new Class[] {PrologObject.class});
            pf.setHandler(pj);
            PrologObject po = (PrologObject)pf.create(new Class[0],new Object[0]);
            PrologMetaClass metaClass = po.getMetaPrologClass();
            for (PrologMetaField f : metaClass.getPrologFields()) {
                f.init(po);
            }
            if (init != null) {                
                po.setTheory(init);
            }		
            return (T)po;
    }

    /**
     * External interface for calling a Prolog method - if a call is reentrant (e.g.
     * because it is coming from the PJLibrary and because the context of the current
     * Prolog call needs to be saved) a new engine is pushed onto the stack.
     */
    public static Object call(Object receiver, Method method, Object[] args, boolean reentrant) {
        if (reentrant || PJ.reentrant > 0)
            pushEngine();
        try {
            return pj.invoke(receiver, method, method, args);
        }
        catch (Exception e) {
            return null;
        }
        finally {
            if (reentrant || PJ.reentrant > 0)
                popEngine();
        }
    }

    @Override
    public Object invoke(Object receiver, Method method, Method proceed, Object[] args) throws IllegalAccessException, IllegalArgumentException, java.lang.reflect.InvocationTargetException, ClassNotFoundException, alice.tuprolog.InvalidTheoryException {
        if (method.getDeclaringClass().equals(PrologObject.class)) {
            return invokeInternal(receiver, method, args); 
        }
        if (method.getAnnotation(PrologMethod.class) == null) {
            return proceed.invoke(receiver, args);
        }

        if (PJ.reentrant > 0) {
            pushEngine();
        }
        PJ.reentrant++;
        
        Object result = null;
        try {

        
        PrologObject po = (PrologObject)receiver;
        PrologMetaClass metaClass = getMetaClass(receiver);
        Theory class_t = metaClass.getTheory();
        
        
        PrologInvocationContext ctx = new PrologInvocationContext(method);
        /* theory = class_theory + method_theory + fields_theories */
        WithTermifiable withTermifiable = metaClass.getJavaClass().getAnnotation(WithTermifiable.class);
        if (withTermifiable != null) {
            for (String className : withTermifiable.value()) {
                Class<?> klass = Class.forName(className);
                String termName = klass.getAnnotation(Termifiable.class).predicate();
                JavaTerm.hashtable.put(termName, klass);
            }
        }
        engine().setTheory(class_t);
        if (receiver != null) {
            JavaObject<Object> jo = new JavaObject<>(receiver);
            Compound1<JavaObject<?>> head = new Compound1<>("this", jo);
            Clause<Compound1<?>, Nil> ct = new Clause<>(head, null);
            ArrayList<Clause<?,?>> list = new ArrayList<>();
            list.add(ct);
            Theory t = new Theory(list);
            engine().addTheory(t);
        }
        
        for (PrologMetaMethod metaMethod : metaClass.getPrologMethods()) {
            Theory method_t = metaMethod.getTheory();
            engine().addTheory(method_t);
        }

        for (PrologMetaField metaField : metaClass.getPrologFields()) {
            Theory field_t = metaField.getTheory();
            engine().addTheory(field_t);
        }
        
        Theory t = po.getTheory();
        if (t != null)
            engine().addTheory(t);
        
        
        
        result = ctx.dispatch(engine(),args);
    }
    finally {
        
    }
        return result;
    }

    public static Object invokeInternal(Object receiver, Method method, Object... args) {
        switch (method.getName()) {
            case "getMetaPrologClass":
                return getMetaClass(receiver);
            case "getTheory":
                return getTheory(receiver);
            case "setTheory":
                return setTheory(receiver, (Theory) args[0]);
        }
        return null;                        
    }

    private static PrologMetaClass getMetaClass(Object o) {
        try {
            java.lang.reflect.Field metaclass_field = o.getClass().getField("_meta$Prolog$Class");
            PrologMetaClass metaClass = (PrologMetaClass)metaclass_field.get(o);                
            if (metaClass == null) {
                metaClass = new PrologMetaClass(o.getClass());
                metaclass_field.set(o,metaClass);                
            }
            return metaClass;
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }            
    }
    
    private static Theory getTheory(Object o) {
        try {
            java.lang.reflect.Field metaclass_field = o.getClass().getField("_prolog$Theory");
            return (Theory)metaclass_field.get(o);
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }            
    }
    
    private static Void setTheory(Object o, Theory t) {
        try {
            java.lang.reflect.Field metaclass_field = o.getClass().getField("_prolog$Theory");
            metaclass_field.set(o, t);  
            return null;
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }            
    }
    
    public static alice.tuprolog.Struct registerJavaObject(Object o) {
        
        try {
            return engine().getPJLibrary().register(o);
        }
        catch (Exception e) {
            return null;
        }
    }
    
    public static Object getRegisteredJavaObject(alice.tuprolog.Struct t) {
        
        try {
            Object obj = engine().getPJLibrary().getRegisteredObject(t);
            return obj == null ? engine().getPJLibrary().getRegisteredDynamicObject(t) : obj;
        }
        catch (Exception e) {
            return null;
        }
    }

    public static void assertClause(PrologObject po, alice.tuprolog.Term clause) {
        try {
            pushEngine();
            engine().setTheory(po.getTheory());
            Compound1<Term<?>> goal = new Compound1<>("assert", Term.unmarshal(clause));
            engine().solve(goal);
            po.setTheory(engine().getTheory());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            popEngine();
        }
    }

    public static void retractClause(PrologObject po, alice.tuprolog.Term clause) {
        try {
            pushEngine();
            engine().setTheory(po.getTheory());
            Compound1<Term<?>> goal = new Compound1<>("retract", Term.unmarshal(clause));
            engine().solve(goal);
            po.setTheory(engine().getTheory());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            popEngine();
        }
    }

    public static void retractAllClauses(PrologObject po, alice.tuprolog.Term clause) {
        try {
            pushEngine();
            engine().setTheory(po.getTheory());
            alice.tuprolog.Struct goal = new alice.tuprolog.Struct("retractall", clause);
            System.out.println(goal);
            engine().engine.solve(goal);
            po.setTheory(engine().getTheory());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            popEngine();
        }
    }
}


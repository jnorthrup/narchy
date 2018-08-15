/*
 * PrologCompiler.java
 *
 * Created on 14 marzo 2007, 11.57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.annotations.processing;

import alice.tuprologx.pj.annotations.PrologClass;
import alice.tuprologx.pj.annotations.PrologField;
import alice.tuprologx.pj.annotations.PrologMethod;
import alice.tuprologx.pj.annotations.parser.Parser;
import alice.tuprologx.pj.annotations.parser.PrologTree.PredicateExpr;
import alice.tuprologx.pj.annotations.parser.PrologTree.SignatureExpr;
import alice.tuprologx.pj.annotations.parser.PrologTree.VariableExpr;
import alice.tuprologx.pj.model.Term;
import alice.tuprologx.pj.model.Theory;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementScanner6;
import javax.lang.model.util.Types;
import java.util.*;

import static alice.tuprologx.pj.annotations.processing.ProcessorMessages.*;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 *
 * @author Maurizio
 */
public class PJAnnotationProcessor extends ElementScanner6<Void,Void> implements Processor {   
    
    public static final String[] supportedAnnotations = {"alice.tuprologx.pj.annotations.*"};
    
    
    
    private DeclaredType nilType;


	private DeclaredType iterableType;


	private DeclaredType j2PExceptionType;


	private DeclaredType boolType;
    
    
    private TypeElement teBool;


	private TypeElement teCompound2;


	private TypeElement teCons;


	private TypeElement teNil;


	private TypeElement teTerm;


	private TypeElement teVar;


	private TypeElement teIterable;


	private TypeElement teJ2PException;    
    private ProcessingEnvironment env;
    private Theory classTheory;
    
    private TypeElement enclosingDeclaration;
    private PredicateExpr predicate;
    private SignatureExpr signature;
    

    @Override
    public Void visitType(TypeElement d, Void v) {
        TypeElement oldDecl = enclosingDeclaration;
        enclosingDeclaration = d;        
        String theory="";
        PrologClass pc = d.getAnnotation(PrologClass.class);
        for (String s : pc.clauses()) {
            theory+=s+ '\n';
        }
        classTheory = checkTheory(theory);
        if (classTheory == null) { 
            return null;
        }                
        checkAbstractOrInterface(d);
        Collection<? extends Element> mDecls = d.getEnclosedElements();
        for (Element e : mDecls) {
            e.accept(this,null);
        }
        enclosingDeclaration=oldDecl;
        return null;
    }

    @Override
    public Void visitExecutable(ExecutableElement d, Void v) {
        PrologMethod pm = d.getAnnotation(PrologMethod.class);                
        if (pm != null) {                
            String theory = "";
            for (String s : pm.clauses()) {
                theory = theory + s + '\n';
            }            
            
            checkPredicate(pm.predicate(),d);
            checkSignature(pm.signature(),d);
            checkThrowClause(pm.exceptionOnFailure(),d);
            checkTypes(pm, d);            
        }           
        return null;
    }

    @Override
    public Void visitVariable(VariableElement v, Void arg1) {
        PrologField pf = v.getAnnotation(PrologField.class);
        String init = pf.init();
        if (init != "") {
            alice.tuprolog.Term t = null;
            try {                
                t = alice.tuprolog.Parser.parseSingleTerm(init);
            }
            catch (Throwable tw) {
                env.getMessager().printMessage(ERROR,ERR_BAD_VAR_INIT,v);
                return null;
            }
            TypeElement te = env.getElementUtils().getTypeElement(Term.unmarshal(t).getClass().getCanonicalName());            
            if (!env.getTypeUtils().isSubtype(te.asType(), v.asType()))
                env.getMessager().printMessage(ERROR,ERR_BAD_TYPE_IN_VAR_INIT,v);
        }
        return null;
    }
    
    private void checkAbstractOrInterface(TypeElement d) {
        if (!(d.getKind() == ElementKind.INTERFACE || (d.getKind() == ElementKind.CLASS && d.getModifiers().contains(Modifier.ABSTRACT)))) {
            env.getMessager().printMessage(ERROR,ERR_PROLOG_CLASS_NOT_ALLOWED,d);
        }
    }
    /*
     * unused
    private void checkAbstractOrInterface(ExecutableElement d) {
        if (!d.getModifiers().contains(Modifier.ABSTRACT)) {
            env.getMessager().printMessage(ERROR,ERR_PROLOG_METHOD_NOT_ALLOWED,d);
        }
    }
     */
    private void checkPredicate(String p,ExecutableElement md) {                        
        if (p.isEmpty()) {
            return;
        } 
        try {
            predicate = new Parser(p).parsePredicate();            
        }
        catch (Exception e) {
            env.getMessager().printMessage(ERROR,ERR_PREDICATE_MALFORMED,md);
        }
    }
    
    private void checkSignature(String s,ExecutableElement md) {                        
        if (s.isEmpty()) {
            return;
        }
        try {
            signature = new Parser(s).parseSignature();            
        }
        catch (Exception e) {
            env.getMessager().printMessage(ERROR,ERR_SIGNATURE_MALFORMED,md);
        }
    }
    
    private static Theory checkTheory(String theory) {
        Theory t = new Theory(theory);
        /*if (t == null) {
            env.getMessager().printMessage(ERROR,ERR_THEORY_INVALID,enclosingDeclaration);
        } */       
        return t;
    }
    
    private void checkThrowClause(boolean canRaiseException, ExecutableElement md) {                
        for (TypeMirror t : md.getThrownTypes()) {
            if (env.getTypeUtils().isSameType(t,j2PExceptionType)) {
                return;                
            }
        }
        if (canRaiseException) {
            env.getMessager().printMessage(WARNING,ERR_THROW_MISSING,md);
        }
    }
    
    private void checkTypes(PrologMethod pm, ExecutableElement md) {                
        if (pm.types().length == 0 || predicate == null || (pm.types().length > 0 &&  pm.types().length != predicate.variables.size())) {
            env.getMessager().printMessage(WARNING,ERR_CANT_CHECK_TYPES,md);
        }
        else if (signature != null && predicate != null) {
            checkArguments(md,pm);
            checkReturnType(pm.keepSubstitutions(),md,pm);                        
        }                        
        else if (signature == null && predicate != null) {
            if (!pm.keepSubstitutions()) {
                env.getMessager().printMessage(WARNING,ERR_KEEP_SUBST_DEFAULTS_TRUE,md);
            }
            checkTypeVariables(md,pm);
        }
    }
    
    private void checkReturnType(boolean keepSubstitutions, ExecutableElement md, PrologMethod pm) {                        
        boolean isMultiple = signature.multipleResult;        
        
        
        
        Types types = env.getTypeUtils();
        TypeMirror returnType = md.getReturnType();        
        if (isMultiple && !types.isSameType(types.erasure(iterableType),types.erasure(returnType))) {
            env.getMessager().printMessage(ERROR,ERR_RETURN_MULTIPLE_REQUIRED,md);
            return;
        }        
        if (types.isSameType(types.erasure(iterableType),types.erasure(returnType))) {
            returnType = ((DeclaredType)returnType).getTypeArguments().get(0);            
        }        
        
        TypeMirror requiredType = null;
        
        if (signature.outputTree.variables.isEmpty()) {
            requiredType=boolType;
        }
        else {
            Vector<TypeMirror> parameters = new Vector<>();
            for (VariableExpr i : signature.outputTree.variables) {                
                VariableExpr decl = predicate.variables.get(predicate.variables.indexOf(i));
                parameters.add(formType(new TypeParser(pm.types()[predicate.variables.indexOf(i)]).parseType(),decl.annotations,keepSubstitutions));
            }                                                            
            if (signature.outputTree.variables.size() == 1) {                
                requiredType = parameters.get(0);
            }
            else if (signature.outputTree.variables.size() == 2) {
                requiredType = types.getDeclaredType(teCompound2,parameters.toArray(new TypeMirror[0]));
            }                    
            else {
                requiredType = nilType;
                for (int i = parameters.size()-1; i>=0;i--) {
                    requiredType = types.getDeclaredType(teCons, requiredType,parameters.get(i));
                }                
            }
        }
        if (!types.isSameType(returnType,requiredType)) {                    
            env.getMessager().printMessage(ERROR,ERR_RETURN_TYPE_REQUIRED+". \nRequired : "+requiredType+"\nFound : " + returnType,md);
        }        
    }
    
    private void checkArguments(ExecutableElement md, PrologMethod pm) {
        int i = 0;
        for (VariableElement pd : md.getParameters()) {
            checkArgument(pd,i,pm.types());
            i++;
        }
    }
    
    private void checkArgument(VariableElement pd, int pos, String... types) {
        String varName = signature.inputTree.variables.get(pos).name;
        java.util.List<Character> annotations = null;
        TypeMirror baseType = null;
        int i = 0;
        for (VariableExpr v : predicate.variables) {
            if (v.name.equals(varName)) {
                annotations = v.annotations;
                baseType = new TypeParser(types[i]).parseType();
            }
            i++;
        }                
        if (!env.getTypeUtils().isSameType(formType(baseType,annotations,true),pd.asType())) {        
            env.getMessager().printMessage(ERROR,ERR_ARG_BAD_TYPE+ ' ' +formType(baseType,annotations,true),pd);
        }
    }
    
    private void checkTypeVariables(ExecutableElement md, PrologMethod pm) {
        int i = 0;
        for (TypeParameterElement tvar : md.getTypeParameters()) {
            checkTypeVariable(tvar,i,pm.types());
            i++;
        }
    }
    
    private void checkTypeVariable(TypeParameterElement pd, int pos, String... types) {
        String varName = pd.getSimpleName().toString();
        java.util.List<Character> annotations = null;
        TypeMirror baseType = null;
        int i = 0;
        for (VariableExpr v : predicate.variables) {
            if (v.name.equals(varName)) {
                annotations = v.annotations;
                baseType = new TypeParser(types[i]).parseType();
            }
            i++;
        }                
        if (!env.getTypeUtils().isSameType(formType(baseType,annotations,true),pd.getBounds().get(0))) {        
            env.getMessager().printMessage(ERROR,ERR_TVAR_BAD_BOUND+ ' ' +formType(baseType,annotations,true),pd);
        }
    }
    
    private TypeMirror generalize(TypeMirror baseType) {        
        if (baseType instanceof DeclaredType) {
            DeclaredType dt = (DeclaredType)baseType;            
            Vector<TypeMirror> types = new Vector<>();
            for (TypeMirror t : dt.getTypeArguments()) {
                types.add(env.getTypeUtils().getWildcardType(env.getTypeUtils().getDeclaredType(teTerm, generalize(t)),null));
            }
            return env.getTypeUtils().getDeclaredType((TypeElement)dt.asElement(),types.toArray(new TypeMirror[0]));
        }
        else {
            return baseType;
        }
    }
    
    private TypeMirror formType(TypeMirror baseType, java.util.List<Character> annotations, boolean keepSubstitutions) {
        TypeMirror formedType = null;        
        
        if (annotations.contains('@') && !annotations.contains('?')) {        
            formedType = baseType;
        }
        else if (annotations.contains('!') && annotations.contains('?')) {            
            formedType = env.getTypeUtils().getDeclaredType(teTerm,baseType);     
        }
        else if (annotations.contains('!') && annotations.contains('-') && keepSubstitutions ) {                    
            formedType = env.getTypeUtils().getDeclaredType(teVar,baseType);     
        }
        else if (annotations.contains('!') && annotations.contains('-') && !keepSubstitutions ) {
            formedType = baseType;
        }
        else if (annotations.contains('!') && annotations.contains('+')) {
            formedType = baseType;
        }
        else {
        
            TypeMirror generalizedType = generalize(baseType);
            if (annotations.contains('+') || (annotations.contains('-') && !keepSubstitutions)) {                
        
                formedType = generalizedType;
            }
            else if (annotations.contains('-') && keepSubstitutions) {
        
                DeclaredType dt = (DeclaredType)baseType;
                TypeMirror wt = dt.getTypeArguments().isEmpty() ? baseType : env.getTypeUtils().getWildcardType(generalizedType,null);
                formedType = env.getTypeUtils().getDeclaredType(teVar,wt);     
            }
            else if (annotations.contains('?')) {                
                DeclaredType dt = (DeclaredType)baseType;
                TypeMirror wt = dt.getTypeArguments().isEmpty() ? baseType : env.getTypeUtils().getWildcardType(generalizedType,null);
                formedType = env.getTypeUtils().getDeclaredType(teTerm,wt);     
            }
        }
        return formedType;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Collection<? extends Element> decls = env.getElementsAnnotatedWith(PrologClass.class);        
        for (Element d : decls) {
            scan(d,null);
        }
        return true;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return null;
    }

    /*
     * unused
    private boolean is(TypeMirror _this, DeclaredType _that) {
        DeclaredType dt = (DeclaredType)_this;
        if (dt.asElement().equals(_that.asElement())) {        
            return !env.getTypeUtils().isSameType(_this,termType);            
        }
        else {
            return false;
        }
    }
    */
    /*
     * unused
    private boolean contains(TypeMirror containing, DeclaredType contained) {
        if (containing instanceof DeclaredType) {  
            DeclaredType dt = (DeclaredType)containing;
            if (is(dt,contained)) {
                return true;
            }
            for (TypeMirror t : dt.getTypeArguments()) {
                if (contains(t,contained)) {
                    return true;
                }
            }
        }
        else if (containing instanceof WildcardType) {
            WildcardType wt = (WildcardType)containing;
            if (wt.getExtendsBound()!=null) {
                return contains(wt.getExtendsBound(),contained); 
            }
            else if (wt.getSuperBound()!=null) {
                return contains(wt.getSuperBound(),contained); 
            }
            else {
                return false;
            }
        }
        return false;        
    }
    */
    @Override
    public void init(ProcessingEnvironment processingEnv) {
        env = processingEnv;                
        
        
        teCompound2 = env.getElementUtils().getTypeElement("alice.tuprologx.pj.model.Compound2");
        teCons = env.getElementUtils().getTypeElement("alice.tuprologx.pj.model.Cons");
        teNil = env.getElementUtils().getTypeElement("alice.tuprologx.pj.model.Nil");
        teTerm = env.getElementUtils().getTypeElement("alice.tuprologx.pj.model.Term");
        teVar = env.getElementUtils().getTypeElement("alice.tuprologx.pj.model.Var");
        teBool = env.getElementUtils().getTypeElement("java.lang.Boolean");
        
        teIterable = env.getElementUtils().getTypeElement("java.lang.Iterable");
        teJ2PException = env.getElementUtils().getTypeElement("alice.tuprologx.pj.engine.NoSolutionException");                        
        WildcardType wt = env.getTypeUtils().getWildcardType(null,null);
        
        
        
        
        nilType = env.getTypeUtils().getDeclaredType(teNil);
        
        
        boolType = env.getTypeUtils().getDeclaredType(teBool);        
        
        iterableType = env.getTypeUtils().getDeclaredType(teIterable,wt);        
        j2PExceptionType = env.getTypeUtils().getDeclaredType(teJ2PException);        
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_6;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return new java.util.TreeSet<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new java.util.TreeSet<>(Arrays.asList(supportedAnnotations));
    }
    
    private class TypeParser {
    
        final StringTokenizer lexer;
        String currentToken;
    
        private TypeParser(String s) {
            lexer = new StringTokenizer(s,"<,>",true);
            currentToken = lexer.nextToken();
        }

        public TypeMirror parseType() {
            boolean wildcard = false;
            if (currentToken.equals("?")) {
                wildcard = true;
            }            
            String baseName = currentToken.indexOf('.') != -1 ? currentToken : "alice.tuprologx.pj.model."+currentToken;
            try {
                currentToken = lexer.nextToken();
            }
            catch (Exception e) {
                return env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement(baseName));
            }
            TypeMirror[] params = parseTypes();
            return !wildcard ? env.getTypeUtils().getDeclaredType(env.getElementUtils().getTypeElement(baseName), params) : env.getTypeUtils().getWildcardType(null, null);
        }

        public TypeMirror[] parseTypes() {
            if (currentToken.equals("<")) {
                Vector<TypeMirror> types = new Vector<>();
                while (!currentToken.equals(">")) {
                    currentToken = lexer.nextToken();                
                    types.add(parseType());
                }        
                try {
                    currentToken = lexer.nextToken();                
                }
                catch (Exception e) {}
                return types.toArray(new TypeMirror[0]);
            }
            return new TypeMirror[0];
        }
    }
}



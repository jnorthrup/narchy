/*
 * CompilerMessages.java
 *
 * Created on 14 marzo 2007, 14.16
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.annotations.processing;

/**
 *
 * @author Maurizio
 */
public interface ProcessorMessages {
    
    String ERR_PROLOG_CLASS_NOT_ALLOWED = "Annotation 'PrologClass' only applies to an abstract class/interface declaration";
    
    String ERR_PROLOG_METHOD_NOT_ALLOWED = "Annotation 'PrologMethod' only applies to an abstract/interface method declaration";
    
    String ERR_THEORY_INVALID = "Attribute 'clauses' does not contain a valid Prolog theory";
    
    String ERR_PREDICATE_MALFORMED = "Malformed attribute 'predicate'";
    
    String ERR_SIGNATURE_MALFORMED = "Malformed attribute 'signature'";
    
    String ERR_RETURN_MULTIPLE_REQUIRED = "The return type of a multiple-output Prolog method must be a subtype of java.lang.Iterable<?>";
    
    String ERR_SELECT_EMPTY = "Relational style requires at least one attribute not to be marked as @HIDE";
    
    String ERR_CANT_CHECK_TYPES = "Cannot check Prolog method arguments/return type signatures";
    
    String ERR_SELECT_TOO_MANY = "Too many result terms specified in the 'select' attribute";
    
    String WARN_SELECT_IGNORED = "Ignoring the 'select' attribute";
    
    String ERR_PARAMS_NUMBER_WRONG = "Wrong number of method parameters with respect to the matching Prolog clause and the selected invocation style";
    
    String ERR_MAPPING_WRONG = "Functional style requires a '$0' item to be specified in 'link'";
    
    String ERR_BAD_ITEM_IN_MAPPING = "'link' refers to a non-existent Prolog method argument";
    
    String ERR_THROW_MISSING = "Exception 'alice.tuprologx.j2p.engine.NoSolutionException' not declared to be thrown while 'exceptionOnFailure' attribute set";
    
    String ERR_RETURN_TYPE_REQUIRED = "Return type doesn not match with the specified argument annotations";
    
    String ERR_ARG_BAD_TYPE = "Argument type not compatible with the specified annotation values";
    
    String ERR_TVAR_BAD_BOUND = "Bound of type variable not compatible with the specified annotation values";
    
    String ERR_RET_BAD_TYPE = "Return type not compatible with the specified annotation values";
    
    String ERR_KEEP_SUBST_DEFAULTS_TRUE = "Forcing 'keepSubstitution' to 'true' because 'signature' is missing";
    
    String ERR_BAD_VAR_INIT = "Bad initializer for PrologField";
    
    String ERR_BAD_TYPE_IN_VAR_INIT = "Type mismatch in PrologField initializer";
}

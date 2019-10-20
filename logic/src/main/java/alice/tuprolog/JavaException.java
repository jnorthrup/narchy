package alice.tuprolog;

import jcog.Util;

/**
 * @author Matteo Iuliani
 */
public class JavaException extends Throwable {
	private static final long serialVersionUID = 1L;
    
    private final Throwable e;

    public JavaException(Throwable e) {
        this.e = e;
    }

    public Struct getException() {

        var java_exception = e.getClass().getName();

        var cause = e.getCause();
        var causeTerm = cause != null ? new Struct(cause.toString()) : new NumberTerm.Int(0);

        var message = e.getMessage();
        var messageTerm = message != null ? new Struct(message) : new NumberTerm.Int(0);


        var elements = e.getStackTrace();
        
        return new Struct(java_exception, causeTerm, messageTerm,
                new Struct(Util.map(e -> new Struct(e.toString()), new Term[elements.length], e.getStackTrace())));
    }

}

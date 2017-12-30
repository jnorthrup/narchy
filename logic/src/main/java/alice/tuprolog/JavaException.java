package alice.tuprolog;

import jcog.Util;

/**
 * @author Matteo Iuliani
 */
public class JavaException extends Throwable {
	private static final long serialVersionUID = 1L;
    // eccezione Java che rappresenta l'argomento di java_throw/1
    private final Throwable e;

    public JavaException(Throwable e) {
        this.e = e;
    }

    public Struct getException() {
        // java_exception
        String java_exception = e.getClass().getName();
        // Cause
        Throwable cause = e.getCause();
        Term causeTerm = cause != null ? new Struct(cause.toString()) : new Int(0);
        // Message
        String message = e.getMessage();
        Term messageTerm = message != null ? new Struct(message) : new Int(0);
        // StackTrace

        StackTraceElement[] elements = e.getStackTrace();
        // return
        return new Struct(java_exception, causeTerm, messageTerm,
                new Struct(Util.map(e -> new Struct(e.toString()), new Term[elements.length], e.getStackTrace())));
    }

}

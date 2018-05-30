/*Castagna 06/2011*/
package alice.tuprolog.event;

import java.util.EventObject;

public class ExceptionEvent extends EventObject{
	private static final long serialVersionUID = 1L;
	private final String msg;

	public ExceptionEvent(Object source, String msg_) {
		super(source);
		msg=msg_;
	}

	public String getMsg(){
		return msg;
	}

}
/**/
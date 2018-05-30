package alice.tuprolog.event;

import alice.tuprolog.lib.UserContextInputStream;

import java.util.EventObject;

public class ReadEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final alice.tuprolog.lib.UserContextInputStream stream;
	
	public ReadEvent(UserContextInputStream str) {
		super(str);
		this.stream = str;
	}

	public UserContextInputStream getStream()
	{
		return this.stream;
	}

}

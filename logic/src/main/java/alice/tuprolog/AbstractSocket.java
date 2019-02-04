package alice.tuprolog;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

public abstract class AbstractSocket extends Term{
	private static final long serialVersionUID = 1L;
	public abstract boolean isClientSocket();
	
	public abstract boolean isServerSocket();
	
	public abstract boolean isDatagramSocket();
	
	public abstract Object getSocket();
	
	public abstract InetAddress getAddress();


	@Override
	public boolean isEmptyList() {
		
		return false;
	}

	@Override
	public boolean isAtom() {
		
		return false;
	}

	@Override
	public boolean isCompound() {
		
		return false;
	}

	@Override
	public boolean isAtomic() {
		
		return false;
	}

	@Override
	public boolean isList() {
		
		return false;
	}

	@Override
	public boolean isGround() {
		
		return false;
	}

	@Override
	public boolean isGreater(Term t) {
		
		return false;
	}
	@Override
	public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
		
		return false;
	}

	@Override
	public boolean isEqual(Term t) {
		return t == this;
	}


	@Override
	Term copy(Map<Var, Var> vMap, Map<Term, Var> substMap) {
		return this;
	}


}



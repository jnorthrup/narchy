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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAtom() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCompound() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAtomic() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isList() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGround() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGreater(Term t) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEqual(Term t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Term term() {
		return this;
	}


	@Override
	void resolveTerm(long count) {

	}

	@Override
	Term copy(Map<Var, Var> vMap, int idExecCtx) {
		return this;
	}

	@Override
	Term copy(Map<Var, Var> vMap, Map<Term, Var> substMap) {
		return this;
	}


}



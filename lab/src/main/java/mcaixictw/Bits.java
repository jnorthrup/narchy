package mcaixictw;

/**
 * representation of a bit string
 */
public class Bits extends BooleanArrayList {

	public Bits() {
		super();
	}


	public Bits one() {
		add(true);
		return this;
	}

	public Bits zero() {
		add(false);
		return this;
	}


	public Bits rand() {
		add(McAIXIUtil.randSym());
		return this;
	}

	public Bits rand(int length) {
		for (int i = 0; i < length; i++) {
			add(McAIXIUtil.randSym());
		}
		return this;
	}

}
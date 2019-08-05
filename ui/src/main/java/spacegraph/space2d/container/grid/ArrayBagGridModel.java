package spacegraph.space2d.container.grid;

import jcog.pri.Prioritizable;
import jcog.pri.bag.impl.ArrayBag;
import org.jetbrains.annotations.Nullable;

public class ArrayBagGridModel<X extends Prioritizable> implements GridModel<X> {

	final ArrayBag<?,X> bag;

	public ArrayBagGridModel(ArrayBag<?, X> bag) {
		this.bag = bag;
	}

	@Override
	public int cellsX() {
		return 1;
	}

	@Override
	public int cellsY() {
		return bag.capacity();
	}

	@Nullable
	@Override
	public X get(int x, int y) {
		return x==0 ? bag.get(y) : null;
	}
}

package spacegraph.space2d.container;

import jcog.data.map.ConcurrentFastIteratingHashMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.AbstractMutableContainer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class StackingMap<X> extends AbstractMutableContainer {

	private final ConcurrentFastIteratingHashMap<Object, Surface> map;

	public StackingMap() {
		super();
		this.map = new ConcurrentFastIteratingHashMap<>(Surface.EmptySurfaceArray);
	}

	@Override
	public void add(Surface... s) {
		throw new UnsupportedOperationException();
	}

	public Surface computeIfAbsent(Object x, Function<? super Object, ? extends Surface> s) {
		return map.compute(x, (xx, xp) -> {
			Surface xn = s.apply(xx);
			if (xp != null) {
				if (xp == xn) return xn;
				else {
					stop(xp);
				}
			}
			return start(xn);
		});
	}

	private Surface start(@Nullable Surface s) { if (s!=null) s.start(this); return s;}
	private Surface stop(@Nullable Surface s) { if (s!=null) s.stop(); return s; }

	public Surface put(Object x, Surface s) {
		return stop(map.put(x, start(s)));
	}

	public Surface remove(Object x) {
		return stop(map.remove(x));
	}

	@Override
	protected AbstractMutableContainer clear() {
		map.clear();
		return this;
	}

	@Override
	public void doLayout(float dtS) {
		forEach(c -> c.pos(bounds));
	}

	@Override
	public int childrenCount() {
		return map.size();
	}

	@Override
	public void forEach(Consumer<Surface> o) {
		map.forEachValue(o);
	}

	@Override
	public boolean whileEach(Predicate<Surface> o) {
		return map.whileEachValue(o);
	}

	@Override
	public boolean whileEachReverse(Predicate<Surface> o) {
		return map.whileEachValueReverse(o);
	}
}

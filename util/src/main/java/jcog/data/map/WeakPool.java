package jcog.data.map;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * https:
 */
public class WeakPool<X> {

	private static final WeakHashMap<Object,WeakReference<Object>> map = new WeakHashMap<>();

	public synchronized static <X> X the(final X x) {

		final WeakReference ref = map.get(x);
		Object y;
		if (ref != null) {
			y = ref.get();
			if (y!=null)
				return (X)y;
		}

		map.put(x, new WeakReference(x));
		return x;
	}
}
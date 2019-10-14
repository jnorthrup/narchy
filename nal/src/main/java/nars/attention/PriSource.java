package nars.attention;

import jcog.data.graph.MapNodeGraph;
import jcog.math.FloatRange;

/** variably adjustable priority source */
public final class PriSource extends PriNode {

	public final FloatRange amp;

	public PriSource(Object id, float p) {
		super(id);
		amp = new FloatRange(p, 0, 1);
		_pri(p);
	}

	private void _pri(float x) {
		this.pri.pri(x);
	}

	@Override
	public void update(MapNodeGraph<PriNode, Object> graph) {
		//assert(_node.edgeCount(true,false)==0);
		_pri(amp.get());
	}

	public void pri(float p) {
		amp.set(p);
	}
}

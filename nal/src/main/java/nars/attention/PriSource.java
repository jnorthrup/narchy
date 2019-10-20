package nars.attention;

import jcog.data.graph.MapNodeGraph;

/** variably adjustable priority source */
public final class PriSource extends PriNode {


	public PriSource(Object id, float p) {
		super(id);
		_pri(p);
	}

	private void _pri(float x) {
		this.pri.pri(x);
	}

	@Override
	public void update(MapNodeGraph<PriNode, Object> graph) {


	}

	public void pri(float p) {
		_pri(p);
	}
}

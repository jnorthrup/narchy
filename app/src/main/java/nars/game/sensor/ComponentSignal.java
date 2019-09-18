package nars.game.sensor;

import jcog.math.FloatRange;
import nars.game.Game;
import nars.term.Term;

/** one of many component signals in a vector sensor */
public abstract class ComponentSignal extends Signal {
	final VectorSensor vector;
	public float value;

	public ComponentSignal(Term componentID, VectorSensor vector) {
		super(componentID, vector.nar);
		this.vector = vector;
	}


	@Override
	public void update(Game a) {
		throw new UnsupportedOperationException("managed by " + vector + " dont call this directly");
	}

	abstract protected float value(Game g);

//        @Override
//        public short[] cause() {
//            return vector.cause;
//        }

	@Override
	public FloatRange resolution() {
		return vector.res;
	}
}

package nars.game.sensor;

import nars.game.Game;
import nars.term.Term;

/** one of many component signals in a vector sensor */
public abstract class ComponentSignal extends Signal {

	public ComponentSignal(Term componentID, VectorSensor vector) {
		super(componentID, vector.nar);
	}


	@Override
	public void update(Game a) {
		throw new UnsupportedOperationException();
	}

	abstract protected float value(Game g);

//        @Override
//        public short[] cause() {
//            return vector.cause;
//        }

//	@Override
//	public FloatRange resolution() {
//		return vector.res;
//	}
}

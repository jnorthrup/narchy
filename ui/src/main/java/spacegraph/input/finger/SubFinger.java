package spacegraph.input.finger;

import jcog.math.v2;
import spacegraph.SpaceGraph;

/** satellite cursor attached to a Finger */
abstract public class SubFinger extends Finger {

    public final Finger parent;
    final v2 posRel = new v2();

    public SubFinger(Finger parent, int buttons) {
        super(buttons);
        this.parent = parent;
    }

    @Override
    public final v2 posGlobal() {
        return parent.posGlobal().addClone(posRel);
    }

    /** dummy random finger for testing */
    public static class RandomSubFinger extends SubFinger {

        public RandomSubFinger(Finger parent) {
            super(parent, 0);
        }

        @Override
        protected void start(SpaceGraph x) {

        }

        @Override
        protected void stop(SpaceGraph x) {

        }
    }
}

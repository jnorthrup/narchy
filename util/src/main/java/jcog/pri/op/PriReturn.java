package jcog.pri.op;

/** selects the type of (float) value returned by a merge operation */
public enum PriReturn {

    /** NOP */
    Void {
        @Override
        public float apply(float incoming, float pBefore, float pAfter) {
            return Float.NaN;
        }
    },

    /** the value before the update */
    Pre {
        @Override
        public float apply(float incoming, float pBefore, float pAfter) {
            return pBefore;
        }
    },

    /** delta = after - before */
    Delta {
        @Override
        public float apply(float incoming, float pBefore, float pAfter) {
            return ((pAfter==pAfter) ? pAfter : 0) - pBefore;
        }
    },

    /** the value after the update */
    Post {
        @Override
        public float apply(float incoming, float pBefore, float pAfter) {
            return pAfter;
        }
    },

    /** any resultng overflow priority which was not absorbed by the target, >=0 */
    Overflow {
        @Override
        public float apply(float incoming, float pBefore, float pAfter) {

            if (incoming!=incoming)
                incoming = 0;

            if (pAfter != pAfter)
                return incoming; //deleted

            return incoming - (pAfter - pBefore);
        }
    };

    /** pBefore will not be NaN but pAfter might. */
    abstract public float apply(float incoming, float pBefore, float pAfter);
}

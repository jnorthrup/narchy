package nars.link;

import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Prioritized;

/** TODO use the cause value in derived tasks, use in plugins which create termlink and tasklinks */
public interface CauseLink {
    short[] cause();

    class PriCauseLink<X> extends PLink<X> implements CauseLink {

        private final short cause;

        public PriCauseLink(X id, float p, short cause) {
            super(id, p);
            this.cause = cause;
        }

        @Override
        public short[] cause() {
            return new short[] { cause };
        }
    }

    class CauseLinkUntilDeleted<X extends Prioritized> extends PLinkUntilDeleted<X> implements CauseLink {

        private final short cause;

        public CauseLinkUntilDeleted(X id, float p, short cause) {
            super(id, p);
            this.cause = cause;
        }

        @Override
        public short[] cause() {
            return new short[] { cause };
        }
    }

}

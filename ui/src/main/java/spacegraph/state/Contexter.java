package spacegraph.state;

public interface Contexter extends Context {

    class ContextBranch implements Contexter {

        private final String id;
        private Contexter parent = null;

        public ContextBranch(String id) {
            this.id = id;
        }

        public ContextBranch start(Contexter parent) {
            synchronized (this) {
                Contexter prevParent = this.parent;
                if (prevParent!=parent) {
                    if (prevParent!=null)
                        stop();

                    if (parent!=null)
                        start(this.parent = parent);
                }
            }
            return this;
        }

        protected void stop() {

        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Contexter parent() {
            return parent;
        }
    }
}

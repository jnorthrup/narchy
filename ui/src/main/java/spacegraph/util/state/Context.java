package spacegraph.util.state;

public interface Context {
    String id();

    default String[] tags() {
        return null;
    }

    Contexter parent();
}

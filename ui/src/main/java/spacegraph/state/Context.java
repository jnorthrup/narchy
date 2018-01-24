package spacegraph.state;

public interface Context {
    public String id();

    default String[] tags() {
        return null;
    }

    Contexter parent();
}

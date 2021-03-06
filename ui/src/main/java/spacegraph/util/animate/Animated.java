package spacegraph.util.animate;


@FunctionalInterface public interface Animated {

    /** returns whether to continue (true), or false if the animation should be removed */
    boolean animate(float dt);

    default boolean animate(int dtMS) {
        return animate((float) dtMS /1000f);
    }

}

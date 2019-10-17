package spacegraph.space2d;

import org.jetbrains.annotations.Nullable;

public interface Labeled {

    Surface label();

    default @Nullable Runnable labelClicked() {
        return null;
    }

}

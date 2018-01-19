package spacegraph;

import org.jetbrains.annotations.Nullable;

/** indicates it 'holds' or 'mounts' zero or more surfaces within, and provides a potential link to the root of the hierarchy */
public interface SurfaceBase {
    @Nullable SurfaceRoot root();
}

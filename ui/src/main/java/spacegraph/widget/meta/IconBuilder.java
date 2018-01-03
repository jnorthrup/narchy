package spacegraph.widget.meta;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.render.Draw;
import spacegraph.widget.text.Label;

import java.util.function.Function;

/** decoration inference for dynamic representational surfaces of arbitrary objects */
abstract public class IconBuilder<X> implements Function<X,Surface> {

    public static final IconBuilder<Object> simpleBuilder = new IconBuilder<>() {

        @Override
        public Surface apply(Object o) {
            return new Label(o.toString()) {

                final int classHash = o.getClass().hashCode();

                @Override
                public boolean tangible() {
                    return false;
                }

                @Override
                protected void paintBelow(GL2 gl) {
                    super.paintBelow(gl);
                    Draw.colorHash(gl, classHash);
                    Draw.rect(gl, bounds);
                }
            };
        }
    };

}

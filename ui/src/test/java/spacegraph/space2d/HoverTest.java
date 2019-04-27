package spacegraph.space2d;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.Fingering;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.video.OrthoSurfaceGraph;

import java.util.function.BiFunction;
import java.util.function.Function;

/** hover / tooltip tests */
public class HoverTest {

    static class Hover<X extends Surface,Y extends Surface> extends Fingering {

        /** positioned exactly the same as source's visible position */
        public static final BiFunction<Surface, Finger, RectFloat> Exact =
                (s,f)->s.bounds;

        /** maximized to screen extents */
        public static final BiFunction<Surface, Finger, RectFloat> Maximimum =
                (s,f)->f.boundsScreen;

        /** smaller and to the side so as not to cover any of the visible extents of the source.
         *  uses global screen pos as a heuristic of what direction to shift towards to prevent
         *  clipping beyond the screen edge
         * */
        public static final BiFunction<Surface, Finger, RectFloat> ToolTip =
                (s,f)-> {
                    RectFloat ss = f.globalToPixel(s.bounds);
                    return ss.scale(0.25f).move(ss.w/2, ss.h / 2);
                }; //TODO

        /** attached relative to cursor center and sized relative to element */
        public static final BiFunction<Surface, Finger, RectFloat> Cursor =
                (s,f)->{
                    RectFloat ss = f.globalToPixel(s.bounds);
                    return RectFloat.XYWH(f.posPixel, ss.w, ss.h);
                };

        //TODO delayNS

        final X source;
        volatile Surface target = null;
        private final Function<X, Y> targetBuilder;
        private RectFloat srcBoundsPx;
        private RectFloat tgtBoundsPx;

        /** computes display position, in screen (pixel) coordinates */
        final BiFunction<X,Finger,RectFloat> positioner;

        Hover(X source, Function<X, Y> target, BiFunction<X, Finger, RectFloat> positioner) {
            this.source = source;
            this.targetBuilder = target;
            this.positioner = positioner;
            this.target = null;
        }

        @Override
        protected boolean start(Finger f) {
            //TODO reset delay time

            if (update(f)) {
                return show();
            }
            return false;
        }

        @Override
        public boolean update(Finger f) {
            //update
            if (f.touching()==source) {

                srcBoundsPx = f.globalToPixel(source.bounds);
                tgtBoundsPx = positioner.apply(source, f);

                if (tgtBoundsPx!=null && target!=null) {
                    updatePos();
                }

                return (tgtBoundsPx !=null);
            }
            return false;
        }



        protected boolean show() {
            Stacking root = ((OrthoSurfaceGraph) source.root()).layers;
            synchronized (targetBuilder) {
                target = targetBuilder.apply(source);
            }
            if (target!=null) {
                root.add(target);
                updatePos();
                return true;
            }
            return false;
        }

        private void updatePos() {
            //Exe.invokeLater(()->{
            target.pos(tgtBoundsPx); //HACK TODO dont allow root Stacking to maximize pos that this resets
            //});
        }

        @Override
        public boolean defer(Finger finger) {
            return true;
        }


        protected void hide() {
            synchronized (targetBuilder) {
                if (target!=null) {
                    target.delete();
                    target = null;
                }
            }
        }

        @Override
        public void stop(Finger finger) {
            hide();
            super.stop(finger);
        }
    }

    public static void main(String[] args) {
        SpaceGraph.window(new Gridding(
            new HoverButton("x", Hover.Exact),
            new HoverButton("y", Hover.Maximimum),
            new HoverButton("z", Hover.ToolTip),
            new HoverButton("w", Hover.Cursor)
        ), 500, 500);
    }

    private static class HoverButton extends PushButton {

        final Hover hover;

        public HoverButton(String label, BiFunction<Surface, Finger, RectFloat>  pos) {
            super(label);
            this.hover = new Hover<>(this, b ->
                    new BitmapLabel("yes").backgroundColor(0.9f, 0.5f, 0f, 0.5f)
                    , pos);
        }

        @Override
        public Surface finger(Finger finger) {
            Surface s = super.finger(finger);
            if (s == this) {
                finger.test(hover);
            }
            return s;
        }
    }
}

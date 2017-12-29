package spacegraph.widget.windo;

import com.jcraft.jsch.JSchException;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import org.jetbrains.annotations.Nullable;
import spacegraph.AspectAlign;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.layout.Layout;
import spacegraph.layout.Stacking;
import spacegraph.layout.VSplit;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.ConsoleTerminal;
import spacegraph.widget.console.TextEdit;
import spacegraph.widget.meta.MetaFrame;
import spacegraph.widget.slider.BaseSlider;
import spacegraph.widget.slider.FloatSlider;
import spacegraph.widget.slider.XYSlider;
import spacegraph.widget.text.Label;

import java.io.IOException;

import static spacegraph.layout.Grid.*;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
abstract public class Widget extends Stacking {

    @Nullable Finger touchedBy;

    /** z-raise/depth: a state indicating push/pull (ex: buttons)
     * positive: how lowered the button is: 0= not touched, to 1=push through the screen
     *     zero: neutral state, default for components
     * negative: how raised
     * */
    protected float dz = 0;

     /** indicates current level of activity of this component, which can be raised by various
      *  user and system actions and expressed in different visual metaphors.
      *  positive: active, hot, important
      *      zero: neutral
      *  negative: disabled, hidden, irrelevant
      */
    float temperature = 0;


//MARGIN
//    @Override
//    public void setParent(Surface s) {
//        super.setParent(s);
//
//        float proportion = 0.9f;
//        float margin = 0.0f;
//        //float content = 1f - margin;
//        float x = margin / 2f;
//
//        Surface content = content();
//        content.scaleLocal.set(proportion, proportion);
//        content.translateLocal.set(x, 1f - proportion, 0);
//
//    }

    public Widget() {

    }

    public Widget(Surface... child) {
        children(child);
    }

    @Override
    public boolean tangible() {
        return true;
    }


    @Override
    public void prePaint(int dtMS) {

        if (dtMS > 0) {
            if (touchedBy != null) {
                temperature = Math.min(1f, temperature + dtMS / 100f);
            }

            if (temperature != 0) {
                float decayRate = (float)Math.exp(-dtMS / 1000f);
                temperature *= decayRate;
                if (Math.abs(temperature) < 0.01f)
                    temperature = 0f;
            }
        }


    }


    @Override
    protected void paintBelow(GL2 gl) {

        if (tangible()) {
            float dim = 1f - (dz /* + if disabled, dim further */) / 3f;
            float bri = 0.25f * dim;
            float r, g, b;
            r = g = b = bri;


            float t = this.temperature;
            if (t >= 0) {
                //fire palette TODO improve
                //            r += t / 2f;
                //            g += t / 4f;

                r += t / 4f;
                g += t / 4f;
                b += t / 4f;
            } else {
                //ice palette TODO improve
                b += -t / 2f;
                g += -t / 4f;
            }

            gl.glColor4f(r, g, b, 0.5f);

            Draw.rect(gl, bounds);
        }

                //rainbow backgrounds
        //Draw.colorHash(gl, this.hashCode(), 0.8f, 0.2f, 0.25f);
        //Draw.rect(gl, 0, 0, 1, 1);
    }

    @Override
    protected void paintAbove(GL2 gl) {
        if (touchedBy != null) {
            Draw.colorHash(gl, getClass().hashCode(), 0.5f + dz/2f);
            //gl.glColor3f(1f, 1f, 0f);
            gl.glLineWidth(6 + dz*6);
            Draw.rectStroke(gl, x(), y(), w(), h());
        }
    }

    //    @Override
//    protected boolean onTouching(v2 hitPoint, short[] buttons) {
////        int leftTransition = buttons[0] - (touchButtons[0] ? 1 : 0);
////
////        if (leftTransition == 0) {
////            //no state change, just hovering
////        } else {
////            if (leftTransition > 0) {
////                //clicked
////            } else if (leftTransition < 0) {
////                //released
////            }
////        }
//
//
//        return false;
//    }


    public void touch(@Nullable Finger finger) {
        touchedBy = finger;
        if (finger == null) {
            onTouch(finger, null, null);
        }
    }

    @Override
    protected boolean onTouching(Finger finger, v2 hitPoint, short[] buttons) {
        if (finger != null && finger.clickReleased(2)) { //released right button
            MetaFrame.toggle(this);
            return true;
        }
        return super.onTouching(finger, hitPoint, buttons);
    }

    public static void main(String[] args) throws IOException, JSchException {

        SpaceGraph s = SpaceGraph.window(

                widgetDemo()
                , 1200, 800);


        //SpaceGraph dd = SpaceGraph.window(new Cuboid(widgetDemo(), 16, 8f).color(0.5f, 0.5f, 0.5f, 0.25f), 1000, 1000);

//        new SpaceGraph2D(
//                new Cuboid(widgetDemo(), 16, 8f, 0.2f).color(0.5f, 0.5f, 0.5f, 0.25f).move(0,0,0)
//        ).show(800, 600);

    }


    public static Layout widgetDemo() {
        return
            grid(
                row(new PushButton("row1"), new PushButton("row2"), new PushButton("clickMe()", (p) -> {
                    p.setLabel(Texts.n2(Math.random()));
                })),
                new VSplit(
                        new PushButton("vsplit"),
                        row(
                            col(new CheckBox("checkbox"), new CheckBox("checkbox")),
                            grid(
                                    new PushButton("a"), new PushButton("b"), new PushButton("c"), new PushButton("d")
                            )
                        ), 0.8f
                ),
                col(
                        new Label("label"),
                        new FloatSlider("solid slider", .25f  /* pause */, 0, 1),
                        new FloatSlider("knob slider", 0.75f, 0, 1).type(BaseSlider.Knob)
                ),
                new XYSlider(),
                new DummyConsole().align(AspectAlign.Align.Center)
            );
    }

    private static class DummyConsole extends ConsoleTerminal implements Runnable {

        public DummyConsole() {
            super(new TextEdit(15, 15));
            new Thread(this).start();
        }

        @Override
        public void run() {

            while (true) {

                append((Math.random()) + " ");

                term.flush();

                Util.sleep(200);
            }
        }
    }
}

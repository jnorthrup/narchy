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
        set(child);
    }

    @Override
    public boolean tangible() {
        return true;
    }

    @Override
    protected void paint(GL2 gl) {

        paintComponent(gl);

        super.paint(gl);

//        /*if (Param.DEBUG)*/ {
//            Draw.colorHash(gl, hashCode(), 0.5f);
//            String s = "g:" + scaleGlobal;
//            Draw.text(gl, s, 0.025f, 0.5f, 0, 0);
//            String s2 = "l:" + scaleLocal;
//            Draw.text(gl, s2, 0.025f, 0.5f, 1f, 0);
//        }

        //rainbow backgrounds
        //Draw.colorHash(gl, this.hashCode(), 0.8f, 0.2f, 0.25f);
        //Draw.rect(gl, 0, 0, 1, 1);


        if (touchedBy != null) {
            Draw.colorHash(gl, getClass().hashCode(), 0.5f);
            //gl.glColor3f(1f, 1f, 0f);
            gl.glLineWidth(4);
            Draw.rectStroke(gl, x(), y(), w(), h());
        }


    }


    abstract protected void paintComponent(GL2 gl);


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
        }
        return super.onTouching(finger, hitPoint, buttons);
    }

    public static void main(String[] args) throws Exception {

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
                        new FloatSlider("knob slider", 0.75f, 0, 1).draw(BaseSlider.Knob)
                ),
                new XYSlider(),
                new DummyConsole().align(AspectAlign.Align.Center, 1f)
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

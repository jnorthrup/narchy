package spacegraph.widget.console;


import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Label;
import spacegraph.SpaceGraph;
import spacegraph.render.JoglSpace;

import static java.lang.Math.round;

public class BitmapLabel extends ConsoleGUI {

    private final Label label;

    public BitmapLabel(String text) {
        super(text.length(), 1);
        this.label = new Label(text);

        //default: white on black
        colorText(1f,1f,1f);
        colorBackground(0f,0f,0f);
    }

    public BitmapLabel text(String newText) {
        this.label.setText(newText);
        return this;
    }

    public String text() {
        return label.getText();
    }

    public BitmapLabel colorText(float rr, float gg, float bb) {
        return colorText(round(rr * 255), round(gg * 255), round(bb * 255));
    }

    public BitmapLabel colorBackground(float rr, float gg, float bb) {
        return colorBackground(round(rr * 255), round(gg * 255), round(bb * 255));
    }

    public BitmapLabel colorText(int rr, int gg, int bb) {
        label.setForegroundColor(new TextColor.RGB(
                rr,
                gg,
                bb));
        return this;
    }

    public BitmapLabel colorBackground(int rr, int gg, int bb) {
        label.setBackgroundColor(new TextColor.RGB(
                rr,
                gg,
                bb));
        return this;
    }


    @Override
    protected void init(BasicWindow window) {
        window.setComponent(label);
    }

    public static void main(String[] args) {
        JoglSpace.window(new BitmapLabel("what the fuck"), 500, 500);
    }
}

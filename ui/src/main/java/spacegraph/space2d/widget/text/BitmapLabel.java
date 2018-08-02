package spacegraph.space2d.widget.text;


import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.console.BitmapTextGrid;
import spacegraph.space2d.widget.windo.Widget;

import java.io.IOException;

import static java.lang.Math.round;

public class BitmapLabel extends BitmapTextGrid {


    private String text;
    private TextColor fgColor = TextColor.ANSI.WHITE, bgColor= TextColor.ANSI.BLACK;

    public BitmapLabel(String text) {
        super();

        cursorCol = cursorRow = -1; //hidden

        setFillTextBackground(false);
        colorText(1f,1f,1f);

        text(text);



        //setUpdateNecessary();

    }

    @Override
    public void doLayout(int dtMS) {
        //HACK override the auto-sizing
    }

    public BitmapLabel text(String newText) {
        this.text = newText;
        resize(text.length(), 1);
        //setUpdateNecessary();

        return this;
    }

    public String text() {
        return text;
    }

    private BitmapLabel colorText(float rr, float gg, float bb) {
        return colorText(round(rr * 255), round(gg * 255), round(bb * 255));
    }

    private BitmapLabel colorBackground(float rr, float gg, float bb) {
        return colorBackground(round(rr * 255), round(gg * 255), round(bb * 255));
    }

    private BitmapLabel colorText(int rr, int gg, int bb) {
        fgColor = (new TextColor.RGB(
                rr,
                gg,
                bb));
        return this;
    }

    private BitmapLabel colorBackground(int rr, int gg, int bb) {
        bgColor = (new TextColor.RGB(
                rr,
                gg,
                bb));
        return this;
    }


    public static void main(String[] args) {
        BitmapLabel b = new BitmapLabel("x");
        SpaceGraph.window(new Widget(b), 500, 500);
    }

    @Override
    protected boolean updateBackBuffer() {
        for (int i = 0; i < cols; i++)
            for (int j = 0; j < rows; j++)
                redraw(charAt(i,j),i,j);
        return true;
    }

    @Override
    public TextCharacter charAt(int col, int row) {
        return new TextCharacter(text.charAt(col), fgColor, bgColor);
    }

    @Override
    public Appendable append(CharSequence charSequence) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Appendable append(char c) throws IOException {
        throw new UnsupportedOperationException();
    }
}

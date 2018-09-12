package spacegraph.space2d.widget.text;


import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.widget.console.BitmapTextGrid;

import java.util.Arrays;

import static java.lang.Math.round;

public class BitmapLabel extends BitmapTextGrid {

    static final float charAspect = 1.6f;
    static final int minPixelsToBeVisible = 7;

    private volatile String text = "";
    private volatile TextColor fgColor = TextColor.ANSI.WHITE, bgColor= TextColor.ANSI.BLACK;
    private volatile RectFloat2D textBounds;

    public BitmapLabel(String text) {
        super();

        textBounds = bounds;
        cursorCol = cursorRow = -1; //hidden

        setFillTextBackground(false);
        colorText(1f,1f,1f);

        text(text);



        //setUpdateNecessary();

    }

    public BitmapLabel() {
        this("");
    }

    @Override
    protected boolean prePaint(SurfaceRender r) {
        float p = r.visP(bounds).minDimension();
        if (p < minPixelsToBeVisible) {
            return false;
        }
        return true;
    }

    @Override
    protected RectFloat2D textBounds() {
        return textBounds;
    }
    protected void layoutText() {
        if (cols > 0 && rows > 0) {

            textBounds = AspectAlign.the(bounds, (rows * charAspect) / cols);

        } else
            textBounds = bounds; //nothing
    }

    @Override
    public void doLayout(int dtMS) {
        //HACK override the auto-sizing
        layoutText();
    }

    public BitmapLabel text(String newText) {
        if (!this.text.equals(newText)) {
            this.text = newText;

            int rows = 1 + Texts.count(newText, '\n');
            if (rows == 1) {
                resize(newText.length(), 1);
            } else {
                //HACK do better
                int cols = Arrays.stream(newText.split("\n")).mapToInt(String::length).max().getAsInt();
                resize(cols, rows);
            }
            layoutText();
            invalidate();
        }

        return this;
    }

    public String text() {
        return text;
    }

    public BitmapLabel colorText(float rr, float gg, float bb) {
        return colorText(round(rr * 255), round(gg * 255), round(bb * 255));
    }

    private BitmapLabel colorBackground(float rr, float gg, float bb) {
        return colorBackground(round(rr * 255), round(gg * 255), round(bb * 255));
    }

    public BitmapLabel colorText(int rr, int gg, int bb) {
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




    @Override
    protected boolean updateBackBuffer() {

        clearBackground(); //may not be necessary if only one line and all characters are used but in multiline the matrix currently isnt regular so some chars will not be redrawn

        int n = text.length();
        int row = 0, col =0;
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                row++;
                col = 0;
            } else {
                redraw(new TextCharacter(c, fgColor, bgColor), col++, row);
            }
        }
        return true;
    }

    @Override
    @Deprecated public TextCharacter charAt(int col, int row) {
        return new TextCharacter(text.charAt(col), fgColor, bgColor);
    }

    @Override
    public Appendable append(CharSequence charSequence) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Appendable append(char c) {
        throw new UnsupportedOperationException();
    }
}

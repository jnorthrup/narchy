package spacegraph.space2d.widget.text;


import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.space2d.widget.console.BitmapTextGrid;

import java.util.Arrays;

public class BitmapLabel extends BitmapTextGrid {

    static final float charAspect = 1.6f;
    static final int minPixelsToBeVisible = 7;

    private volatile String text = "";
    private volatile Color3f fgColor = Color3f.WHITE, bgColor= Color3f.BLACK;
    private volatile RectFloat textBounds;

    public BitmapLabel(String text) {
        super();

        textBounds = bounds;
        cursorCol = cursorRow = -1; //hidden

        setFillTextBackground(false);
        textColor(1f,1f,1f);

        text(text);



        //setUpdateNecessary();

    }

    public BitmapLabel() {
        this("");
    }

    @Override
    protected boolean prePaint(SurfaceRender r) {
        return r.visP(bounds, minPixelsToBeVisible);
    }

    @Override
    protected RectFloat textBounds() {
        return textBounds;
    }
    protected void layoutText() {
        if (cols > 0 && rows > 0) {

            textBounds = AspectAlign.innerBounds(bounds, (rows * charAspect) / cols);

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

    public BitmapLabel textColor(float rr, float gg, float bb) {
        fgColor.set((rr), (gg), (bb));
        return this;
    }

    public BitmapLabel backgroundColor(float rr, float gg, float bb) {
        bgColor.set((rr), (gg), (bb));
        return this;
    }





    @Override
    protected boolean renderText() {

        clearBackground(); //may not be necessary if only one line and all characters are used but in multiline the matrix currently isnt regular so some chars will not be redrawn

        int n = text.length();
        int row = 0, col =0;
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                row++;
                col = 0;
            } else {
                redraw(c, col++, row, fgColor, bgColor);
            }
        }
        return true;
    }




}

package nars.experiment.minicraft.top.screen;

import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;

import java.util.List;

public class Menu {
    protected TopDownMinicraft game;
    protected InputHandler input;

    public void init(TopDownMinicraft game, InputHandler input) {
        this.input = input;
        this.game = game;
    }

    public void tick() {
    }

    public void render(Screen screen) {
    }

    public static void renderItemList(Screen screen, int xo, int yo, int x1, int y1, List<? extends ListItem> listItems, int selected) {
        var renderCursor = true;
        if (selected < 0) {
            selected = -selected - 1;
            renderCursor = false;
        }
        var h = y1 - yo - 1;
        var i1 = listItems.size();
        if (i1 > h) i1 = h;
        var io = selected - h / 2;
        if (io > listItems.size() - h) io = listItems.size() - h;
        if (io < 0) io = 0;

        var i0 = 0;
        for (var i = i0; i < i1; i++) {
            listItems.get(i + io).renderInventory(screen, (1 + xo) * 8, (i + 1 + yo) * 8);
        }

        if (renderCursor) {
            var yy = selected + 1 - io + yo;
            Font.draw(">", screen, (xo + 0) * 8, yy * 8, Color.get(5, 555, 555, 555));
            var w = x1 - xo;
            Font.draw("<", screen, (xo + w) * 8, yy * 8, Color.get(5, 555, 555, 555));
        }
    }
}

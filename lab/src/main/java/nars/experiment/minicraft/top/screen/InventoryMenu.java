package nars.experiment.minicraft.top.screen;

import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.item.Item;

public class InventoryMenu extends Menu {
    private final Player player;
    private int selected;

    public InventoryMenu(Player player) {
        this.player = player;

        if (player.activeItem != null) {
            player.inventory.items.add(0, player.activeItem);
            player.activeItem = null;
        }
    }

    @Override
    public void tick() {
        if (input.menu.clicked) game.setMenu(null);

        if (input.up.clicked) selected--;
        if (input.down.clicked) selected++;

        var len = player.inventory.items.size();
        if (len == 0) selected = 0;
        if (selected < 0) selected += len;
        if (selected >= len) selected -= len;

        if (input.attack.clicked && len > 0) {
            var item = player.inventory.items.remove(selected);
            player.activeItem = item;
            game.setMenu(null);
        }
    }

    @Override
    public void render(Screen screen) {
        Font.renderFrame(screen, "", 1, 1, 12, 11);
        renderItemList(screen, 1, 1, 12, 11, player.inventory.items, selected);
    }
}
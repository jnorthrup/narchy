package nars.experiment.minicraft.top.screen;

import nars.experiment.minicraft.top.gfx.Screen;

@FunctionalInterface
public interface ListItem {
    void renderInventory(Screen screen, int i, int j);
}

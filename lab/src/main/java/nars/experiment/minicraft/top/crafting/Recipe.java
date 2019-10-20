package nars.experiment.minicraft.top.crafting;

import nars.experiment.minicraft.top.entity.Player;
import nars.experiment.minicraft.top.gfx.Color;
import nars.experiment.minicraft.top.gfx.Font;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.item.Item;
import nars.experiment.minicraft.top.item.ResourceItem;
import nars.experiment.minicraft.top.item.resource.Resource;
import nars.experiment.minicraft.top.screen.ListItem;

import java.util.ArrayList;
import java.util.List;

public abstract class Recipe implements ListItem {
    public List<Item> costs = new ArrayList<>();
    public boolean canCraft;
    public Item resultTemplate;

    public Recipe(Item resultTemplate) {
        this.resultTemplate = resultTemplate;
    }

    public Recipe addCost(Resource resource, int count) {
        costs.add(new ResourceItem(resource, count));
        return this;
    }

    public void checkCanCraft(Player player) {
        for (var i = 0; i < costs.size(); i++) {
            var item = costs.get(i);
            if (item instanceof ResourceItem) {
                var ri = (ResourceItem) item;
                if (!player.inventory.hasResources(ri.resource, ri.count)) {
                    canCraft = false;
                    return;
                }
            }
        }
        canCraft = true;
    }

    @Override
    public void renderInventory(Screen screen, int x, int y) {
        screen.render(x, y, resultTemplate.getSprite(), resultTemplate.getColor(), 0);
        var textColor = canCraft ? Color.get(-1, 555, 555, 555) : Color.get(-1, 222, 222, 222);
        Font.draw(resultTemplate.getName(), screen, x + 8, y, textColor);
    }

    public abstract void craft(Player player);

    public void deductCost(Player player) {
        for (var i = 0; i < costs.size(); i++) {
            var item = costs.get(i);
            if (item instanceof ResourceItem) {
                var ri = (ResourceItem) item;
                player.inventory.removeResource(ri.resource, ri.count);
            }
        }
    }
}
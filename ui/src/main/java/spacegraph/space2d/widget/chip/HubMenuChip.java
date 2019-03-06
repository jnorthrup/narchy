package spacegraph.space2d.widget.chip;

import jcog.exe.Exe;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.windo.GraphEdit;

import java.util.Map;
import java.util.function.Supplier;

public class HubMenuChip extends Bordering {

    private final Map<String, Supplier<Surface>> menu;

    public HubMenuChip(Surface the, Map<String,Supplier<Surface>> items) {
        super(the);
        this.menu = items;

        size(100,100); //temporar
    }

    @Override
    protected void starting() {
        super.starting();

        GraphEdit g = parent(GraphEdit.class); //TODO generic Spawnable interface
        menu.forEach((name, i)->{
            ExpandingChip ii = new ExpandingChip(name, i);
            Container iii = g.add(ii).posRel(bounds, 1, 1, 0.1f, 0.1f); //TODO radial layout

            Exe.invokeLater(()->{
                g.addWire(new Wire(HubMenuChip.this, ii));
            });

        });

    }
}

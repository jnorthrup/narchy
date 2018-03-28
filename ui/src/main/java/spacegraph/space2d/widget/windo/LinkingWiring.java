package spacegraph.space2d.widget.windo;

import spacegraph.input.finger.Wiring;
import spacegraph.space2d.Surface;

/** attempts to establish a default link if the wiring was successful */
public class LinkingWiring extends Wiring {

    public LinkingWiring(Surface start) {
        super(start);
    }

    @Override
    protected void onWired() {
        source().link(start, end);
    }
}

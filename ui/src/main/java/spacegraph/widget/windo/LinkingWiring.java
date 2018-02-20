package spacegraph.widget.windo;

import spacegraph.Surface;
import spacegraph.input.Wiring;

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

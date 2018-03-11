package spacegraph.test.dyn2d;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.Util;
import jcog.math.IntRange;
import jcog.net.UDPeer;
import spacegraph.container.Gridding;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.PhyWall;
import spacegraph.widget.windo.Port;

import java.io.IOException;

public class MeshChipTest {

    /**
     * encapsulates a Mesh node end-point with ports for send and recv
     */
    public static class MeshChip extends Gridding {

        final IntRange ttl = new IntRange(3, 1, 5);

        final UDPeer peer;
        private final Port in, out;

        public MeshChip(UDPeer peer) {
            this.peer = peer;
            peer.runFPS(5f);
            this.in = new Port().on(x->{
                try {
                    peer.tellSome(Util.toBytes(x), ttl.intValue());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
            this.out = new Port();

            set(
                    new Gridding(
                            new Label(peer.name()),
                            in,
                            out
                    )
            );
        }
    }

    public static void main(String[] args) throws IOException {

        PhyWall p = PhyWall.window(800, 800);
        p.addWindow(new MeshChip(new UDPeer()), 1, 1);
        p.addWindow(new MeshChip(new UDPeer()), 1, 1);
    }
}

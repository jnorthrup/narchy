package spacegraph.space2d.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.Util;
import jcog.event.On;
import jcog.exe.Every;
import jcog.math.IntRange;
import jcog.net.UDPeer;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.SpaceGraph;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meter.BagChart;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.PhyWall;
import spacegraph.space2d.widget.windo.Port;

import java.io.IOException;

public class MeshChipTest {

    /**
     * encapsulates a Mesh node end-point with ports for send and recv
     */
    public static class MeshChip extends Gridding {

        final IntRange ttl = new IntRange(3, 1, 5);

        final UDPeer peer;
        private final Port in, out;
        private final BagChart<UDPeer.UDProfile> themChart;
        private final Every display;
        private On recv;

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

            this.themChart = new BagChart<>(peer.them);
            set(
                new Gridding(
                        new Label(peer.name()),
                        new LabeledPane("I", in),
                        new LabeledPane("O", out),
                        new LabeledPane("them", themChart)
                )
            );
            this.display = new Every(themChart::update, 100);
        }

        @Override
        public boolean start(SurfaceBase parent) {
            if (super.start(parent)){
                recv = peer.onReceive.on(this::receive);
                return true;
            }
            return false;
        }

        protected void receive(Pair<UDPeer.UDProfile,UDPeer.Msg> x) {
            try {
                out.out(Util.fromBytes(x.getTwo().data(), Object.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean stop() {
            if (super.stop()) {
                recv.off();
                recv = null;
                return true;
            }
            return false;
        }

        @Override
        public void prePaint(int dtMS) {
            super.prePaint(dtMS);
            display.next();
        }
    }

    public static void main(String[] args) throws IOException {

        PhyWall p = SpaceGraph.wall(800, 800);
        p.put(new MessageChip(), 1, 1);
        p.put(new MeshChip(new UDPeer()), 1, 1);
        p.put(new MeshChip(new UDPeer()), 1, 1);
    }

    public static class MessageChip extends Splitting {

        final Port out = new Port();

        final TextEdit t = new TextEdit(24, 3) {
            @Override
            protected void onKeyCtrlEnter() {
                String t = text();
                text("");
                out.out(t);
            }
        };

        public MessageChip() {
            super();
            split(0.1f);
            set(t.surface(), out);
        }
    }

}

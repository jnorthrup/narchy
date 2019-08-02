package nars;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.graph.Node;
import jcog.event.Off;
import nars.attention.PriNode;
import nars.attention.What;
import nars.gui.DurSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.ForceDirected2D;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.util.MutableRectFloat;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class AttentionUI {

//    static class NodeUI extends Gridding {
//        public final PriNode node;
//
//        NodeUI(PriNode node) {
//            this.node = node;
//            add(new VectorLabel(node.toString()));
//            add();
//        }
//    }

//    public static GraphEdit serviceGraph(NAR n, NAgent a) {
//        GraphEdit<Surface> g = GraphEdit.window(800, 500);
//        g.add(new ExpandingChip(a.toString(), (x)->{
//            new ObjectSurface(a).forEach
//        }));
//        return g;
//    }

    public static Surface whatMixer(NAR n) {
        WhatMixer w = new WhatMixer(n);
        return DurSurface.get(w, n, w::commit);
    }

    public static Surface attentionGraph(NAR n) {
        Graph2D<PriNode> aaa = new Graph2D<PriNode>()
                .build(x->{
                    x.set(LabeledPane.the(x.id.toString(), new ObjectSurface<>(x.id, 2)));
                })
                .render((Graph2D.Graph2DRenderer<PriNode>) (node, graph) -> {
                    n.control.graph.node(node.id).nodes(false,true).forEach(c -> {
                        EdgeVis<PriNode> e = graph.edge(node, c.id());
                        if (e!=null) {
                            e.weight(1f);
                            e.color(0.5f, 0.5f, 0.5f);
                        }
                    });
                    float s = node.id.pri();
                    float d = 0.5f * node.id.pri();
//                            float r = Math.min(1, s/d);
                    node.color(Math.min(d, 1), Math.min(s, 1), 0);

                })
                .update(new ForceDirected2D<>() {
                    @Override
                    protected void size(MutableRectFloat<PriNode> m, float a) {
                        float q =
                                    m.node.id.pri();

                        float s = (float)(Math.sqrt((Math.max(0, q))));
                        s = Util.clamp(s * a, 2, 32);
                        m.size(s, s);
                    }
                });
        return DurSurface.get(aaa.widget(), n, () -> {
            aaa.set(Iterables.transform(n.control.graph.nodes(), Node::id));
        } ).live();
    }

    private static class WhatMixer extends Gridding {

        private final NAR n;
        private Off off;

        public WhatMixer(NAR n) {
            this.n = n;
        }

        @Override
        protected void starting() {
            super.starting();
            off = n.eventOnOff.on(this::update);
            update();
        }

        private void update() {
            List<Surface> channel = n.parts(What.class).map(this::mix).collect(toList());
            if (channel.isEmpty())
                set(new BitmapLabel("Empty"));
            else
                set(channel);
        }

        private Surface mix(What p) {
            FloatSlider amp = new WhatPri(p);
            //Surface s = new Bordering(amp);
            return LabeledPane.the(
                    p.term().toString(),
                    amp
            );
        }

        void commit() {
            forEachRecursively(x -> {
                if (x instanceof WhatPri) {
                    ((WhatPri)x).commit();
                }
            });
        }

        @Override
        protected void stopping() {
            off.close();
            off = null;
            super.stopping();
        }

        private static class WhatPri extends FloatSlider {

            private final What p;

            public WhatPri(What p) {
                super(p.pri(), 0, 1);
                this.p = p;
                on((x) -> {
                    p.pri(x);
                });
            }

            public void commit() {
                float nextPri = p.pri.amp();
                //System.out.println(p + " "+ nextPri);
                set(nextPri);
            }
        }
    }
}

package nars;

import jcog.data.graph.Node;
import jcog.event.Off;
import jcog.pri.Prioritized;
import nars.attention.PriNode;
import nars.attention.What;
import nars.gui.DurSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.ForceDirected2D;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.port.Surplier;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.util.MutableRectFloat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
		Graph2D aaa = new Graph2D<>()
			.build(x ->
				x.set(Surplier.button(x.id.toString(), () ->LabeledPane.the(x.id.toString(),
					new ObjectSurface(x.id, 2, ObjectSurface.builtin,
						Map.of(
							//TODO nars specific renderers
						)))))
			).render((xx, graph) -> {
				Object x = xx.id;
				Node<PriNode, Object> nn = n.control.graph.node(x);
				if (nn!=null) {
					nn.nodes(false, true).forEach(c -> {
						EdgeVis<Object> e = graph.edge(xx, c.id());
						if (e != null) {
							e.weight(1f);
							e.color(0.5f, 0.5f, 0.5f);
						}
					});
				}
				if (x instanceof Prioritized) {
					Prioritized node = (Prioritized)x;
					float s = node.pri();
					xx.pri = s;

					float d = 0.5f * node.pri();
//                            float r = Math.min(1, s/d);
					xx.color(Math.min(d, 1), Math.min(s, 1), 0);
				} else {
					//..infer priority of unprioitized items, ex: by graph metrics in relation to prioritized
				}

			})
			.update(new ForceDirected2D<>() {
				@Override
				protected void size(MutableRectFloat m, float a) {
					float q = m.node.pri;
					float s = (float) (Math.sqrt((Math.max(0, q))));
					//w = Util.clamp(s * a, 2, 32);
					float w = 2 + s * a;
					m.size(w, w);
				}
			});
		return DurSurface.get(aaa.widget(), n, () -> {

			Stream s =
				//Stream.concat(Stream.of(n.control.graph.nodeIDs()), n.partStream());
				n.partStream();

//			s = s.peek(x -> {
//				System.out.println(x.getClass()  + "\t" + x);
//			});

			aaa.set(s::iterator);

		}).live();
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
					((WhatPri) x).commit();
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
			float next = Float.NaN;

			public WhatPri(What p) {
				super(p.pri(), 0, 1);
				this.p = p;
				on((n) -> next = n);
			}

			public void commit() {
				float nextPri;
				if (next == next) {
					p.pri(nextPri = next);
					next = Float.NaN;
				} else {
					nextPri = p.pri.pri();
				}
				//System.out.println(p + " "+ nextPri);
				set(nextPri);
			}
		}
	}
}

package nars;

import com.google.common.collect.Streams;
import jcog.data.graph.Node;
import jcog.event.Off;
import jcog.exe.Exe;
import jcog.pri.Prioritized;
import nars.attention.PriNode;
import nars.attention.What;
import nars.concept.Concept;
import nars.game.sensor.VectorSensor;
import nars.gui.DurSurface;
import nars.gui.NARui;
import nars.gui.concept.ConceptSurface;
import nars.gui.sensor.VectorSensorChart;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.jetbrains.annotations.NotNull;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.ForceDirected2D;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.port.Surplier;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.MutableRectFloat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
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

	public static GraphEdit2D graphGraph(NAR n) {
        GraphEdit2D g = new GraphEdit2D();
		g.resize(800.0F, 800.0F);
		//g.windoSizeMinRel(0.02f, 0.02f);

		Exe.runLater(new Runnable() {
            @Override
            public void run() {
//		g.add(NARui.game(a)).posRel(0.5f, 0.5f, 0.4f, 0.3f);
                //g.add(NARui.top(n)).posRel(0.5f, 0.5f, 0.2f, 0.1f);
                g.add(NARui.attentionUI(n)).resize(400.25f, 400.25f);
            }
        });
		return g;
	}

	@Deprecated
	public static Surface objectGraphs(NAR n) {
        ObjectGraphs g = new ObjectGraphs(new Iterable() {
            @NotNull
            @Override
            public Iterator iterator() {
                return Stream.concat(Streams.stream(n.control.graph.nodeIDs()), n.partStream())
                        .iterator();
            }
        }
			,
			Map.of(
				NAR.class, new BiFunction<NAR, Object, Surface>() {
                        @Override
                        public Surface apply(NAR nn, Object relation) {
                            return new VectorLabel(nn.self().toString());
                        }
                    },
				VectorSensor.class, new BiFunction<VectorSensor, Object, Surface>() {
                        @Override
                        public Surface apply(VectorSensor v, Object relation) {
                            return new VectorSensorChart(v, n);
                        }
                    },
				//GameLoop.class, (GameLoop g, Object relation) -> g.components()
				DurSurface.class, new BiFunction<Object, Object, Surface>() {
                        @Override
                        public Surface apply(Object x, Object y) {
                            return null;
                        }
                    },
				Concept.class, new BiFunction<Concept, Object, Surface>() {
                        @Override
                        public Surface apply(Concept c, Object relation) {
                            return new ConceptSurface(c, n);
                        }
                    }
				//PriNode.class, (PriNode v, Object relation)-> ...

				//TODO nars specific renderers
			), new Graph2D.Graph2DRenderer<Object>() {
            @Override
            public void node(NodeVis<Object> xx, Graph2D.GraphEditing<Object> graph) {
//
//				if (xx.id instanceof PriNode) {
//					EdgeVis<Object> eID = graph.edge(xx, ((PriNode)(xx.id)).id);
//					if (eID!=null)
//						eID.weight(1f).color(0.2f, 0.2f, 0.5f);
//				}
                Node<PriNode, Object> nn = n.control.graph.node(xx.id);
                if (nn != null) {
                    //to identity object


                    for (Node<PriNode, Object> c : nn.nodes(false, true)) {
                        EdgeVis<Object> e = graph.edge(xx, c.id());
                        if (e != null) {
                            e.weight(1f).color(0.5f, 0.5f, 0.5f);
                        }
                    }

                }
            }
        });



		return DurSurface.get(g, n, g::update).every(4.0F);
	}

	public static Graph2D objectGraph(Graph2D.Graph2DRenderer<Object> renderer) {
		return new Graph2D<>()
			.render(new Graph2D.Graph2DRenderer<Object>() {
                @Override
                public void node(NodeVis<Object> xx, Graph2D.GraphEditing<Object> graph) {
                    Object x = xx.id;
                    if (x instanceof Prioritized) {
                        Prioritized node = (Prioritized) x;
                        float s = node.priElseZero();
                        xx.pri = s;
                        //((Widget)xx.the()).color.set(Math.min(s, 1), Math.min(s, 1), 0, 1);
                    } else {
                        //..infer priority of unprioitized items, ex: by graph metrics in relation to prioritized
                    }

                }
            }, renderer)
			.update(new ForceDirected2D<>() {
				@Override
				protected void size(MutableRectFloat m, float a) {
                    float q = m.node.pri;
                    float s = (float) (Math.sqrt((double) (Math.max((float) 0, q))));
					//w = Util.clamp(s * a, 2, 32);
                    float w = 10.0F + 2.0F + s * a;
					m.size(w, w);
				}
			});


	}

	static class ObjectGraphs extends Bordering {
		public static final float INNER_CONTENT_SCALE = 0.9f;
		private final Map<Class, BiFunction<?, Object, Surface>> builders;

        private final Iterable content;
		private final Graph2D graph;

		ObjectGraphs(Iterable/*<X>*/ content, Map<Class, BiFunction<?, Object, Surface>> builders, Graph2D.Graph2DRenderer<Object> renderer) {

			this.builders = builders;
            this.content = content;

			this.graph = graph(objectGraph(renderer));
			set(graph.widget());
		}

		public synchronized void update() {
			graph.set(content);
		}


//
//		public void layer(Object key) {
//
//			if (content != null)
//				computeIfAbsent(key, oo -> graph(objectGraph(renderer)).widget().pos(bounds));
//			else
//				remove(key);
//
//
//		}

		private Graph2D<Object> graph(Graph2D<Object> g) {
			g.build(new Consumer<NodeVis<Object>>() {
                        @Override
                        public void accept(NodeVis<Object> x) {
                            Object xid = x.id;
                            String label = xid.toString();

                            boolean lazy = true;
                            x.set(lazy ? Surplier.button(label, new Supplier<Surface>() {
                                @Override
                                public Surface get() {
                                    return ObjectGraphs.this.icon(xid, label);
                                }
                            }) : ObjectGraphs.this.icon(xid, label));


                            //new CheckBox(xid.toString()).on((boolean v) -> {
//if (v) {
//							Surface o = new ObjectSurface(x, new AutoBuilder<Object, Surface>(
//								2, ObjectSurface.DefaultObjectSurfaceBuilder,
//								new Map[] { ObjectSurface.builtin }));
//
//							if (o instanceof ContainerSurface) {
//								ContainerSurface c = (ContainerSurface)o;
//								int cc = c.childrenCount();
//								if (cc == 1) {
//									c.forEach(ccc -> {
//										layer(xid, List.of(ccc));
//									});
//								} else {
//									List<Surface> components = new FasterList(cc);
//
//									c.forEach(components::add);
//									layer(xid, components);
//								}
//							} else if (o!=null) {
//								layer(xid, List.of(o));
//							}
//						} else
//							layer(xid, null);


                        }
                    }
			);
			return g;
		}

		
		private Scale icon(Object xid, String label) {
			return new Scale(
				LabeledPane.the(label,
					new ObjectSurface(xid, 1,
						builders, ObjectSurface.builtin)
				), INNER_CONTENT_SCALE);
		}


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
            List<Surface> channel = n.parts(What.class).map(WhatMixer::mix).collect(toList());
			if (channel.isEmpty())
				set(new BitmapLabel("Empty"));
			else
				set(channel);
		}

		private static Surface mix(What p) {
			FloatSlider amp = new WhatPri(p);
			//Surface s = new Bordering(amp);
			return LabeledPane.the(
				p.term().toString(),
				amp
			);
		}

		void commit() {
			forEachRecursively(new Consumer<Surface>() {
                @Override
                public void accept(Surface x) {
                    if (x instanceof WhatPri) {
                        ((WhatPri) x).commit();
                    }
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
				super(p.pri(), (float) 0, 1.0F);
				this.p = p;
				on(new FloatProcedure() {
                    @Override
                    public void value(float n) {
                        next = n;
                    }
                });
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

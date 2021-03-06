package nars.gui;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.jogamp.newt.event.KeyEvent;
import jcog.TODO;
import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.table.Table;
import jcog.event.Off;
import jcog.exe.Exe;
import jcog.learn.ql.HaiQae;
import jcog.learn.ql.dqn3.DQN3;
import jcog.pri.VLink;
import jcog.signal.wave1d.IIRFilter;
import jcog.thing.Thing;
import nars.*;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.concept.Concept;
import nars.game.Game;
import nars.game.Reward;
import nars.game.sensor.Signal;
import nars.game.util.RLBooster;
import nars.gui.concept.ConceptColorIcon;
import nars.gui.concept.ConceptSurface;
import nars.gui.graph.run.BagregateConceptGraph2D;
import nars.link.TaskLink;
import nars.link.TaskLinkSnapshot;
import nars.link.TaskLinks;
import nars.op.stm.ConjClustering;
import nars.task.util.PriBuffer;
import nars.term.Termed;
import nars.time.part.DurLoop;
import nars.truth.Truth;
import nars.util.MemorySnapshot;
import net.beadsproject.beads.data.Pitch;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.grid.GridRenderer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.grid.KeyValueGrid;
import spacegraph.space2d.container.layout.TreeMap2D;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.phys.common.Color3f;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.Submitter;
import spacegraph.space2d.widget.menu.Menu;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meta.*;
import spacegraph.space2d.widget.meter.PaintUpdateMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.meter.ScatterPlot2D;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.port.FloatRangePort;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.video.Draw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.jogamp.newt.event.KeyEvent.VK_ENTER;
import static java.util.stream.Collectors.toList;
import static jcog.Util.sqrt;
import static nars.$.*;
import static nars.Op.*;
import static spacegraph.space2d.container.grid.Gridding.VERTICAL;
import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * SpaceGraph-based visualization utilities for NARchy
 */
public class NARui {

	public static Surface beliefChart(Termed x, NAR nar) {
		return /*new Widget*/(
			new MetaFrame(new BeliefTableChart(x, nar))
		);
	}

	public static Surface beliefCharts(NAR nar, Termed... x) {
		return beliefCharts(nar, ArrayIterator.iterable(x));
	}

	public static Surface beliefCharts(NAR nar, Iterable<? extends Termed> ii) {
		return new Gridding(Lists.newArrayList(StreamSupport.stream(ii.spliterator(), false).map(i -> beliefChart(i, nar))
			.collect(toList())));
	}

	public static Surface top(NAR n) {
		return new Bordering(
			new Splitting(
				attentionUI(n),
				0.5f, false,
				new TabMenu(menu(n)/* , new WallMenuView() */)
			).resizeable()
//                    AttentionUI.
//                        objectGraphs(n)
//                        //graphGraph(n)
		)
			.north(ExeCharts.runPanel(n))
			//.south(new OmniBox(new NarseseJShellModel(n))) //+50mb heap
			;
	}

	public static HashMap<String, Supplier<Surface>> parts(Thing p) {
		return (HashMap<String, Supplier<Surface>>) p.partStream().collect(Collectors.toMap(Object::toString, new Function<Object, Supplier<Surface>>() {
            @Override
            public Supplier<Surface> apply(Object s) {
                return new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new ObjectSurface(s);
                    }
                };
            }
        }, new BinaryOperator<Supplier<Surface>>() {
            @Override
            public Supplier<Surface> apply(Supplier<Surface> a, Supplier<Surface> b) {
                return b;
            }
        }, (Supplier<HashMap<String, Supplier<Surface>>>) HashMap::new));
	}

	public static HashMap<String, Supplier<Surface>> menu(NAR n) {
		Map<String, Supplier<Surface>> m = Map.of(
			//"shl", () -> new ConsoleTerminal(new TextUI(n).session(10f)),
			"nar", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new ObjectSurface(n, 2);
                    }
                },
			"on", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new ObjectSurface(n.whens().entrySet(), 2);
                    }
                },
			"exe", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return ExeCharts.exePanel(n);
                    }
                },
			"val", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return ExeCharts.valuePanel(n);
                    }
                },
			"mem", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return MemEdit(n);
                    }
                },
//                "how", () -> ExeCharts.howChart(n),
			//"can", () -> ExeCharts.causeProfiler(n),
			//ExeCharts.focusPanel(n),
			///causePanel(n),
			"svc", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new PartsTable(n);
                    }
                },
			"cpt", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new ConceptBrowser(n);
                    }
                }
		);
		HashMap<String, Supplier<Surface>> mm = new HashMap<>() {{
			putAll(m);
			put("snp", new Supplier<Surface>() {
                @Override
                public Surface get() {
                    return memoryView(n);
                }
            });
			put("tsk", new Supplier<Surface>() {
                @Override
                public Surface get() {
                    return taskView(n);
                }
            });
//            put("mem", () -> ScrollGrid.list(
//                (int x, int y, Term v) -> new PushButton(m.toString()).click(() ->
//                        window(
//                                ScrollGrid.list((xx, yy, zm) -> new PushButton(zm.toString()), n.memory.contents(v).collect(toList())), 800, 800, true)
//                ),
//                n.memory.roots().collect(toList())
//                )
//        );
		}};

		return mm;
	}

//    private static Surface priView(NAR n) {
//        TaskLinks cc = n.attn;
//
//        return Splitting.row(
//                new BagView<>(cc.links, n),
//                0.2f,
//                new Gridding(
//                     new ObjectSurface(
////                        new XYSlider(
////                                cc.activationRate
//                                cc.decay
//                                //.subRange(1/1000f, 1/2f)
//                        ) {
////                            @Override
////                            public String summaryX(float x) {
////                                return "forget=" + n4(x);
////                            }
////
////                            @Override
////                            public String summaryY(float y) {
////                                return "activate=" + n4(y);
////                            }
//                        },
//
//                        new PushButton("Print", () -> {
//                            Appendable a = null;
//                            try {
//                                a = TextEdit.out().append(
//                                        Joiner.on('\n').join(cc.links)
//                                );
//                                window(a, 800, 500);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }),
//                        new PushButton("Clear", () -> cc.links.clear())
//                )
//        );
//
//    }

	public static Surface MemEdit(NAR nar) {
		return new Gridding(
			memLoad(nar),
			memSave(nar)
//                new PushButton("Prune Beliefs", () -> {
//                    nar.runLater(() -> {
//                        //nar.logger.info("Belief prune start");
////                        final long scaleFactor = 1_000_000;
//                        //Histogram i = new Histogram(1<<20, 5);
//                        Quantiler q = new Quantiler(128 * 1024);
//                        long now = nar.time();
//                        float dur = nar.dur();
//                        nar.tasks(true, false, false, false).forEach(t ->
//                                {
//                                    try {
//                                        float c = (float) w2cSafe(t.evi(now, dur));
//                                        //i.recordValue(Math.round(c * scaleFactor));
//                                        q.add(c);
//                                    } catch (Throwable e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                        );
//                        //System.out.println("Belief evidence Distribution:");
//                        //Texts.histogramPrint(i, System.out);
//
//                        //float confThresh = i.getValueAtPercentile(50)/ scaleFactor;
//                        float confThresh = q.quantile(0.5f);
//                        final int[] removed = {0};
//                        nar.tasks(true, false, false, false, (c, t) -> {
//                            try {
//                                if (w2cSafe(t.evi(now, dur)) < confThresh)
//                                    if (c.remove(t))
//                                        removed[0]++;
//                            } catch (Throwable e) {
//                                e.printStackTrace();
//                            }
//                        });
//                        //nar.logger.info("Belief prune finish: {} tasks removed", removed[0]);
//                    });
//                })

		);

	}

	public static Surface memLoad(NAR nar) {
		return new VectorLabel("Load: TODO");
	}

	public static Surface memSave(NAR nar) {
		TextEdit path = new TextEdit(40);
		try {
			path.text(Files.createTempFile(nar.self().toString(), "" + System.currentTimeMillis()).toAbsolutePath().toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Object currentMode = null;
		ButtonSet mode = new ButtonSet(ButtonSet.Mode.One,
			new CheckBox("txt"), new CheckBox("bin")
		);
		return new Gridding(
			path,
			new Gridding(
				mode,
				new PushButton("save").clicked(new Runnable() {
                    @Override
                    public void run() {
                        Exe.runLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    nar.output(new File(path.text()), false);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                    }
                })
			));
	}

	public static Surface taskView(NAR n) {
		List<Predicate<Task>> filter = new CopyOnWriteArrayList<>();
		Consumer<Task> printer = new Consumer<Task>() {
            @Override
            public void accept(Task t) {
                if (Util.and(t, (Iterable) filter))
                    System.out.println(t);
            }
        };

		return LabeledPane.the("Trace",
			grid(
				grid(
					new CheckBox("Belief").on(taskTrace(n, BELIEF, printer)),
					new CheckBox("Goal").on(taskTrace(n, GOAL, printer)),
					new CheckBox("Question").on(taskTrace(n, QUESTION, printer)),
					new CheckBox("Quest").on(taskTrace(n, QUEST, printer))
				),
				grid(
					new CheckBox("Not Eternal").on(taskFilter(filter, new Predicate<Task>() {
                        @Override
                        public boolean test(Task x) {
                            return !x.isEternal();
                        }
                    })),
					new CheckBox("Not Signal").on(taskFilter(filter, new Predicate<Task>() {
                        @Override
                        public boolean test(Task x) {
                            return !(x instanceof Signal);
                        }
                    })),
					new CheckBox("Not Input").on(taskFilter(filter, new Predicate<Task>() {
                        @Override
                        public boolean test(Task x) {
                            return x.stamp().length > 1;
                        }
                    }))
					//TODO priority and complexity sliders
				)
			)
		);
	}

	static BooleanProcedure taskFilter(List<Predicate<Task>> ff, Predicate<Task> f) {
		return new BooleanProcedure() {
			@Override
			public synchronized void value(boolean on) {
				if (on) {
					ff.add(f);
				} else {
					boolean rem = ff.remove(f);
					assert (rem);
				}
			}
		};
	}


	static BooleanProcedure taskTrace(NAR n, byte punc, Consumer<Task> printer) {
		return new BooleanProcedure() {

			private Off off;

			@Override
			public synchronized void value(boolean b) {
				if (b) {
					assert (off == null);
					off = n.onTask(printer, punc);
				} else {
					assert (off != null);
					off.close();
					off = null;
				}
			}
		};
	}

//    public static Surface taskTable(NAR n) {
//
//        int cap = 32;
//        float rate = 1f;
//
//        CheckBox updating = new CheckBox("Update");
//        updating.on(true);
//
//        /** TODO make multithread better */
//        PLinkArrayBag<Task> b = new PLinkArrayBag<>(PriMerge.replace, cap);
//        List<Task> taskList = new FasterList();
//
//        ScrollXY tasks = ScrollXY.listCached(t ->
//                        new Splitting<>(new FloatGuage(0, 1, t::priElseZero),
//                                new PushButton(new VectorLabel(t.toStringWithoutBudget())).click(() -> {
//                                    conceptWindow(t, n);
//                                }),
//                                false, 0.1f),
//                taskList, 64);
//        tasks.view(1, cap);
//
//        TextEdit0 input = new TextEdit0(16, 1);
//        input.onKey((k) -> {
//            if (k.getKeyType() == KeyType.Enter) {
//                //input
//            }
//        });
//
//
//        Surface s = new Splitting(
//                tasks,
//                new Gridding(updating, input /* ... */)
//                , 0.1f);
//
//        Off onTask = n.onTask((t) -> {
//            if (updating.on()) {
//                b.put(new PLinkHashCached<>(t, t.priElseZero() * rate));
//            }
//        });
//        return DurSurface.get(s, n, (nn) -> {
//
//        }, (nn) -> {
//            if (updating.on()) {
//                synchronized (tasks) {
//                    taskList.clear();
//                    b.commit();
//                    b.forEach(x -> taskList.add(x.get()));
//                    tasks.update();
//                }
//            }
//        }, (nn) -> {
//            onTask.off();
//        });
//    }

	private static Surface memoryView(NAR n) {

		return new ScrollXY<>(new KeyValueGrid(new MemorySnapshot(n).byAnon),
                new GridRenderer<Object>() {
                    @Override
                    public Surface apply(int x, int y, Object v) {
                        if (x == 0) {
                            return new PushButton(v.toString()).clicked(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                        } else {
                            return new VectorLabel(((Collection) v).size() + " concepts");
                        }
                    }
                });
	}

	public static void conceptWindow(String t, NAR n) {
		conceptWindow(INSTANCE.$$(t), n);
	}

	public static void conceptWindow(Termed t, NAR n) {
		SpaceGraph.window(new ConceptSurface(t, n), 500, 500);
	}

	public static Surface game(Game a) {

		Iterable<? extends Concept> rewards = new Iterable<Concept>() {
            @NotNull
            @Override
            public Iterator<Concept> iterator() {
                return a.rewards.stream().flatMap(new Function<Reward, Stream<? extends Concept>>() {
                    @Override
                    public Stream<? extends Concept> apply(Reward r) {
                        return StreamSupport.stream(r.spliterator(), false);
                    }
                }).iterator();
            }
        };
		Iterable<? extends Concept> actions = a.actions;

		Menu aa = new TabMenu(Map.of(
			a.toString(), new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new ObjectSurface(a, 3);
                    }
                },

			"stat", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new Gridding(
                                new TriggeredSurface<>(
                                        new Plot2D(512, Plot2D.Line)
                                                .add("Happy", a::happiness),
                                        a::onFrame, Plot2D::commit),
                                new TriggeredSurface<>(
                                        new Plot2D(512, Plot2D.Line)
                                                .add("Dex+0", a::dexterity),
                                        a::onFrame, Plot2D::commit),
                                new TriggeredSurface<>(
                                        new Plot2D(512, Plot2D.Line)
                                                .add("Coh", a::coherency),
                                        a::onFrame, Plot2D::commit)
                        );
                    }
                },

//                        .addAt("Dex+2", () -> a.dexterity(a.now() + 2 * a.nar().dur()))
//                        .addAt("Dex+4", () -> a.dexterity(a.now() + 4 * a.nar().dur())), a),
			"reward", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return NARui.beliefCharts(a.nar(), rewards);
                    }
                },
			"actions", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return NARui.beliefCharts(a.nar(), actions);
                    }
                }
		));
		return LabeledPane.the(a.id.toString(), aa);
	}

	public static Gridding beliefIcons(NAR nar, List<? extends Termed> c) {

		BiConsumer<Concept, spacegraph.space2d.phys.common.Color3f> colorize = new BiConsumer<Concept, Color3f>() {
            @Override
            public void accept(Concept concept, Color3f color) {
                if (concept != null) {

                    @Nullable Truth b = nar.beliefTruth(concept, nar.time());
                    if (b != null) {
                        float f = b.freq();
                        float conf = b.conf();
                        float a = 0.25f + conf * 0.75f;
                        color.set((1.0F - f) * a, f * a, (float) 0);
                        return;
                    }
                }
                color.set(0.5f, 0.5f, 0.5f);
            }
        };
		ArrayList<ConceptColorIcon> d = new ArrayList<>();
		for (Termed x : c) {
			ConceptColorIcon conceptColorIcon = new ConceptColorIcon(x.term(), nar, colorize);
			d.add(conceptColorIcon);
		}
		return grid((Iterable<ConceptColorIcon>) d.iterator());
	}

	public static TextEdit newNarseseInput(NAR n, Consumer<Task> onTask, Consumer<Exception> onException) {
		TextEdit input = new TextEdit(16, 1);
		input.onKeyPress(new Consumer<KeyEvent>() {
            @Override
            public void accept(KeyEvent k) {
                if ((int) k.getKeyCode() == (int) VK_ENTER) {
                    String s = input.text();
                    input.text("");
                    try {
                        List<Task> t = n.input(s);
                        for (Task task : t) {
                            onTask.accept(task);
                        }
                    } catch (Narsese.NarseseException e) {
                        onException.accept(e);
                    }
                }
            }
        });
		return input;
	}

	public static Surface clusterView(ConjClustering c, NAR n) {

		ScatterPlot2D.ScatterPlotModel<VLink<Task>> model = new ScatterPlot2D.SimpleXYScatterPlotModel<>() {


			final float[] c = new float[4];
			private long now = n.time();

			@Override
			public void start() {
				now = n.time();
			}

			@Override
			public void coord(VLink<Task> v, float[] target) {
				Task t = v.get();
				target[0] = (float) (t.mid() - now); //to be certain of accuracy with 32-bit reduced precision assigned from long
				target[1] = t.priElseZero();
			}

			@Override
			public String label(VLink<Task> id) {
				return id.get()
					.term().toString();
				//toStringWithoutBudget();
			}

			@Override
			public float pri(VLink<Task> v) {
				return v.priElseZero();
			}

			@Override
			public void colorize(VLink<Task> v, NodeVis<VLink<Task>> node) {
				int centroid = v.centroid;

				float a = 0.8f;//v.priElseZero() * 0.5f + 0.5f;
				if (centroid >= 0) {
					Draw.colorHash(centroid, c, 1.0F, 0.75f + 0.25f * v.priElseZero(), a);
					node.color(c[0], c[1], c[2], c[3]);
				} else {
					node.color(0.5f, 0.5f, 0.5f, a); //unassigned
				}
			}

			@Override
			public float width(VLink<Task> v, int population) {
				Task t = v.get();
				return (float) ((long) t.term().eventRange() + t.range()) / ((float) population * 50f);
				//return (0.5f + v.get().priElseZero()) * 1/20f;
			}

			@Override
			public float height(VLink<Task> v, int population) {
				return 1.0F / ((float) population * 1f);
			}
		};

		ScatterPlot2D<VLink<Task>> s = new ScatterPlot2D<>(model);
		return DurSurface.get(s, n, new Runnable() {
            @Override
            public void run() {

                s.set(c.data.bag); //Iterable Concat the Centroids as dynamic VLink's

            }
        }).every();
	}

	public static Surface taskBufferView(PriBuffer b, NAR n) {
		Plot2D plot = new Plot2D(256, Plot2D.Line).add("load", b::load, (float) 0, 1.0F);
		DurSurface plotSurface = DurSurface.get(plot, n, plot::commit);
		Gridding g = new Gridding(
			plotSurface,
			new MetaFrame(b),
			new Gridding(
				new FloatRangePort(
					DurLoop.cache(b::load, (float) 0, 1.0F, 1.0F, n).getOne(),
					"load"
				)
			)
		);
		if (b instanceof PriBuffer.BagTaskBuffer)
			g.add(new BagView(((PriBuffer.BagTaskBuffer) b).tasks, n));

		return g;
	}


	public static Surface tasklinkSpectrogram(What w, int history) {
		return tasklinkSpectrogram(((TaskLinkWhat) w).links.links, history, w.nar);
	}

	public static Surface tasklinkSpectrogram(Table<?, TaskLink> b, int history, NAR n) {
		return tasklinkSpectrogram(n, b, history, b.capacity());
	}

	public static Surface attentionUI_2(NAR n) {
		//TODO
		return new BagView(n.what, n);
	}

	public static Surface attentionUI(NAR n) {
		//TODO watch for added and removed What's for live update

		Map<String, Supplier<Surface>> global = new HashMap();
		global.put("Attention", new Supplier<Surface>() {
            @Override
            public Surface get() {
                return AttentionUI.objectGraphs(n);
            }
        });
		global.put("What", new Supplier<Surface>() {
            @Override
            public Surface get() {
                return AttentionUI.whatMixer(n);
            }
        });


		Map<String, Supplier<Surface>> attentions = new HashMap();
		for (What v : n.what) {
			attentions.put(v.id.toString(), new Supplier<Surface>() {
                @Override
                public Surface get() {
                    return attentionUI(v);
                }
            });
		}
		TabMenu atMenu = new TabMenu(attentions);
		return new Splitting(new TabMenu(global), 0.25f, atMenu).horizontal(true).resizeable();
	}

	public static Surface attentionUI(What w) {
		Bordering m = new Bordering();
		NAR n = w.nar;
		TaskLinks attn = ((TaskLinkWhat) w).links;
		m.center(new TabMenu(Map.of(
			"Input", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return taskBufferView(w.inBuffer, n);
                    }
                },
			"Spectrum", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return tasklinkSpectrogram(w, 300);
                    }
                },
			"Histogram", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new BagView(attn.links, n);
                    }
                },
			"ConceptGraph", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return BagregateConceptGraph2D.get(attn, n);
                    }
                },
			"TaskList", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new TaskListView(w, 32);
                    }
                },
			"ConceptList", new Supplier<Surface>() {
                    @Override
                    public Surface get() {
                        return new ConceptListView(w, 32);
                    }
                }
		)));
		m.south(new ObjectSurface(attn));
		m.west(new Gridding(
			new PushButton("Clear").clicked(w::clear), //TODO n::clear "Clear All"
			Submitter.text("Load", new Consumer<String>() {
                @Override
                public void accept(String t) {
                    throw new TODO();
                }
            }),
			Submitter.text("Save", new Consumer<String>() {
                @Override
                public void accept(String t) {
                    throw new TODO(); //tagging
                }
            }),
			new PushButton("List").clicked(new Runnable() {
                @Override
                public void run() {
                    attn.links.bag.print();
                }
            }) //TODO better

		));
//        m.east(new Gridding(
//                //TODO interactive filter widgets
//        ));

		return m;
	}

	public static Surface tasklinkSpectrogram(NAR n, Table<?, nars.link.TaskLink> active, int history, int width) {

		//mode select menu
		Gridding m = new Gridding();

		Spectrogram s = new Spectrogram(true, history, width);


		Bordering Z = new Bordering(s).west(m);

		var tls = new TaskLinkSnapshot(active) {
			final int[] opColors = new int[]{
				Draw.rgbInt(1.0F, (float) 0, (float) 0),
				Draw.rgbInt((float) 0, 1.0F, (float) 0),
				Draw.rgbInt((float) 0, (float) 0, 1.0F),
				Draw.rgbInt(0.5f, 0.5f, 0f),
				Draw.rgbInt(0.5f, 0f, 0.5f),
				Draw.rgbInt(0f, 0.5f, 0.5f),
				Draw.rgbInt(0.5f, 0.5f, 0.5f), //TODO
				Draw.rgbInt(0.5f, 0.5f, 0.5f),//TODO
				Draw.rgbInt(0.5f, 0.5f, 0.5f),//TODO
				Draw.rgbInt(0.5f, 0.5f, 0.5f),//TODO
				Draw.rgbInt(0.5f, 0.5f, 0.5f),//TODO
				Draw.rgbInt(0.5f, 0.5f, 0.5f),//TODO
				Draw.rgbInt(0.5f, 0.5f, 0.5f),
				Draw.rgbInt(0.5f, 0.5f, 0.5f),
				Draw.rgbInt(0.5f, 0.5f, 0.5f),
				Draw.rgbInt(0.5f, 0.5f, 0.5f)
			};
			final IntToIntFunction opColor = new IntToIntFunction() {
                @Override
                public int valueOf(int _x) {
                    TaskLink x = items[_x];
                    if (x == null) return 0;
                    Op o = x.term().op();
                    return opColors[(int) o.id];
                }
            };
			final IntToIntFunction volColor = new IntToIntFunction() {
                @Override
                public int valueOf(int _x) {
                    TaskLink x = items[_x];
                    if (x == null) return 0;
                    float v = (float) Math.log((double) (1 + x.term().volume()));
                    return Draw.colorHSB(v / 10f, 0.5f + 0.5f * v / 10f, v / 10f); //TODO
                }
            };
			final IntToIntFunction puncColor = new IntToIntFunction() {
                @Override
                public int valueOf(int _x) {
                    TaskLink x = items[_x];
                    if (x == null)
                        return 0;

                    float r = x.priPunc(BELIEF);
                    float g = x.priPunc(GOAL);
                    float b = (x.priPunc(QUESTION) + x.priPunc(QUEST)) / 2.0F;
                    return Draw.rgbInt(r, g, b);
                }
            };

			IntToIntFunction color = puncColor;

			{
				m.set(
					new PushButton("Punc", new Runnable() {
                        @Override
                        public void run() {
                            color = puncColor;
                        }
                    }),
					new PushButton("Op", new Runnable() {
                        @Override
                        public void run() {
                            color = opColor;
                        }
                    }),
					new PushButton("Vol", new Runnable() {
                        @Override
                        public void run() {
                            color = volColor;
                        }
                    })
				);
			}

			{

				Z.south(new SonificationPanel() {

					/** TODO use BiQuadFilter */
					IIRFilter filter;

					FloatSlider freqSlider = new FloatSlider("freq", 500.0F, 10.0F, 1700.0F);
					FloatSlider ampSlider = new FloatSlider("amp", 1f, (float) 0, 1.0F);
					FloatSlider filterFreq = new FloatSlider("filt", 800.0F, 60.0F, 4000.0F);

					{
						add(freqSlider, ampSlider, filterFreq);

					}

					@Override
					protected void sound(float[] buf, float readRate) {

						if (filter == null)
							filter = new IIRFilter.LowPassSP(filterFreq.asFloat(), readRate);

						filter.setFrequency(filterFreq.asFloat());


						TaskLink[] ii = items;
						int n = ii.length;
						Random rng = ThreadLocalRandom.current();
						float baseFreq = freqSlider.asFloat();
						float vol = ampSlider.asFloat();
						for (int i = 0; i < n; i++) {
							TaskLink x = items[i];
							if (x == null) continue;
							float amp = vol * (float) ((Math.exp((double) (x.pri() * 10.0F)) - 1.0) / (double) Util.sqrt((float) n));
							Op o = x.op();

							//stupid grain synth
							float f = baseFreq * (float) (1 + Pitch.forceToScale((int) o.id + 1, Pitch.dorian));
							float grainTime = Util.lerp(Math.min(1.0F, (float) x.term().volume() / 30f), 0.1f, 0.33f);
							int sw = Math.round((float) buf.length * grainTime);
							int ss = (int) (rng.nextFloat() * (float) (buf.length - sw - 1));
							int se = ss + sw;
							for (int s = ss; s < se; s++) {
								float env = (float) (2 * Math.min(Math.abs(s - ss), Math.abs(s - se))) / ((float) sw + 1f); //triangular
								buf[s] += amp * (float) Math.sin((double) (f * (float) s * 2.0F * 1f / readRate)) * env;
							}
						}

						filter.process(buf, 0, buf.length);
					}
				});
			}

			@Override
			protected void next() {
				s.next(color);
			}
		};


		return DurSurface.get(Z, n, tls);
	}

	public static Surface rlbooster(RLBooster rlb) {

//            return new Gridding(
//                Stream.of(((HaiQ) (rlb.agent)).q,((HaiQ) (rlb.agent)).et).map(
//                        l -> {
//
//                            BitmapMatrixView i = new BitmapMatrixView(l);
//                            rlb.env.onFrame(i::update);
//                            return i;
//                        }
//                ).collect(toList()));

		Plot2D plot = new Plot2D(200, Plot2D.Line);
		Gridding charts = new Gridding();
		if (rlb.agent instanceof HaiQae) {
			HaiQae q = (HaiQae) rlb.agent;
			charts.add(
				new ObjectSurface(q),
				new Gridding(VERTICAL,
					new PaintUpdateMatrixView(rlb.input),
					new PaintUpdateMatrixView(q.ae.W),
					new PaintUpdateMatrixView(q.ae.y),
					new PaintUpdateMatrixView(rlb.actionFeedback)
				),
				new Gridding(VERTICAL,
					new PaintUpdateMatrixView(q.q),
					new PaintUpdateMatrixView(q.et)
				)
			);
		}
		if (rlb.agent instanceof DQN3) {
			DQN3 d = (DQN3) rlb.agent;
			charts.add(
				new ObjectSurface(d),
				new Gridding(VERTICAL,
					new PaintUpdateMatrixView(new Supplier<double[]>() {
                        @Override
                        public double[] get() {
                            return d.input;
                        }
                    }, d.inputs),
					matrix(d.W1.w),
					matrix(d.B1.w),
//                            matrix(d.W1.dw),
					matrix(d.W2.w),
					matrix(d.B2.w)
//                            matrix(d.W2.dw)
				)
			);
			Plot2D dqn3Plot = new Plot2D(200, Plot2D.Line);
			dqn3Plot.add("DQN3 Err", new DoubleSupplier() {
                @Override
                public double getAsDouble() {
                    return d.lastErr;
                }
            });
			charts.add(dqn3Plot);
			rlb.env.onFrame(dqn3Plot::commit);
		}
		AtomicDouble rewardSum = new AtomicDouble();
		plot.add("Reward", new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return rewardSum.getAndSet((double) 0); //clear
            }
        }, (float) 0, (float) +1);

		rlb.env.onFrame(new Runnable() {
            @Override
            public void run() {
                rewardSum.addAndGet(rlb.lastReward);
                plot.commit();
            }
        });

		charts.add(plot);
		return charts;


//            window(
//                    new LSTMView(
//                            ((LivePredictor.LSTMPredictor) ((DQN2) rlb.agent).valuePredict).lstm.agent
//                    ), 800, 800
//            );
//
////            window(new Gridding(
////                Stream.of(((DQN2) (rlb.agent)).valuePredict.layers).map(
////                        l -> {
////
////                            BitmapMatrixView i = new BitmapMatrixView(l.input);
////                            BitmapMatrixView w = new BitmapMatrixView(l.weights);
////                            BitmapMatrixView o = new BitmapMatrixView(l.output);
////
////                            a.onFrame(i::update);
////                            a.onFrame(w::update);
////                            a.onFrame(o::update);
////
////                            return new Gridding(i, w, o);
////                        }
////                ).collect(toList()))
////            , 800, 800);

	}

//    @Deprecated public static void agentOld(NAgent a) {
//        NAR nar = a.nar();
//        //nar.runLater(() -> {
//            window(
//                    grid(
//                            new ObjectSurface(a),
//
//                            beliefCharts(a.actions(), a.nar()),
//
//                            new EmotionPlot(64, a),
//                            grid(
//
//                                    new TextEdit() {
//                                        @Override
//                                        protected void onKeyEnter() {
//                                            String s = text();
//                                            text("");
//                                            try {
//                                                nar.conceptualize(s);
//                                            } catch (Narsese.NarseseException e) {
//                                                e.printStackTrace();
//                                            }
//                                            conceptWindow(s, nar);
//                                        }
//                                    }.surface(),
//
//
//                                    new PushButton("dump", () -> {
//                                        try {
//                                            nar.output(Files.createTempFile(a.toString(), "" + System.currentTimeMillis()).toFile(), false);
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }),
//
//                                    new PushButton("clear", () -> {
//                                        nar.runLater(NAR::clear);
//                                    }),
//
//
//                                    new WindowToggleButton("top", () -> new ConsoleTerminal(new nars.TextUI(nar).session(10f))),
//
//                                    new WindowToggleButton("concept graph", () -> {
//                                        DynamicConceptSpace sg;
//                                        SpaceGraphPhys3D s = new SpaceGraphPhys3D<>(
//                                                sg = new DynamicConceptSpace(nar, () -> nar.attn.active().iterator(),
//                                                        128, 16)
//                                        );
//                                        EdgeDirected3D fd = new EdgeDirected3D();
//                                        s.dyn.addBroadConstraint(fd);
//                                        fd.condense.setAt(fd.condense.get() * 8);
//
//                                        s.addAt(new SubOrtho(
//
//                                                grid(new ObjectSurface<>(fd), new ObjectSurface<>(sg.vis))) {
//
//                                        }.posWindow(0, 0, 1f, 0.2f));
//
//
//
//
//                                        s.camPos(0, 0, 90);
//                                        return s;
//                                    })
//
//
//
//
//
//
//
//
//
//
//
//
//
//
////
////                                    a instanceof NAgentX ?
////                                            new WindowToggleButton("vision", () -> grid(((NAgentX) a).sensorCam.stream().map(cs -> new AspectAlign(
////                                                    new CameraSensorView(cs, a).withControls(), AspectAlign.Align.Center, cs.width, cs.height))
////                                                    .toArray(Surface[]::new))
////                                            ) : grid()
//                            )
//                    ),
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//                    900, 600);
//        //});
//    }


	//    static class NarseseJShellModel extends OmniBox.JShellModel {
//        private final NAR nar;
//
//        public NarseseJShellModel(NAR n) {
//            this.nar = n;
//        }
//
//        @Override
//        public void onTextChange(String text, int cursorPos, MutableContainer target) {
//            super.onTextChange(text, cursorPos, target);
//        }
//
//        @Override
//        public void onTextChangeControlEnter(String text, MutableContainer target) {
//            text = text.trim();
//            if (text.isEmpty())
//                return;
//            try {
//                nar.input(text);
//            } catch (Narsese.NarseseException e) {
//                super.onTextChangeControlEnter(text, target);
//            }
//        }
//
//    }

	public static PaintUpdateMatrixView matrix(double[] dw) {
		return dw.length > 2048 ?
			PaintUpdateMatrixView.scroll(dw, false, 64, 8) :
			new PaintUpdateMatrixView(new Supplier<double[]>() {
                @Override
                public double[] get() {
                    return dw;
                }
            }, dw.length, dw.length / Math.max(1, (int) Math.ceil((double) sqrt((float) dw.length))));
	}

	public static <X> Surface focusPanel(Iterable<X> all, FloatFunction<X> pri, Function<X, String> str, NAR nar) {

		Graph2D<X> s = new Graph2D<X>().render(new Graph2D.Graph2DRenderer<X>() {
            @Override
            public void node(NodeVis<X> node, Graph2D.GraphEditing<X> g) {
                X c = node.id;

                //float p = Math.max(Math.max(epsilon, c.pri()), epsilon);
                float p = pri.floatValueOf(c);
                float v = p; //TODO support separate color fucntion
                node.color(p, v, 0.25f);


                //Graph2D G = node.parent(Graph2D.class);
//                float parentRadius = node.parent(Graph2D.class).radius(); //TODO cache ref
//                float r = (float) ((parentRadius * 0.5f) * (sqrt(p) + 0.1f));

                final float epsilon = 0.01f;
                node.pri = Math.max(epsilon, p);
            }
        })
			//.layout(fd)
			.update(new TreeMap2D<>())
			.build(new Consumer<NodeVis<X>>() {
                @Override
                public void accept(NodeVis<X> node) {
                    node.set(new Scale(new ExeCharts.CausableWidget<>(node.id, str.apply(node.id)), 0.9f));
                }
            });


		return DurSurface.get(
			new Splitting(s, 0.1f, s.configWidget()),
			nar,
                new Runnable() {
                    @Override
                    public void run() {
                        s.set(all);
                    }
                });
	}


}

package nars.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import nars.util.Iterative;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;
import spacegraph.*;
import spacegraph.layout.Flatten;
import spacegraph.math.Color3f;
import spacegraph.math.v3;
import spacegraph.obj.layout.Grid;
import spacegraph.obj.layout.Stacking;
import spacegraph.obj.widget.Label;
import spacegraph.obj.widget.LabeledPane;
import spacegraph.obj.widget.Plot2D;
import spacegraph.render.Draw;
import spacegraph.render.SpaceGraph2D;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static spacegraph.obj.layout.Grid.*;

/**
 * SpaceGraph-based visualization utilities for NAR analysis
 */
public class Vis {
    public static void newBeliefChartWindow(NAgent narenv, long window) {
        Grid chart = agentActions(narenv, window);
        new SpaceGraph().add(new Ortho(chart).maximize()).show(800, 600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, Term... t) {
        Grid chart = agentActions(nar, Lists.newArrayList(t), window);
        new SpaceGraph().add(new Ortho(chart).maximize()).show(800, 600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, List<? extends Termed> t) {
        Grid chart = agentActions(nar, t, window);
        new SpaceGraph().add(new Ortho(chart).maximize()).show(800, 600);
    }


    public static Grid agentActions(NAR nar, Iterable<? extends Termed> cc, long window) {
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> actionTables = $.newArrayList();
        for (Termed c : cc) {
            actionTables.add(new BeliefTableChart(nar, c, btRange));
        }

        return new Grid(VERTICAL, actionTables);
    }

    public static Grid agentActions(NAgent a, long window) {
        List<Termed> ii = Lists.newArrayList();
        ii.addAll(a.actions);
//        ii.add(a.happy);
//        ii.add(a.joy);

        NAR nar = a.nar;

        return beliefCharts(window, ii, nar);
    }

    public static Grid beliefCharts(long window, List<? extends Termed> ii, NAR nar) {
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> s = ii.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
        return new Grid(VERTICAL, s);
    }



    public static BagChart<Concept> conceptsTreeChart(final Default d, final int count) {
        long[] now = new long[] { d.time() };
        BagChart<Concept> tc = new BagChart<Concept>(d.core.active, count) {

            @Override
            public void accept(BLink<Concept> x, ItemVis<BLink<Concept>> y) {
                float p = x.pri();
                float ph = 0.25f + 0.75f * p;

                float r, g, b;

                Concept c = x.get();
                if (c != null) if (c instanceof Atomic) {
                    r = g = b = ph * 0.5f;
                } else {
                    float belief = 0;

                    long n = now[0];

                    @Nullable Truth bt = c.beliefs().truth(n);
                    if (bt != null)
                        belief = bt.conf();


                    float goal = 0;
                    @Nullable Truth gt = c.goals().truth(n);
                    if (gt != null)
                        goal = gt.conf();

                    if (belief > 0 || goal > 0) {
                        r = 0;
                        g = 0.25f + 0.75f * belief;
                        b = 0.25f + 0.75f * goal;
                    } /*else if (c.hasQuestions() || c.hasQuests()) {
                        r = 1; //yellow
                        g = 1/2;
                        b = 0;
                    } */ else {
                        r = g = b = 0;
                    }
                }
                else {
                    r = g = b = 0.5f;
                }

                y.update(p, r, g, b);


            }
        };

        d.onFrame(xx -> {

            //if (s.window.isVisible()) {
            now[0] = xx.time();
            tc.update();
            //}
        });

        return tc;
    }

    public static <X extends Termed> BagChart<X> items(Bag<X> bag, final Iterative d, final int count) {
        BagChart tc = new BagChart(bag, count) {
            @Override
            public void accept(BLink x, ItemVis y) {
                float p = x.pri();

                float[] f = Draw.hsb(
                        (0.3f * x.get().hashCode() / (float) Integer.MAX_VALUE),
                        .5f + 0.25f * p, 0.5f + 0.25f * p, 1f, null);
                y.update(p, f[0], f[1], f[2]);

            }
        };

        d.onFrame(xx -> {

            //if (s.window.isVisible()) {
            tc.update();
            //}
        });

        return tc;
    }

    public static Surface budgetHistogram(NAR nar, int bins) {
        if (nar instanceof Default) {
            return budgetHistogram(((Default)nar).core.active, bins);
        } else { //if (nar instance)
            //return budgetHistogram(((Default2)nar).active, bins);
            return grid(); //TODO
        }
    }

    public static Surface budgetHistogram(Bag bag, int bins) {
        //new SpaceGraph().add(new Facial(

        double[] d = new double[bins];
        return //new GridSurface(VERTICAL,
                Vis.pane("Concept Priority Distribution (0..1)", new HistogramChart(
                        ()->bag.priHistogram(d), new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f)));

//                PanelSurface.of("Concept Durability Distribution (0..1)", new HistogramChart(nar, c -> {
//                    if (c != null)
//                        return c.dur();
//                    return 0;
//                }, bins, new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))

    }

    public static Grid conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory, FloatFunction<Termed> value) {

        //TODO make a lambda Grid constructor
        Grid grid = new Grid();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
            p.add(t.toString(), () -> value.floatValueOf(t), 0f, 1f);
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onFrame(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }

    public static Grid conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory) {

        //TODO make a lambda Grid constructor
        Grid grid = new Grid();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
            p.setTitle(t.toString());
            p.add("P", () -> nar.activation(t), 0f, 1f);
            p.add("B", () -> nar.concept(t).beliefFreq(nar.time()), 0f, 1f);
            p.add("G", () -> nar.concept(t).goalFreq(nar.time()), 0f, 1f);
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onFrame(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }


    public static Grid agentBudgetPlot(NAgent t, int history) {
        return conceptLinePlot(t.nar,
                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)), history);
    }

    public static Grid emotionPlots(NAR nar, int plotHistory) {
//        Plot2D plot = new Plot2D(plotHistory, Plot2D.Line);
//        plot.add("Rwrd", reward);

        Plot2D plot1 = new Plot2D(plotHistory, Plot2D.Line);
        plot1.add("Conf", () -> nar.emotion.confident.getSum());

        Plot2D plot2 = new Plot2D(plotHistory, Plot2D.Line);
        plot2.add("Busy", () -> nar.emotion.busy.getSum());
        plot2.add("Lern", () -> nar.emotion.busy.getSum() - nar.emotion.frustration.getSum());

        Plot2D plot3 = new Plot2D(plotHistory, Plot2D.Line);
        plot3.add("Strs", () -> nar.emotion.stress.getSum());


        Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
        plot4.add("Hapy", () -> nar.emotion.happy.getSum());
        plot4.add("Sad", () -> nar.emotion.sad.getSum());

//                Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
//                plot4.add("Errr", ()->nar.emotion.errr.getSum());

        nar.onFrame(f -> {
            //plot.update();
            plot1.update();
            plot2.update();
            plot3.update();
            plot4.update();
        });

        return col(plot1, plot2, plot3, plot4);
    }

    public static Label label(String text) {
        return new Label(text);
    }

    /** ordering: first is underneath, last is above */
    public static Stacking stacking(Surface... s) {
        return new Stacking(s);
    }

    public static LabeledPane pane(String k, Surface s) {
        return new LabeledPane(k, s);
    }

    public static SpaceGraph<Term> conceptsWindow3D(NAR nar, int maxNodes, int maxEdges) {


        NARSpace n = new ConceptsSpace(nar, maxNodes, maxEdges);


        SpaceGraph<Term> s = new SpaceGraph<Term>(

                n.with(
//                        new SpaceTransform<Term>() {
//                            @Override
//                            public void update(SpaceGraph<Term> g, AbstractSpace<Term, ?> src, float dt) {
//                                float cDepth = -9f;
//                                src.forEach(s -> {
//                                    ((SimpleSpatial)s).moveZ(
//                                            s.key.volume() * cDepth, 0.05f );
//                                });
//                            }
//                        }
                        new Flatten() {
                            protected void locate(SimpleSpatial s, v3 f) {
                                f.set(s.x(), s.y(), 10 - ((Term) (s.key)).volume() * 1);
                            }
                        }
//                        //new Spiral()
//                        //new FastOrganicLayout()
                )
        );

        s.dyn.addBroadConstraint(new ForceDirected());


        return s;

    }

    public static SpaceGraph<Term> conceptsWindow2D(NAR nar, int maxNodes, int maxEdges) {
        return new SpaceGraph2D(
                new ConceptsSpace(nar, maxNodes, maxEdges).with(
                    new Flatten()
                    //new Spiral()
                    //new FastOrganicLayout()
                )
        ).with(new ForceDirected());
    }

}

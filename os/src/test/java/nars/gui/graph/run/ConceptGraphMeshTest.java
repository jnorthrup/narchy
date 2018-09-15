//package nars.gui.graph.run;
//
//import nars.NAR;
//import nars.NARS;
//import nars.concept.Concept;
//import nars.term.Term;
//import nars.test.impl.DeductiveMeshTest;
//import org.jetbrains.annotations.NotNull;
//import spacegraph.SpaceGraph;
//import spacegraph.space2d.container.ForceDirected2D;
//import spacegraph.space2d.widget.Graph2D;
//import spacegraph.space2d.widget.windo.Windo;
//
//import static nars.Op.INT;
//import static nars.Op.PROD;
//
//public class ConceptGraphMeshTest {
//    public static void main(String[] args) {
//
//        NAR n = NARS.tmp(4);
//        n.termVolumeMax.set(14);
//
//
//
//        int GW = 5;
//        int GH = 5;
//
//
//        DeductiveMeshTest mesh = new DeductiveMeshTest(n, GW, GH);
//
//        ConceptGraph2D g = new ConceptGraph2D(n) { //Iterables.transform(mesh.coords, x -> n.concept(x)), n) {
//            @Override
//            public @NotNull Graph2D.Graph2DUpdater<Concept> getLayout() {
//                return new ForceDirected2D<>() {
//                    @Override
//                    public void update(Graph2D<Concept> g, int dtMS) {
//
//                        float gw = g.w()*0.8f;
//                        float gh = g.h()*0.8f;
//
//                        g.forEachValue(n -> {
//                            Term t = n.id.term();
//                            if (t.op()==PROD && t.subs()==2 && t.sub(0).op()==INT && t.sub(1).op()==INT) {
//                                float x = ((nars.term.atom.Int)t.sub(0)).id;
//                                float y = ((nars.term.atom.Int)t.sub(1)).id;
//                                n.pos(//RectFloat2D.XYWH(
//                                        //g.x() + (gw / 2 - gw / 4) + (float) Math.random() * gw / 2f,
//                                        //g.y() + (gh / 2 - gh / 4) + (float) Math.random() * gh / 2f,
//                                        gw * (x / GW) + g.x() + gw/4,
//                                        gh * (y / GH) + g.y() + gh/4
//                                        //20, 20
//                                );
//                            } else {
//                            }
//                        });
//
//                        super.update(g, dtMS);
//
//
//                    }
//                };
//            }
//        };
//
//        SpaceGraph.window(
//
//                new Windo(g.widget())
//
//                , 1200, 800
//        );
//
//        n.startFPS(35f);
//    }
//
//
//}

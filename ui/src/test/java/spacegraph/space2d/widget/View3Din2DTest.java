package spacegraph.space2d.widget;

import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space3d.SpaceDisplayGraph3D;
import spacegraph.space3d.widget.SurfacedCuboid;
import spacegraph.test.Graph2DTest;
import spacegraph.test.WidgetTest;

class View3Din2DTest {
	public static void main(String[] args) {
		SpaceDisplayGraph3D s = new SpaceDisplayGraph3D().camPos(0, 0, 5);
//        for (int x = -10; x < 10; x++) {
//            for (int y = -10; y < 10; y++) {
//                s.add(
//                    new SimpleSpatial().move(x, y, 0).scale(0.75f).color(1, 1, 1)
//                );
//            }
//        }


		SpaceGraph.window(//new Splitting(new PushButton("y"), 0.9f, new Splitting(
			new View3Din2D(s),
			//0.1f, new PushButton("x")).resizeable()).resizeable(),
			1280, 1024);

//		s.zFar = 100;
		s.add(new SurfacedCuboid("x",
			WidgetTest.widgetDemo(),
			//new PushButton("x").clicked(()->System.out.println("x")),
			//new XYSlider(),
			//new Sketch2DBitmap(128, 128),
			//new BitmapLabel("y"),
			//new MetaFrame(new Sketch2DBitmap(32,32)),
			1, 1)
			.rotate(0, 1, 0, 0.2f, 1f)
			.rotate(0, 0, 1, 0.2f, 1f)
			.move(-1, 0, -4)
		);
		s.add(new SurfacedCuboid("y",
			new MetaFrame(new Sketch2DBitmap(512,512)), 1, 1)
			.scale(2, 1, 1)
			.rotate(1, 0, 0, -0.1f, 0.0001f)
			.rotate(0, 0, 1, -0.1f, 1f)
			.move(1, 1, 0)
		);
		s.add(new SurfacedCuboid("z",
			Graph2DTest.newSimpleGraph(),
			1, 1)
			.rotate(1, 0, 0, 0.2f, 0.0001f)
			.rotate(0, 0, 1, -0.1f, 1f)
			.move(1, -1, 0)
		);
	}
}
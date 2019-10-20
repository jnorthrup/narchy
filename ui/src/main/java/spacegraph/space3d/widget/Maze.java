package spacegraph.space3d.widget;

import jcog.math.v3;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.*;

import java.nio.ByteBuffer;

import static jcog.math.v3.v;

/**
 * TODO extend CompoundSpatial
 */
public class Maze extends CompoundSpatial {


    private final boolean[][] cells;

    public Maze(String id, int x, int y) {
        super(id);
        cells = new boolean[x][y];
        build(0,0,x-1,y-1);
    }

    @Override
    protected void create(Dynamics3D world) {

        float dx = 1, dy = 1;
        float y = 0;
        for (var c : cells) {
            float x = 0;
            for (var cc : c) {


                if (cc) {


                    var b = Dynamics3D.newBody(
                            1f, 
                            new BoxShape(0.9f, 0.9f, 0.9f), new Transform(x, y, 0),
                            +1, 
                            -1
                    );
                    b.setData(this);

                    


                    b.setDamping(0.9f, 0.5f);
                    b.setFriction(0.9f);

                    add(b);
                }

                x+=dx;

            }
            y+=dy;
        }



        CollisionShape groundShape = new BoxShape(v(20f, 20f, 10f));
        var ground = Dynamics3D.newBody(0f, groundShape, new Transform(0,0,-15), +1, -1);
        ground.setData(this);
        add(ground);


        /*ConvexShape blobShape = new BvhTriangleMeshShape(
                new TriangleIndexVertexArray(), true
        );*/
        
        










    }


    public static ConvexHullShape hull() {
        var c = new ConvexHullShape().add(
            v(-3,0,0), v(-3, 0, 3), v(-3, -3, 3), v(-3, 3, 3),
            v(0, 3, 1.5f)
        );

        return c;
    }

    protected static CollisionShape terrain(int tesselation, float height, int seed, v3 scale) {


        var count = tesselation;
        var countsq = count * count;
        var countsqp = countsq + (count * 2 + 1);

        var nsscroll = seed;
        var pvert = new v3[countsqp];

        var hdim =  count * .5f;
        var ind = ByteBuffer.allocateDirect(countsq * 24);
        var vert = ByteBuffer.allocateDirect(countsqp * 12);
        var zscl = 7.5f;
        var nscl = .09f;
        var indCount = 0;
        for (var a = 0; a < countsqp; a++) {
            var xi = a % (count + 1);
            var yi = (int) Math.floor(a / (count + 1f));
            var xx = xi - hdim;
            var zz = yi  - hdim;
            float yy = 0;
            yy += noise((nsscroll + xx + 1) * nscl, zz * nscl);
            yy += noise((nsscroll + xx + 1) * nscl, (zz + 1) * nscl);
            yy += noise((nsscroll + xx - 1) * nscl, zz * nscl);
            yy += noise((nsscroll + xx) * nscl, (zz - 1) * nscl);
            yy *= height * zscl;
            if (xi == 0 || xi == count || yi == 0 || yi == count)
                yy -= zscl * .2;
            pvert[a] = new v3(xx, yy, zz);

            vert.putFloat(xx);
            vert.putFloat(yy);
            vert.putFloat(zz);
            if (a != 0 && (xi == count || a > countsq + (count - 2))) {
                continue;
            }
            indCount += 2;
            ind.putInt(a);
            ind.putInt(a + 1);
            ind.putInt(a + count + 1);
            ind.putInt(a + 1);
            ind.putInt(a + count + 2);
            ind.putInt(a + count + 1);
        }
        var tiva = new TriangleIndexVertexArray(indCount, ind, 12, countsq, vert, 12);

        var cs = new BvhTriangleMeshShape(tiva, true);
        cs.setLocalScaling(scale);
        return cs;
    }

    private static float noise(float a, float b) {
        
        return (float)Math.random();
    }

    private static int irand(int x) {
        return (int)(Math.random() * x);
    }

        private void build(int x1, int y1, int x2, int y2) {

            for (var x = x1; x < x2; x++)
                for (var y = y1; y < y2; y++)
                    cells[x][y] = true;

            var w = x2 - x1 + 1;
            var rw = (w + 1) / 2;
            var h = y2 - y1 + 1;
            var rh = (h + 1) / 2;

            var sx = x1 + 2 * irand(rw);
            var sy = y1 + 2 * irand(rh);
            cells[sx][sy] = false;

            var finishedCount = 0;
            for (var i = 1; (i < (rw * rh * 1000)) && (finishedCount < (rw * rh)); i++) {
                var x = x1 + 2 * irand(rw);
                var y = y1 + 2 * irand(rh);
                if (!cells[x][y])
                    continue;

                var dx = (irand(2) == 1) ? (irand(2) * 2 - 1) : 0;
                var dy = (dx == 0) ? (irand(2) * 2 - 1) : 0;
                var lx = x + dx * 2;
                var ly = y + dy * 2;
                if ((lx >= x1) && (lx <= x2) && (ly >= y1) && (ly <= y2)) {
                    if (!cells[lx][ly]) {
                        cells[x][y] = false;
                        cells[x+dx][y+dy] = false;
                        finishedCount++;
                    }
                }
            }
        }


    @Override
    public float radius() {
        return 1;
    }
}

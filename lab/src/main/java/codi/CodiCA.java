package codi;

import com.jogamp.opengl.GL2;
import jcog.exe.Loop;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.video.Draw;

import java.util.Arrays;
import java.util.stream.IntStream;

public class CodiCA extends CA {
    private int CAChanged;
    private boolean SignalingInited;


    static class CodiCell {
        int Type;
        int Chromo;
        int Gate;
        int Activ;
        final int[] IOBuf = new int[6];
    }

    protected final CodiCell[][][] cell;

    private static final int BLANK = 0;
    private static final int NEURONSEED = 1;
    private static final int NEURON = 1;
    private static final int AXON = 2;
    private static final int DEND = 4;
    private static final int AXON_SIG = 2;
    private static final int DEND_SIG = 4;

    //    private static final int SIG_COL = 5;
//    private static final int ERROR_COL = 13;
    private final int[][][] ColorSpace;
//    private int ActualColor;

    public CodiCA(int w, int h , int d) {
        super(w, h, d);


        ColorSpace = new int[sizeX][sizeY][sizeZ];
        cell = new CodiCell[sizeX][sizeY][sizeZ];
        for (int ix = 0; ix < sizeX; ix++)
            for (int iy = 0; iy < sizeY; iy++)
                for (int iz = 0; iz < sizeZ; iz++)
                    cell[ix][iy][iz] = new CodiCell();
    }


    @Override
    protected void InitCA() {
        step = 0;
        CAChanged = 1;
        SignalingInited = false;
        uninitialized = true;
        for (int ix = 0; ix < sizeX; ix++)
            for (int iy = 0; iy < sizeY; iy++)
                for (int iz = 0; iz < sizeZ; iz++) {
                    ColorSpace[ix][iy][iz] = 0;
                    CodiCell cell = this.cell[ix][iy][iz];
                    cell.Type = 0;
                    cell.Activ = 0;
                    for (int i = 0; i < 6; i++)
                        cell.IOBuf[i] = 0;
                    cell.Chromo = (random.nextInt() % 256);

                    if (((ix + 1) % 2) * (iy % 2) == 1)
                        cell.Chromo = (cell.Chromo & ~3) | 12;
                    if ((ix % 2) * ((iy + 1) % 2) == 1)
                        cell.Chromo = (cell.Chromo & ~12) | 3;

                    /** add blocks every 2 cells TODO verify this is what it actually means */
                    boolean gridBlock = true;
                    if (gridBlock) {
                        if ((ix % 2) + (iy % 2) != 0)
                            cell.Chromo &= ~192;

                        if ((cell.Chromo >>> 6) == NEURONSEED)
                            if ((random.nextInt() % sizeX) < ix / 2)
                                cell.Chromo &= ~192;
                    }

                    if ((cell.Chromo >>> 6) == NEURONSEED)
                        cell.Chromo =
                                (cell.Chromo & 192) |
                                        ((cell.Chromo & 63) % 4);
                }
    }

    protected void Kicking() {


        CodiCell[][][] ca = cell;
        for (int iz = 0; iz < sizeZ; iz++)
            for (int iy = 0; iy < sizeY; iy++)
                for (int ix = 0; ix < sizeX; ix++) {

                    int[] caio = ca[ix][iy][iz].IOBuf;
                    caio[4] = iz != sizeZ - 1 ? ca[ix][iy][iz + 1].IOBuf[4] : 0;

                    caio[2] = iy != sizeY - 1 ? ca[ix][iy + 1][iz].IOBuf[2] : 0;

                    caio[0] = ix != sizeX - 1 ? ca[ix + 1][iy][iz].IOBuf[0] : 0;
                }


        for (int iz = sizeZ - 1; iz >= 0; iz--)
            for (int iy = sizeY - 1; iy >= 0; iy--)
                for (int ix = sizeX - 1; ix >= 0; ix--) {

                    int[] caio = ca[ix][iy][iz].IOBuf;
                    caio[5] = iz != 0 ? ca[ix][iy][iz - 1].IOBuf[5] : 0;

                    caio[3] = iy != 0 ? ca[ix][iy - 1][iz].IOBuf[3] : 0;

                    caio[1] = ix != 0 ? ca[ix - 1][iy][iz].IOBuf[1] : 0;
                }
    }

    private void InitSignaling() {
        SignalingInited = true;
        CodiCell[][][] ca = cell;
        for (int iz = 0; iz < sizeZ; iz++)
            for (int iy = 0; iy < sizeY; iy++)
                for (int ix = 0; ix < sizeX; ix++) {
                    CodiCell c = ca[ix][iy][iz];
                    c.Activ = 0;
                    Arrays.fill(c.IOBuf, 0, 6, 0);
                    if (c.Type == NEURON)
                        c.Activ = (random.nextInt() % 32);
                }
    }

    @Override
    protected void next() {

        if (step == 0)
            InitCA();
        step++;
        if (CAChanged == 1)
            GrowthStep();
        else {
            if (!SignalingInited)
                InitSignaling();
            SignalStep();
        }
    }

    protected int GrowthStep() {
        CAChanged = 0;
        for (int iz = 0; iz < sizeZ; iz++)
            for (int iy = 0; iy < sizeY; iy++)
                for (int ix = 0; ix < sizeX; ix++) {


                    CodiCell ca = cell[ix][iy][iz];
                    int[] caio = ca.IOBuf;
                    switch (ca.Type) {
                        case BLANK:

                            if ((ca.Chromo >>> 6) == NEURONSEED) {
                                ca.Type = NEURON;
                                CAChanged = 1;

                                ca.Gate = (ca.Chromo & 63) % 6;
                                for (int i = 0; i < 6; i++)
                                    caio[i] = DEND_SIG;
                                caio[ca.Gate] = AXON_SIG;
                                caio[(ca.Gate % 2 * -2) + 1 + ca.Gate] = AXON_SIG;
                                break;
                            }

                            int InputSum =
                                    0;
                            for (int i21 : new int[]{0, 1, 2, 3, 4, 5}) {
                                int i3 = caio[i21];
                                InputSum += i3;
                            }
                            if (InputSum == 0) break;

                            int result = 0;
                            for (int i11 : new int[]{0, 1, 2, 3, 4, 5}) {
                                int i2 = (caio[i11] & AXON_SIG);
                                result += i2;
                            }
                            InputSum =
                                    result;
                            if (InputSum == AXON_SIG) {
                                ca.Type = AXON;
                                CAChanged = 1;
                                for (int i = 0; i < 6; i++)
                                    if (caio[i] == AXON)
                                        ca.Gate = i;
                                for (int i = 0; i < 6; i++)
                                    caio[i] = ((ca.Chromo >>> i) & 1) != 0 ? AXON_SIG : 0;
                                break;
                            }
                            if (InputSum > AXON_SIG) {
                                for (int i = 0; i < 6; i++)
                                    caio[i] = 0;
                                break;
                            }

                            int sum = 0;
                            for (int v : new int[]{0, 1, 2, 3, 4, 5}) {
                                int i1 = (caio[v] & DEND_SIG);
                                sum += i1;
                            }
                            InputSum =
                                    sum;
                            if (InputSum == DEND_SIG) {
                                CAChanged = 1;
                                ca.Type = DEND;
                                for (int i = 0; i < 6; i++)
                                    if ((caio[i]) != 0)
                                        ca.Gate = ((i % 2) * -2) + 1 + i;
                                for (int i = 0; i < 6; i++)
                                    caio[i] = ((ca.Chromo >>> i) & 1) != 0 ? DEND_SIG : 0;
                                break;
                            }

                            for (int i = 0; i < 6; i++)
                                caio[i] = 0;
                            break;
                        case NEURON:
                            for (int i = 0; i < 6; i++)
                                caio[i] = DEND_SIG;
                            caio[ca.Gate] = AXON_SIG;
                            caio[((ca.Gate % 2) * -2) + 1 + ca.Gate] = AXON_SIG;
                            break;
                        case AXON:
                            for (int i = 0; i < 6; i++)
                                caio[i] = ((ca.Chromo >>> i) & 1) != 0 ? AXON_SIG : 0;
                            break;
                        case DEND:
                            for (int i = 0; i < 6; i++)
                                caio[i] = ((ca.Chromo >>> i) & 1) != 0 ? DEND_SIG : 0;
                            break;
                    }
                }
        Kicking();
        return CAChanged;
    }

    protected int SignalStep() {


        for (int iz = 0; iz < sizeZ; iz++)
            for (int iy = 0; iy < sizeY; iy++)
                for (int ix = 0; ix < sizeX; ix++) {

                    CodiCell ca = cell[ix][iy][iz];
                    int[] caio = ca.IOBuf;
                    switch (ca.Type) {
                        case BLANK:


                            break;
                        case NEURON: {
                            int InputSum = 1 +
                                    caio[0] +
                                    caio[1] +
                                    caio[2] +
                                    caio[3] +
                                    caio[4] +
                                    caio[5] -
                                    caio[ca.Gate] -
                                    caio[((ca.Gate % 2) * -2) + 1 + ca.Gate];

                            ca.Activ += InputSum;

                            Arrays.fill(caio, 0);

                            if (ca.Activ > 31) {  //firing threshold
                                caio[ca.Gate] = 1;
                                caio[((ca.Gate % 2) * -2) + 1 + ca.Gate] = 1;
                                ca.Activ = 0;
                            }
                            break;
                        }
                        case AXON:
                            for (int i = 0; i < 6; i++)
                                caio[i] = caio[ca.Gate];
                            ca.Activ = (caio[ca.Gate]) != 0 ? 1 : 0;
                            break;
                        case DEND: {
                            int InputSum =
                                    0;
                            for (int i : new int[]{0, 1, 2, 3, 4, 5}) {
                                int i1 = caio[i];
                                InputSum += i1;
                            }
                            if (InputSum > 2) InputSum = 2;
                            Arrays.fill(caio, 0);

                            caio[ca.Gate] = InputSum;
                            ca.Activ = InputSum != 0 ? 1 : 0;
                            break;
                        }
                    }
                }
        Kicking();
        return 0;
    }

//    @Override
//    protected void DrawCA(Graphics g) {
//        if (bFirstStart) {
//            g.setColor(Color.black);
//            g.fillRect(Offset, Offset,
//                    sizeX * CellWidthPx, sizeY * CellHeightPx);
//            bFirstStart = false;
//        }
//
//        int PosX = Offset - CellWidthPx;
//        int iz = 0;
//        for (int ix = 0; ix < sizeX; ix++) {
//            PosX += CellWidthPx;
//            int PosY = Offset - CellHeightPx;
//            for (int iy = 0; iy < sizeY; iy++) {
//                PosY += CellHeightPx;
//                CodiCell ca = cell[ix][iy][iz];
//                if (ca.Type != 0) {
//                    if (ca.Activ != 0) {
//                        ActualColor = ca.Type != NEURON ? 5 : 1;
//                    } else
//                        switch (ca.Type) {
//                            case NEURON:
//                                ActualColor = 1;
//                                break;
//                            case AXON:
//                                ActualColor = 2;
//                                break;
//                            case DEND:
//                                ActualColor = 4;
//                                break;
//                            default:
//                                ActualColor = 13;
////                                System.out.println("__" + ca.Type + "__");
//                                break;
//                        }
//                    if (ColorSpace[ix][iy][iz] != ActualColor) {
//                        ColorSpace[ix][iy][iz] = ActualColor;
//                        switch (ActualColor) {
//                            case NEURON:
//                                g.setColor(Color.white);
//                                break;
//                            case AXON:
//                                g.setColor(Color.red);
//                                break;
//                            case DEND:
//                                g.setColor(Color.green);
//                                break;
//                            case SIG_COL:
//                                g.setColor(Color.yellow);
//                                break;
//                            default:
//                                g.setColor(Color.blue);
//                        }
//                        g.fillRect(PosX, PosY, CellWidthPx, CellHeightPx);
//                    }
//                }
//            }
//        }
//    }

    public static class CodiSurface extends PaintSurface {

        private final CodiCA c;


        public CodiSurface(CodiCA c) {
            this.c = c;
        }

        @Override
        protected void paint(GL2 gl, ReSurface reSurface) {
            float tw = w()/c.sizeX;
            float th = h()/c.sizeY;


            for (int x = 0; x < c.sizeX; x++) {
                for (int y = 0; y < c.sizeY; y++) {
                    for (int z = 0; z < c.sizeZ; z++) {
                        CodiCell ca = c.cell[x][y][z];
                        int type = ca.Type;
                        if (type == 0)
                            continue;

                        float px = x * tw;
                        float py = y * th;
                        float pz = z * tw;


                        float r, g, b;
                        switch (type) {
                            case NEURON:
                                r = 0.75f;
                                g = 0.75f;
                                b = 0.75f;
                                break;
                            case AXON:
                                r = 0.75f;
                                g = 0f;
                                b = 0;
                                break;
                            case DEND:
                                r = 0f;
                                g = 0.75f;
                                b = 0;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

//                        if (ca.Gate!=0) {
//
//                        }

                        float gate = ca.Gate / 3f;
                        b += gate * 0.5f;

                        gl.glColor4f(r, g, b, 0.5f);
                        Draw.rect(px, py, tw, th, gl);

                        int activation = ca.Activ;
                        if (activation != 0) {
                            float a = 0.5f + 0.5f * Math.abs(activation) / 32f;

                            if (activation > 0)
                                gl.glColor4f(1f, 0.25f + 0.5f * a, 0f, a);
                            else
                                gl.glColor4f(0f, 0.25f + 0.5f * a, 1f, a);
                            Draw.rect(px + tw / 4, py + th / 4, tw / 2, th / 2, gl);
                        }


                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        CodiCA c = new CodiCA(20, 20, 1);

        new Loop() {

            @Override
            public boolean next() {
                c.next();
                return true;
            }
        }.setFPS(20);

        SpaceGraph.window(new CodiSurface(c), 1000, 1000);
    }
}

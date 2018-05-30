package codi;

import java.awt.*;
import java.util.Arrays;

public class CodiCA extends CA {
    private int CAChanged;
    private int InputSum;
    private boolean SignalingInited;

    
    static class CodiCell { 
        int Type;
        int Chromo;
        int Gate;
        int Activ;
        int IOBuf[] = new int[6];
    }

    protected CodiCell CASpace[][][];
    
    private static final int BLANK = 0; 
    private static final int NEURONSEED = 1;
    private static final int NEURON = 1; 
    private static final int AXON = 2; 
    private static final int DEND = 4; 
    private static final int AXON_SIG = 2;
    private static final int DEND_SIG = 4;
    
    private static final int SIG_COL = 5;    
    private static final int ERROR_COL = 13; 
    private final int[][][] ColorSpace; 
    private int ActualColor;

    public CodiCA() {
        super();
        MaxSteps = 500;
        SpaceSizeX = 100;
        SpaceSizeY = 100;
        SpaceSizeZ = 1;
        CellSizeX = 10;
        CellSizeY = 10;
        SpaceSize = SpaceSizeX * SpaceSizeY * SpaceSizeZ;
        CAFrameSizeX = SpaceSizeX * CellSizeX;
        CAFrameSizeY = SpaceSizeY * CellSizeY;
        ColorSpace = new int[SpaceSizeX][SpaceSizeY][SpaceSizeZ];
        CASpace = new CodiCell[SpaceSizeX][SpaceSizeY][SpaceSizeZ];
        for (int ix = 0; ix < SpaceSizeX; ix++)
            for (int iy = 0; iy < SpaceSizeY; iy++)
                for (int iz = 0; iz < SpaceSizeZ; iz++)
                    CASpace[ix][iy][iz] = new CodiCell();
        CLRGraphicsAfterStep = false;
    }

    @Override
    protected void InitCA() {
        CountCAStps = 0;
        CAChanged = 1;
        SignalingInited = false;
        bFirstStart = true;
        for (int ix = 0; ix < SpaceSizeX; ix++)
            for (int iy = 0; iy < SpaceSizeY; iy++)
                for (int iz = 0; iz < SpaceSizeZ; iz++) {
                    ColorSpace[ix][iy][iz] = 0;
                    CodiCell cell = CASpace[ix][iy][iz];
                    cell.Type = 0;
                    cell.Activ = 0;
                    for (int i = 0; i < 6; i++) cell.IOBuf[i] = 0;
                    cell.Chromo = (random.nextInt() % 256);
                    
                    if (((ix + 1) % 2) * (iy % 2) == 1)
                        cell.Chromo = (cell.Chromo & ~3) | 12;
                    if ((ix % 2) * ((iy + 1) % 2) == 1)
                        cell.Chromo = (cell.Chromo & ~12) | 3;
                    
                    if ((ix % 2) + (iy % 2) != 0) cell.Chromo &= ~192;
                    
                    if ((cell.Chromo >>> 6) == NEURONSEED)
                        if ((random.nextInt() % SpaceSizeX) < ix / 2)
                            cell.Chromo &= ~192;
                    
                    if ((cell.Chromo >>> 6) == NEURONSEED)
                        cell.Chromo =
                                (cell.Chromo & 192) |
                                        ((cell.Chromo & 63) % 4);
                }
    }

    protected void Kicking() {
        
        
        
        
        
        
        
        
        

        
        CodiCell[][][] ca = CASpace;
        for (int iz = 0; iz < SpaceSizeZ; iz++)
            for (int iy = 0; iy < SpaceSizeY; iy++)
                for (int ix = 0; ix < SpaceSizeX; ix++) {
                    
                    int[] caio = ca[ix][iy][iz].IOBuf;
                    caio[4] = iz != SpaceSizeZ - 1 ? ca[ix][iy][iz + 1].IOBuf[4] : 0;
                    
                    caio[2] = iy != SpaceSizeY - 1 ? ca[ix][iy + 1][iz].IOBuf[2] : 0;
                    
                    caio[0] = ix != SpaceSizeX - 1 ? ca[ix + 1][iy][iz].IOBuf[0] : 0;
                }
        
        
        for (int iz = SpaceSizeZ - 1; iz >= 0; iz--)
            for (int iy = SpaceSizeY - 1; iy >= 0; iy--)
                for (int ix = SpaceSizeX - 1; ix >= 0; ix--) {
                    
                    int[] caio = ca[ix][iy][iz].IOBuf;
                    caio[5] = iz != 0 ? ca[ix][iy][iz - 1].IOBuf[5] : 0;
                    
                    caio[3] = iy != 0 ? ca[ix][iy - 1][iz].IOBuf[3] : 0;
                    
                    caio[1] = ix != 0 ? ca[ix - 1][iy][iz].IOBuf[1] : 0;
                }
    }

    private void InitSignaling() {
        SignalingInited = true;
        CodiCell[][][] ca = CASpace;
        for (int iz = 0; iz < SpaceSizeZ; iz++)
            for (int iy = 0; iy < SpaceSizeY; iy++)
                for (int ix = 0; ix < SpaceSizeX; ix++) {
                    CodiCell c = ca[ix][iy][iz];
                    c.Activ = 0;
                    Arrays.fill(c.IOBuf, 0, 6, 0);
                    if (c.Type == NEURON)
                        c.Activ = (random.nextInt() % 32);
                }
    }

    @Override
    protected void StepCA() {
        CountCAStps++;
        if (CountCAStps == MaxSteps)
            InitCA();
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
        for (int iz = 0; iz < SpaceSizeZ; iz++)
            for (int iy = 0; iy < SpaceSizeY; iy++)
                for (int ix = 0; ix < SpaceSizeX; ix++) {
                    
                    
                    
                    
                    
                    
                    
                    CodiCell ca = CASpace[ix][iy][iz];
                    int[] caio = ca.IOBuf;
                    switch (ca.Type) {
                        case BLANK:                    
                            
                            if ((ca.Chromo >>> 6) == NEURONSEED) {
                                ca.Type = NEURON;
                                CAChanged = 1;
                                
                                ca.Gate =
                                        (ca.Chromo & 63) % 6;
                                for (int i = 0; i < 6; i++)
                                    caio[i] = DEND_SIG;
                                caio[ca.Gate]
                                        = AXON_SIG;
                                caio
                                        [(ca.Gate % 2 * -2) + 1 + ca.Gate]
                                        = AXON_SIG;
                                break;
                            }
                            
                            InputSum =
                                    caio[0] +
                                            caio[1] +
                                            caio[2] +
                                            caio[3] +
                                            caio[4] +
                                            caio[5];
                            if (InputSum == 0) break;
                            
                            InputSum =
                                    (caio[0] & AXON_SIG) +
                                            (caio[1] & AXON_SIG) +
                                            (caio[2] & AXON_SIG) +
                                            (caio[3] & AXON_SIG) +
                                            (caio[4] & AXON_SIG) +
                                            (caio[5] & AXON_SIG);
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
                            
                            InputSum =
                                    (caio[0] & DEND_SIG) +
                                            (caio[1] & DEND_SIG) +
                                            (caio[2] & DEND_SIG) +
                                            (caio[3] & DEND_SIG) +
                                            (caio[4] & DEND_SIG) +
                                            (caio[5] & DEND_SIG);
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
                            caio[ca.Gate]
                                    = AXON_SIG;
                            caio[((ca.
                                    Gate % 2) * -2) + 1 + ca.Gate]
                                    = AXON_SIG;
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
        
        
        for (int iz = 0; iz < SpaceSizeZ; iz++)
            for (int iy = 0; iy < SpaceSizeY; iy++)
                for (int ix = 0; ix < SpaceSizeX; ix++) {
                    
                    CodiCell ca = CASpace[ix][iy][iz];
                    int[] caio = ca.IOBuf;
                    switch (ca.Type) {
                        case BLANK:                    
                            
                            
                            break;
                        case NEURON:                
                            InputSum = 1 +            
                                    caio[0] +
                                    caio[1] +
                                    caio[2] +
                                    caio[3] +
                                    caio[4] +
                                    caio[5] -
                                    caio[ca.Gate] -
                                    caio
                                            [((ca.
                                            Gate % 2) * -2) + 1 + ca.Gate];
                            for (int i = 0; i < 6; i++)
                                caio[i] = 0;
                            ca.Activ += InputSum;
                            if (ca.Activ > 31) { 
                                caio[ca.Gate] = 1;
                                caio
                                        [((ca.
                                        Gate % 2) * -2) + 1 + ca.Gate]
                                        = 1;
                                ca.Activ = 0;
                            }
                            break;
                        case AXON:                     
                            for (int i = 0; i < 6; i++)
                                caio[i] =
                                        (caio[ca.Gate]);
                            ca.Activ = (caio[ca.Gate]) != 0 ? 1 : 0;
                            break;
                        case DEND:                     
                            InputSum =
                                    caio[0] +
                                            caio[1] +
                                            caio[2] +
                                            caio[3] +
                                            caio[4] +
                                            caio[5];
                            if (InputSum > 2) InputSum = 2;
                            for (int i = 0; i < 6; i++)
                                caio[i] = 0;
                            caio
                                    [ca.Gate] = InputSum;
                            ca.Activ = InputSum != 0 ? 1 : 0;
                            break;
                    }
                }
        Kicking();
        return 0;
    }

    @Override
    protected void DrawCA(Graphics g) {
        if (bFirstStart) {
            DrawCAFrame(g);
            g.setColor(Color.black);
            g.fillRect(Offset, Offset,
                    SpaceSizeX * CellSizeX, SpaceSizeY * CellSizeY);
            bFirstStart = false;
        }
        
        int PosX = Offset - CellSizeX;
        int iz = 0;
        for (int ix = 0; ix < SpaceSizeX; ix++) {
            PosX += CellSizeX;
            int PosY = Offset - CellSizeY;
            for (int iy = 0; iy < SpaceSizeY; iy++) {
                PosY += CellSizeY;
                CodiCell ca = CASpace[ix][iy][iz];
                if (ca.Type != 0) {
                    if (ca.Activ != 0) {
                        ActualColor = ca.Type != NEURON ? 5 : 1;
                    } else
                        switch (ca.Type) {
                            case NEURON:
                                ActualColor = 1;
                                break;
                            case AXON:
                                ActualColor = 2;
                                break;
                            case DEND:
                                ActualColor = 4;
                                break;
                            default:
                                ActualColor = 13;
                                System.out.println("__" + ca.Type + "__");
                                break;
                        }
                    if (ColorSpace[ix][iy][iz] != ActualColor) {
                        ColorSpace[ix][iy][iz] = ActualColor;
                        switch (ActualColor) {
                            case NEURON:
                                g.setColor(Color.white);
                                break;
                            case AXON:
                                g.setColor(Color.red);
                                break;
                            case DEND:
                                g.setColor(Color.green);
                                break;
                            case SIG_COL:
                                g.setColor(Color.yellow);
                                break;
                            default:
                                g.setColor(Color.blue);
                        }
                        g.fillRect(PosX, PosY, CellSizeX, CellSizeY);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        CodiCA c = new CodiCA();
        c.setSize(1000, 1000);
        c.setVisible(true);
        new Thread(c).start();
    }
}

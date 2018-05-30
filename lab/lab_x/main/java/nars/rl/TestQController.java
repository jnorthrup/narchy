package nars.nal;


import nars.NAR;
import nars.Param;
import nars.nar.Default2;

import java.io.PrintStream;
import java.text.DecimalFormat;

/**
 * Dynamic logic controller experiment, using QLearning
 * 
 * 
 * Experiment:
<sseehh_> normally, concept priority drops
<sseehh_> to like 0.03
<sseehh_> average concept priority
<sseehh_> this applies every N cycles
<sseehh_> so its looking at the average over the waiting period
<sseehh_> priority may spike for a few concepts, but this affects little
<sseehh_> if it can raise the avg concept priority, then it has significantly affected logic behavior
 */
public class TestQController {
    
    final static String cpm = "concept.priority.mean";
    final static String td = "task.derived";
    final static String cpv = "concept.priority.variance";
    final static String cph0 = "concept.priority.hist.0";
    final static String cph1 = "concept.priority.hist.1";
    final static String cph2 = "concept.priority.hist.2";
    final static String cph3 = "concept.priority.hist.3";
    final static String nt = "task.novel.total";
    
    
    public static class TestController extends QController {

                
        private double conceptNewMean;
        private double taskDerivedMean;
        
        final int minCycleToForget = 2;
        final int maxCycleToForget = 40;
        
        public TestController(NAR n, int period) {
            super(n, period);
                        
            
            Param p = nar.memory;
            
            add(new NControlSensor(p.conceptForgetDurations, 3));
            
            
            
            
            add(new EventValueControlSensor(nar, nar.memory.logic.CONCEPT_NEW, 0, 1, 7));
            add(new EventValueControlSensor(nar, nar.memory.logic.JUDGMENT_PROCESS, 0, 1, 7));


















            init(3);
            
            
        }

        @Override
        protected int[] getFeedForwardLayers(int inputSize) {
            
            
            
            
            return new int[] { 6 };
        }

        @Override
        protected void act(int action) {
            Param p = nar.memory;
            
            
            switch (action) {
                case 0: 
                    p.conceptForgetDurations.setValue(3);
                    break;
                case 1: 
                    p.conceptForgetDurations.setValue(5);
                    break;
                case 2:
                    p.conceptForgetDurations.setValue(7);
                    break;
            }
            
            













        }        
        
        @Override
        public double reward() {
            
            
            
            
            return nar.memory.emotion.happy() + nar.memory.logic.JUDGMENT_PROCESS.getValue(null, 0);
            
        }


    }





    
    public static NAR newNAR() {
        
        return new Default2(512, 1, 1, 3)
        
        ;
    }
    
    public static void main(String[] arg) {
          
        int controlPeriod = 2;
        
        NAR n = newNAR(); 
        TestController qn = new TestController(n, controlPeriod);
        qn.setActive(false);

        
        NAR m = newNAR();
        TestController qm = new TestController(m, controlPeriod);
        qm.setActive(false);

        
        NAR r = newNAR();
        TestController qr = new TestController(r, controlPeriod) {

            @Override
            protected void act(int ignored) {
                int action = (int)(Math.random() * getNumActions());
                super.act(action); 
            }
            
        };
        qr.setActive(false);
        
        double mm = 0, nn = 0, rr = 0;
        int displayCycles = 100;
        double[] nAction = new double[qn.getNumActions()];
        long startupPeriod = 0;
        int resetPeriod = 50000;
        double avgCycleToForget = 0;
        int time = 0;
        while (true ) {
            

            if (time % resetPeriod == 0) {
                System.out.println("RESET");
                n.reset();        

                m.reset();
                r.reset();
                
                
                
                
            }
            
            if (time > startupPeriod) {
                qr.setActive(true);
                qn.setActive(true);
                double[] oqn = qn.getOutput();
                if (oqn!=null) {
                    for (int i = 0; i < nAction.length; i++)
                        nAction[i] += oqn[i] / displayCycles;
                }                
             
                
            }
            
            n.frame(1);
            m.frame(1);
            r.frame(1);
            
            avgCycleToForget += (n.memory).conceptForgetDurations.getValue() / displayCycles;
            mm += qm.reward();
            nn += qn.reward();
            rr += qr.reward();
            
                        
            if (time % displayCycles == 0) {
                System.out.print(
                        
                                time + ", " +
                                df.format(mm) + " , " + df.format(nn) + " , " + df.format(rr) + " , ");
                          
                
                System.out.print(avgCycleToForget + ", ");
                printCSVLine(System.out, nAction);
                
                mm = nn = rr = avgCycleToForget = 0;
                Arrays.fill(nAction, 0);
            }
            time++;
        }
                
    }

    
    
    
    
    
    protected final static DecimalFormat df = new DecimalFormat("#.###");
    
    public static void printCSVLine(PrintStream out, List<String> o) {
        StringJoiner line = new StringJoiner(",", "", "");
        int n = 0;
        for (String x : o) {
            line.add(x + "_" + (n++));
        }
        out.println(line.toString());
    }

    public static void printCSVLine(PrintStream out, double[] a) {
        StringJoiner line = new StringJoiner(",", "", "");        
        for (double x : a)
            line.add(df.format(x));
        out.println(line.toString());
    }
        
    
    protected static Map<String, String> exCache = new HashMap(); 
    




    
}

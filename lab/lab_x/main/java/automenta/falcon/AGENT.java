package automenta.falcon;

import jurls.reinforcementlearning.domains.RLEnvironment;

public abstract class AGENT {

    public final static int RFALCON = 0;
    public final static int TDFALCON = 1;
    public final static int BPN = 2;

    public final static int QLEARNING = 0;
    public final static int SARSA = 1;

    

    

    

    public double QGamma = (double) 0.1;


    public double initialQ = (double) 0.5;

    

    public double QEpsilonDecay = (double) 0.005;
    public double QEpsilon = (double) 0.50000;
    public double minQEpsilon = (double) 0.05;

    public boolean forgetting = true;

    public static boolean INTERFLAG = false;
    
    
    public boolean Trace = false;

    abstract public void saveAgent(String outfile);

    abstract public void checkAgent(String outfile);

    abstract public void init(int AVTYPE, boolean ImmediateReward);

    abstract public void setAction(int action);

    abstract public void initAction();

    abstract public void resetAction();



    abstract public int doSearchAction(int mode, int type);

    abstract public int act(boolean train, RLEnvironment env);

    abstract public int actDirect(RLEnvironment env, boolean train);

    abstract public int doDirectAccessAction(boolean train, RLEnvironment env);

    abstract public void doLearnACN();

    abstract public void setprev_J();

    abstract public double computeJ(RLEnvironment env);

    abstract public void setNextJ(double J);

    abstract public void turn(int d);

    abstract public void move(int d, boolean actual);

    abstract public double doSearchQValue(int mode, int type);

    abstract public double getMaxQValue(int method, boolean train, RLEnvironment env);

    abstract public void setReward(double reward);

    abstract public double getPrevReward();

    abstract public void setPrevReward(double reward);

    abstract public void init_path(int maxStep);

    abstract public void setTrace(boolean trace);

    abstract public int getNumCode();

    abstract public int getCapacity();

    abstract public void decay();

    abstract public void prune();

    abstract public void purge();

    abstract public void reinforce();

    abstract public void penalize();

    public void age() {
        if (QEpsilon > minQEpsilon)
            QEpsilon -= QEpsilonDecay;
    }




































































































































}

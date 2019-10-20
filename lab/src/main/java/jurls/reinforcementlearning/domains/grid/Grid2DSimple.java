package jurls.reinforcementlearning.domains.grid;


public class Grid2DSimple implements World {
    private final int size;

    private final double REWARD_MAGNITUDE;
    private final double JUMP_FRACTION;
    private final double ENERGY_COST_FACTOR;
    private final double MATCH_REWARD_FACTOR;


    private final int totalTime;
    private int time;

    private final int w;
    private final int h;
    private double focusPositionW;
    private double focusPositionH;

    public Grid2DSimple(int x, int y, int totalTime, double noise, double focusVelocity) {
        this.time = 1;
        this.w = x;
        this.h = y;
        this.size = x * y;
        double VISUALIZE_PERIOD = Math.pow(10.0, 4.0);
        this.ENERGY_COST_FACTOR = 1.0;
        this.MATCH_REWARD_FACTOR = (double) size *1.0;
        this.REWARD_MAGNITUDE = 1.0;
        this.JUMP_FRACTION = 0.1;

        this.totalTime = totalTime;
    }

    
    @Override    public String getName()    {     return "Grid1D";    }
    @Override    public int getNumSensors() {     return size;    }
    @Override    public int getNumActions() {     return size;    }
    @Override    public boolean isActive()  {     return time < totalTime;   }

    double[] action2 = null;

    double get(double[] d, int x, int y) {
        return d[y * h + x];
    }
    void set(double[] d, int x, int y, double v) {
        d[y * h + x] = v;
    }
    
    @Override
    public double step(double[] action, double[] sensor) {

        time++;


        if (Math.random() < JUMP_FRACTION) {
            focusPositionW = (double) w * Math.random();
            focusPositionH = (double) h * Math.random();
        }
        
        
    
                  
        double match = (double) 0;
        double energyCost = (double) 0;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double a = get(action, x, y);
                if (a > (double) 0) {
                    double dx = Math.abs((double) x - focusPositionW);
                    double dy = Math.abs((double) y - focusPositionH);
                    match += a * 1.0/(1.0+Math.sqrt(dx*dx + dy*dy));
                    energyCost += a;                    
                }
            }
        }

        double reward = REWARD_MAGNITUDE * ((MATCH_REWARD_FACTOR * match) - (energyCost * ENERGY_COST_FACTOR));
        
        /*if (reward!=0)
            System.out.println(match + " " + energyCost + " -> " + reward);*/
        
        
        
        final double exp = 3.0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double dx = Math.abs((double) x - focusPositionW);
                double dy = Math.abs((double) y - focusPositionH);
                double distScale = 1.0/(1.0+Math.sqrt(dx*dx + dy*dy));
                double v = Math.pow(distScale, exp);
                if (v < 0.1) v = (double) 0;
                set(sensor, x, y, v);
            }
        }
        
                
        return reward;        
    }
        

    
    /*
    def visualize(self, agent):
        """ Show what's going on in the world """
        if (this.display_state):
            state_image = ['.'] * (this.num_sensors + this.num_actions + 2)
            state_image[this.simple_state] = 'O'
            state_image[this.num_sensors:this.num_sensors + 2] = '||'
            action_index = np.where(this.action > 0.1)[0]
            if action_index.size > 0:
                for i in range(action_index.size):
                    state_image[this.num_sensors + 2 + action_index[i]] = 'x'
            print(''.join(state_image))
            
        if (this.timestep % this.VISUALIZE_PERIOD) == 0:
            print("world age is %s timesteps " % this.timestep)    
    */

    /*
    
    def set_agent_parameters(self, agent):
    """ Turn a few of the knobs to adjust BECCA for this world """
    # Prevent the agent from forming any groups
    #agent.reward_min = -100.
    #agent.reward_max = 100.
    pass
     */
    
    public static void main(String[] args) throws Exception {
        Class<? extends Agent> a = RandomAgent.class;
        
        
        new Simulation(a, new Grid2DSimple(4,4, 11990000, 0.01, 0.005));
        
    }
}

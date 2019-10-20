package jurls.reinforcementlearning.domains.grid;



public class Grid2DBinaryPositioning implements World {
    private final int size;

    private final double REWARD_MAGNITUDE;
    private final double JUMP_FRACTION;
    private final double MATCH_REWARD_FACTOR;


    private final int totalTime;
    private int time;

    private final int w;
    private final int h;
    private double focusPositionW;
    private double focusPositionH;
    private final int b;

    public Grid2DBinaryPositioning(int x, int y, int b, int totalTime, double noise, double focusVelocity) {
        
        this.b = b;
        
        
        this.time = 1;
        this.w = x;
        this.h = y;
        this.size = x * y;
        var VISUALIZE_PERIOD = Math.pow(10, 4);
        this.MATCH_REWARD_FACTOR = size*1.0;
        this.REWARD_MAGNITUDE = 5.0;
        this.JUMP_FRACTION = 0.01;

        this.totalTime = totalTime;
    }

    
    @Override    public String getName()    {     return "Grid1D";    }
    @Override    public int getNumSensors() {     return size;    }
    @Override    public int getNumActions() {     return b*2;    }
    @Override    public boolean isActive()  {     return time < totalTime;   }

    double[] action2 = null;

    double get(double[] d, int x, int y) {
        return d[y * h + x];
    }
    void set(double[] d, int x, int y, double v) {
        d[y * h + x] = v;
    }
    static int i(double v) {
        if (v > 0.5) return 1; 
        return 0;
    }
    
    @Override
    public double step(double[] action, double[] sensor) {

        time++;


        if (Math.random() < JUMP_FRACTION) {
            focusPositionW = w * Math.random();
            focusPositionH = h * Math.random();
        }
        
        
    
                  
        double match = 0;


        var ax = i(action[0]) + 2 * i(action[1]) + 4 * i(action[2]) + 8 * i(action[3]);
        var ay = i(action[4]) + 2 * i(action[5]) + 4 * i(action[6]) + 8 * i(action[7]);

        var adx = Math.abs(ax - focusPositionW);
        var ady = Math.abs(ay - focusPositionH);
        match += 1.0/(1.0+Math.sqrt(adx*adx + ady*ady));

        var reward = REWARD_MAGNITUDE * ((MATCH_REWARD_FACTOR * match));
        
        if (reward!=0)
            System.out.println(match + " " + " -> " + reward);
        
        
        
        final var exp = 2.0;
        for (var x = 0; x < w; x++) {
            for (var y = 0; y < h; y++) {
                var dx = Math.abs(x - focusPositionW);
                var dy = Math.abs(y - focusPositionH);
                var distScale = 1.0/(1.0+Math.sqrt(dx*dx + dy*dy));
                var v = Math.pow(distScale, exp);
                if (v < 0.1) v = 0;
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
        
        
        new Simulation(a, new Grid2DBinaryPositioning(16,16, 4, 11990000, 0.01, 0.005));
        
    }
}

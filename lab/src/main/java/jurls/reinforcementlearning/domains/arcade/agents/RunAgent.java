package jurls.reinforcementlearning.domains.arcade.agents;

import java.io.File;
import java.io.IOException;

/**
 * Created by me on 5/20/15.
 */
public class RunAgent {


    public static void main(String[] args) throws IOException {


        var rom = "space_invaders";
        var romPath = "/home/me/roms";
        var alePath = "/home/me/neuro/ale_0.4.4/ale_0_4";

        var aleCommand = alePath + "/ale -game_controller fifo_named " + romPath + "/" + rom + ".bin";
        System.out.println(aleCommand);

        var proc = Runtime.getRuntime().exec(aleCommand, new String[] {} , new File(alePath));


        var useGUI = true;
        var namedPipesName = alePath + "/ale_fifo_";


        var agent = new RLAgent(useGUI, namedPipesName);

        agent.run();

        
        System.in.read();
    }

}

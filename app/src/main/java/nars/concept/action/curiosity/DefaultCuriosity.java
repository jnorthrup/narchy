package nars.concept.action.curiosity;

import nars.NAL;
import nars.agent.Game;

public enum DefaultCuriosity {
    ;

    public static Curiosity defaultCuriosity(Game a) {
        Curiosity c = new Curiosity(a, NAL.DEFAULT_CURIOSITY_RATE);
        c.add(new EchoDerivedCuriosity().withPri(0.02f));
        c.add(new EchoNegatedDerivedCuriosity().withPri(0.01f));
        c.add(new RandomPhasorCuriosity().withPri(0.07f));
        //c.addAt(new UniformRandomCuriosity().weight(0.1f));
        return c;
    }
}

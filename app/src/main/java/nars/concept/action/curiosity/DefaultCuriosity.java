package nars.concept.action.curiosity;

import nars.agent.NAgent;

public enum DefaultCuriosity {
    ;

    public static Curiosity defaultCuriosity(NAgent a) {
        Curiosity c = new Curiosity(a);
        c.add(new NullCuriosity().weight(0.9f));
        c.add(new EchoDerivedCuriosity().weight(0.07f));
        c.add(new EchoNegatedDerivedCuriosity().weight(0.01f));
        //c.add(new UniformRandomCuriosity().weight(0.1f));
        c.add(new RandomPhasorCuriosity().weight(0.07f));
        return c;
    }
}

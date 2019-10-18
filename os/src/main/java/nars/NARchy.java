package nars;

import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.exe.impl.WorkerExec;
import nars.memory.CaffeineMemory;
import nars.op.language.NARHear;
import nars.op.language.NARSpeak;
import nars.time.clock.RealTime;
import spacegraph.audio.speech.NativeSpeechDispatcher;

public class NARchy extends NARS {

    //static final Logger logger = LoggerFactory.getLogger(NARchy.class);


    public static NAR core() {
        return core(Runtime.getRuntime().availableProcessors());
    }

    public static NAR core(int threads) {


        NAR nar = new DefaultNAR(0, true)

                .index(CaffeineMemory.soft())
                //.index(new HijackConceptIndex(32*1024, 4))

                .exe(new WorkerExec(threads))

                .time(new RealTime.MS(false ).durFPS(10f))
                
                .get();


        nar.dtDither.set(20);

        nar.beliefPriDefault.pri(0.5f);
        nar.goalPriDefault.pri(0.5f);
        nar.questionPriDefault.pri(0.5f);
        nar.questPriDefault.pri(0.5f);

        new Deriver(Derivers.nal(nar, 1, 8, "motivation.nal"));

        //new Arithmeticize.ArithmeticIntroduction(nar, );

        return nar;
    }

    public static NAR ui() {
        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
        NAR nar = core();
        nar.exe.throttle(0.1f);
        
        nar.runLater(()->{

            //User u = User.the();

            NARHear.readURL(nar);

            {
                NARSpeak s = new NARSpeak(nar);
                s.spoken.on(  NativeSpeechDispatcher ::speak);
                
            }

//            new NARVideo(nar);
//            new NARAudio(nar, new AudioSource().start(),  10f);




            InterNAR i = new InterNAR(nar);
            i.fps(2);


        });

        return nar;
    }


}

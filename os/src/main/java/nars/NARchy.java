package nars;

import nars.derive.Derivers;
import nars.derive.BatchDeriver;
import nars.exe.impl.WorkerExec;
import nars.memory.CaffeineMemory;
import nars.op.language.NARHear;
import nars.op.language.NARSpeak;
import nars.op.stm.ConjClustering;
import nars.time.clock.RealTime;
import nars.video.NARVideo;
import spacegraph.audio.speech.NativeSpeechDispatcher;

import static nars.Op.BELIEF;

public class NARchy extends NARS {

    //static final Logger logger = LoggerFactory.getLogger(NARchy.class);


    public static NAR core() {
        return core(1);
    }

    public static NAR core(int threads) {


        NAR nar = new DefaultNAR(0, true)

                .index(new CaffeineMemory(32*1024))
                //.index(new HijackConceptIndex(32*1024, 4))

                .exe(new WorkerExec(threads))

                .time(new RealTime.MS(false ).durFPS(10f))
                
                .get();


        nar.dtDither.set(20);

        nar.beliefPriDefault.amp(0.5f);
        nar.goalPriDefault.amp(0.75f);
        nar.questionPriDefault.amp(0.35f);
        nar.questPriDefault.amp(0.35f);

        nar.start(new ConjClustering(nar, BELIEF,
                Task::isInput
                , 16, 64));
        nar.start(new ConjClustering(nar, BELIEF,
                t -> !t.isInput()
                , 16, 64));

        new BatchDeriver(Derivers.nal(nar, 1, 8, "motivation.nal"));

        //new Arithmeticize.ArithmeticIntroduction(nar, );

        return nar;
    }

    public static NAR ui() {
        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
        NAR nar = core();
        
        nar.runLater(()->{

            //User u = User.the();





            NARHear.readURL(nar);

            {
                NARSpeak s = new NARSpeak(nar);
                s.spoken.on(new NativeSpeechDispatcher()::speak);
                
            }

            new NARVideo(nar);
//            new NARAudio(nar, new AudioSource().start(),  10f);




            InterNAR i = new InterNAR(nar);
            i.fps(2);


        });

        return nar;
    }


}

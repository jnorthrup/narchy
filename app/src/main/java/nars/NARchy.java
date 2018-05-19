package nars;

import jcog.User;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.exe.Focus;
import nars.exe.WorkerMultiExec;
import nars.index.concept.CaffeineIndex;
import nars.op.ArithmeticIntroduction;
import nars.op.language.NARHear;
import nars.op.language.NARSpeak;
import nars.op.stm.ConjClustering;
import nars.time.clock.RealTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.audio.speech.NativeSpeechDispatcher;

import static nars.Op.BELIEF;

public class NARchy extends NARS {

    static final Logger logger = LoggerFactory.getLogger(NARchy.class);

    public static NAR core() {

        //u.forEach(System.out::println);
//        u.put("boot", new Date().toString());
//        Util.pause(100);
//        u.get("boot", (b)->{
//            System.out.println(b);
//        });


        NAR nar = new DefaultNAR(8, true)

                .index(new CaffeineIndex(1000 * 128 * 1024))

                .exe(new WorkerMultiExec(
                        new Focus.AERevaluator(new XoRoShiRo128PlusRandom(1)),
                        1, 128, 1024))

//                .exe(new PoolMultiExec /*WorkerMultiExec*/(
//                        Util.concurrencyDefault(2), 512,
//                    new Focus.AERevaluator(new XoRoShiRo128PlusRandom(1))
//                        //        , 64, 512))
//                ))
//                .exe(new AbstractExec(64) {
//                    @Override
//                    public boolean concurrent() {
//                        return true;
//                    }
//                })
                .time(new RealTime.CS(true /* HACK */).durFPS(10f))
                //.memory("/tmp/nal")
                .get();


        nar.beliefPriDefault.set(0.5f);
        nar.goalPriDefault.set(0.75f);
        nar.questionPriDefault.set(0.35f);
        nar.questPriDefault.set(0.35f);

        ConjClustering conjClusterB = new ConjClustering(nar, BELIEF,
                (t -> true)
                //t -> t.isInput()
                , 16, 64);

        //ConjClustering conjClusterG = new ConjClustering(nar, GOAL, true, false, 16, 64);

        new ArithmeticIntroduction(4, nar);

        return nar;
    }

    public static NAR ui() {
        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
        NAR nar = ui();
        //auxiliary modules, load in background thread
        nar.runLater(()->{

            User u = User.the();

//            new NARAudio(nar);
//
//            new NARVideo(nar);

            NARHear.readURL(nar);

            {
                NARSpeak s = new NARSpeak(nar);
                s.spoken.on(new NativeSpeechDispatcher()::speak);
                //new NativeSpeechDispatcher(s);
            }

//            //new NoteFS("/tmp/nal", nar);


//            InterNAR i = new InterNAR(nar, 8, 0);
//            i.recv.preAmp(0.1f);
//            i.runFPS(2);


        });

        return nar;
    }


}

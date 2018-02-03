package nars;

import jcog.User;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.audio.NARAudio;
import nars.exe.Focus;
import nars.exe.PoolMultiExec;
import nars.op.language.NARHear;
import nars.op.language.NARSpeak;
import nars.op.stm.ConjClustering;
import nars.time.RealTime;
import nars.video.NARVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.BELIEF;

public class NARchy extends NARS {

    static final Logger logger = LoggerFactory.getLogger(NARchy.class);

    public static NAR ui() {

        User u = User.the();
        //u.forEach(System.out::println);
//        u.put("boot", new Date().toString());
//        Util.pause(100);
//        u.get("boot", (b)->{
//            System.out.println(b);
//        });


        NAR nar = new DefaultNAR(8, true)
                //.exe(new WorkerMultiExec(512, 2, 64))
                .exe(new PoolMultiExec(new Focus.AERevaluator(new XoRoShiRo128PlusRandom(1))
                        , 64, 512))
//                .exe(new AbstractExec(64) {
//                    @Override
//                    public boolean concurrent() {
//                        return true;
//                    }
//                })
                .time(new RealTime.CS().durFPS(10f))
                //.memory("/tmp/nal")
                .get();


        ConjClustering conjClusterB = new ConjClustering(nar, BELIEF, (Task::isInput), 16, 64);
        //ConjClustering conjClusterG = new ConjClustering(nar, GOAL, true, false, 16, 64);



        //auxiliary modules, load in background thread
        nar.runLater(()->{

            new NARAudio(nar);

            new NARVideo(nar);

            NARHear.readURL(nar);

            {
                NARSpeak s = new NARSpeak(nar);
                new NARSpeak.CmdlineSpeechDispatcher(s);
            }

//            //new NoteFS("/tmp/nal", nar);


//            InterNAR i = new InterNAR(nar, 8, 0);
//            i.recv.preAmp(0.1f);
//            i.runFPS(2);


        });

        return nar;
    }

    public static NAR core() {
        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
        return ui();
    }


}

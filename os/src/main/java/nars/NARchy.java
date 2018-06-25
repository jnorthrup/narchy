package nars;

import jcog.User;
import jcog.Util;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.exe.BufferedExec;
import nars.index.concept.HijackConceptIndex;
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

        







        NAR nar = new DefaultNAR(8, true)

                .index(new HijackConceptIndex(06*1024, 4))

                .exe(new BufferedExec.ForkJoinExec(Math.max(2,Util.concurrency())))

                .time(new RealTime.MS(false ).durFPS(10f))
                
                .get();


        nar.dtDither.set(20);

        nar.beliefPriDefault.set(0.5f);
        nar.goalPriDefault.set(0.75f);
        nar.questionPriDefault.set(0.35f);
        nar.questPriDefault.set(0.35f);

        ConjClustering conjClusterB = new ConjClustering(nar, BELIEF,
                t -> t.isInput()
                , 16, 64);
        ConjClustering conjClusterBnonInput = new ConjClustering(nar, BELIEF,
                t -> !t.isInput()
                , 16, 64);

        new MatrixDeriver(Derivers.nal(nar, 1, 8, "curiosity.nal", "motivation.nal"));

        new ArithmeticIntroduction(32, nar);

        return nar;
    }

    public static NAR ui() {
        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
        NAR nar = core();
        
        //nar.runLater(()->{

            User u = User.the();





            NARHear.readURL(nar);

            {
                NARSpeak s = new NARSpeak(nar);
                s.spoken.on(new NativeSpeechDispatcher()::speak);
                
            }





            InterNAR i = new InterNAR(nar, 8, 0);
            i.runFPS(2);


        //});

        return nar;
    }


}

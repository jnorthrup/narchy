package nars;

import com.google.common.base.Joiner;
import nars.exe.UniExec;
import nars.op.AtomicExec;
import nars.op.Operator;
import nars.op.stm.ConjClustering;
import nars.term.container.TermContainer;
import nars.time.RealTime;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;
import spacegraph.audio.MaryTTSpeech;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class NARchy extends NARS {

    public static NAR ui() {
        NAR nar = new DefaultNAR(8, true)
                .exe(new UniExec(64) {
                    @Override
                    public boolean concurrent() {
                        return true;
                    }
                })
                .time(new RealTime.CS().durFPS(10f))
                //.memory("/tmp/nal")
                .get();


        ConjClustering conjClusterB = new ConjClustering(nar, 3, BELIEF, true, 16, 64);
        ConjClustering conjClusterG = new ConjClustering(nar, 2, GOAL, true, 16, 64);

        //Hear.wiki(nar);
        //installSpeech(nar);

        return nar;
    }

    public static void installSpeech(NAR nar) {
        nar.runLater(()-> {
            MaryTTSpeech.speak(""); //forces load of TTS so it will be ready ASAP and not load on the first use
            nar.onOp("speak", new AtomicExec((t, n) -> {
                @Nullable TermContainer args = Operator.args(t);
                if (args.AND(x -> !x.op().var)) {
                    String text = Joiner.on(", ").join(args);
                    if (text.isEmpty())
                        return;
                    if (text.charAt(0) != '"')
                        text = "\"" + text + '"';

                    n.believe($.func("speak", args.arrayShared()), Tense.Present);

                    MaryTTSpeech.speak(text);
                }
            }, 0.51f));

//            try {
//                nar.believe($.$("(hear:$1 ==> speak:$1)"), Tense.Eternal);
//                nar.believe($.$("(speak:$1 ==> hear:$1)"), Tense.Eternal);
//                nar.goal($.$("(hear:#1 &| speak:#1)"), Tense.Eternal, 1f);
//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//            }
        });
    }
}

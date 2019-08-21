package spacegraph.space2d.widget.chip;

import jcog.math.MutableEnum;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.button.EnumSwitch;
import spacegraph.space2d.widget.port.TextPort;

public class SpeakChip extends Bordering {

    private final TextPort in;

    enum Modulation {
        AirModem {
            @Override
            protected void accept(String s) {
                new spacegraph.audio.modem.stopcollaboratelisten.AirModem().say(s);
            }
        },
        TinySpeech {
            @Override
            protected void accept(String s) {
                new spacegraph.audio.speech.TinySpeech().say(s);
            }
        },
        System_TTS {
            @Override
            protected void accept(String s) {
                //ex: 'say' command
            }
        }
        //Mary_TTS
        ;
        //..

        abstract protected void accept(String s);
    }

    final MutableEnum<Modulation> mode = new MutableEnum<>(Modulation.AirModem);

    public SpeakChip() {

        set(in = new TextPort() {
            @Override
            public boolean out(String x) {
                mode.get().accept(x);
                return super.out(x);
            }
        });

        east(EnumSwitch.the(mode, "Mode"));

    }

}

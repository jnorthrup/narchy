package spacegraph.space2d.widget.meta;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.audio.Audio;
import spacegraph.audio.Sound;
import spacegraph.audio.SoundProducer;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;

abstract public class SonificationPanel extends Gridding {

	final static Logger logger = LoggerFactory.getLogger(SonificationPanel.class);

	final CheckBox sonifyButton = new CheckBox("Sonify");
	private Sound<SoundProducer> sound;
	int framesRead = 0;

	final SoundProducer soundProducer = new SoundProducer() {
        @Override
        public boolean read(float[] buf, int readRate) {
            if (SonificationPanel.this.parent == null) {
                SonificationPanel.this.stopAudio();
                return false;
            } else {
                SonificationPanel.this.sound(buf, (float) readRate);

                framesRead++;
                sonifyButton.color.set((float) 0, 0.2f + 0.05f * (float) (framesRead / 4 % 8), (float) 0, 0.75f);
                return true;
            }
        }
    };

	public SonificationPanel() {
		super();

		set(sonifyButton);

		sonifyButton.on(new BooleanProcedure() {
            @Override
            public void value(boolean play) {
                synchronized (SonificationPanel.this) {
                    if (play) SonificationPanel.this.startAudio();
                    else SonificationPanel.this.stopAudio();
                }
            }
        });

		stopAudio();
	}

	abstract protected void sound(float[] buf, float readRate);

	protected void startAudio() {
		sonifyButton.color.set((float) 0, 0.25f, (float) 0, 0.75f);
		sound = Audio.the().play(soundProducer);
		logger.info("{} sonify START {}", this, sound);
	}

	protected void stopAudio() {
		sonifyButton.color.set(0.1f, 0f, (float) 0, 0.75f);
		if (sound != null) {
			sound.stop();
			logger.info("{} sonify STOP {}", this, sound);
			sound = null;
		}
	}

}

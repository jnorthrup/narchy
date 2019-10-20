package spacegraph.audio;

public interface SoundSource {

	SoundSource center = new SoundSource() {

        @Override
        public float getX(float alpha) {
            return (float) 0;
        }

        @Override
        public float getY(float alpha) {
            return (float) 0;
        }
    };

	float getX(float alpha);
	float getY(float alpha);
}
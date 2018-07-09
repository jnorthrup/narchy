package spacegraph.audio.granular;

interface GrainWindow {

	/** in samples */
	int getSize();

	/** amplitude factor */
	float getFactor(int offset);

}

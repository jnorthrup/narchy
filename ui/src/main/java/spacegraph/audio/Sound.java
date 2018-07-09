package spacegraph.audio;


/** Auditory element */
public class Sound<S extends SoundProducer> implements SoundSource, Comparable
{
    private static final double l10 = Math.log(10);
    
    public final S producer;
    private final SoundSource source;
    private float volume;
    private float priority;
    
    private float x, y, z;
    private float score;
    
    public float pan;
    public float amplitude;
    
    public Sound(S producer, SoundSource source, float volume, float priority)
    {
        this.producer = producer;
        this.source = source;
        this.volume = volume;
        this.priority = priority;

    }
    
    public boolean update(SoundSource listener, float alpha)
    {
        x = source.getX(alpha)-listener.getX(alpha);
        y = source.getY(alpha)-listener.getY(alpha);
        
        float distSqr = x*x+y*y+z*z;
        float dist = (float)Math.sqrt(distSqr);
        
        float REFERENCE_DISTANCE = 1;
        float ROLLOFF_FACTOR = 2;
        

        float dB = (float)(volume - 20*Math.log(1 + ROLLOFF_FACTOR*(dist-REFERENCE_DISTANCE)/REFERENCE_DISTANCE )/ l10);
        if (dB != dB) dB = 0;

        dB = Math.min(dB, +6);

        
        score = dB*priority;


		
        float p = -x;
        if (p<-1) p = -1;
        if (p>1) p = 1;

        pan = p;
        amplitude = volume / (1.0f + dist); 

        return isLive();
    }

    public void read(float[] buf, int readRate)    {
        producer.read(buf, readRate);
    }

    public void skip(int samplesToSkip, int readRate)
    {
        producer.skip(samplesToSkip, readRate);
    }

    public final boolean isLive()    {
        return producer.isLive();
    }

    @Override
    public int compareTo(Object o) {
        if (this == o) return 0;
        int s = Double.compare(score, ((Sound)o).score);
        return s == 0 ? Integer.compare(hashCode(), o.hashCode()) : s;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public float getX(float alpha) {
        return source.getX(alpha);
    }

    @Override
    public float getY(float alpha) {
        return source.getY(alpha);
    }
}
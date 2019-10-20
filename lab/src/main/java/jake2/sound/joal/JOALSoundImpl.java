/*
 * JOALSoundImpl.java
 * Copyright (C) 2004
 *
 */
package jake2.sound.joal;

import com.jogamp.openal.*;
import com.jogamp.openal.eax.EAX;
import com.jogamp.openal.eax.EAXConstants;
import com.jogamp.openal.eax.EAXFactory;
import jake2.Defines;
import jake2.Globals;
import jake2.game.Cmd;
import jake2.game.GameBase;
import jake2.game.cvar_t;
import jake2.game.entity_state_t;
import jake2.qcommon.Com;
import jake2.qcommon.Cvar;
import jake2.qcommon.FS;
import jake2.qcommon.xcommand_t;
import jake2.sound.*;
import jake2.util.Lib;
import jake2.util.Vargs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * JOALSoundImpl
 */
public final class JOALSoundImpl implements Sound {
	
	static {
		S.register(new JOALSoundImpl());
	}

	static AL al;
	static ALC alc;
	static ALCdevice alcDevice;
	static ALCcontext alcContext;
	static EAX eax;
	
	cvar_t s_volume;
	
	private final int[] buffers = new int[MAX_SFX + STREAM_QUEUE];
	
	private JOALSoundImpl() {
	}

	/* (non-Javadoc)
	 * @see jake2.sound.SoundImpl#Init()
	 */
	@Override
    public boolean Init() {
		        
		try {
		    alc = ALFactory.getALC();
		    alcDevice = alc.alcOpenDevice(null);
		    alcContext = alc.alcCreateContext(alcDevice, null);
		    alc.alcMakeContextCurrent(alcContext);
		    al = ALFactory.getAL();
		    checkError();
		    initOpenALExtensions();		
		} catch (ALException e) {
		    Com.Printf(e.getMessage() + '\n');
		    return false;
		} catch (Throwable e) {
		    Com.Printf(e.toString() + '\n');
		    return false;
		}
		
		s_volume = Cvar.Get("s_volume", "0.7", Defines.CVAR_ARCHIVE);

		al.alGenBuffers(buffers.length, buffers, 0);
        int count = Channel.init(al, buffers);
		Com.Printf("... using " + count + " channels\n");
		al.alDistanceModel(ALConstants.AL_INVERSE_DISTANCE_CLAMPED);
		Cmd.AddCommand("play", new xcommand_t() {
			@Override
            public void execute() {
				Play();
			}
		});
		Cmd.AddCommand("stopsound", new xcommand_t() {
			@Override
            public void execute() {
				StopAllSounds();
			}
		});
		Cmd.AddCommand("soundlist", new xcommand_t() {
			@Override
            public void execute() {
				SoundList();
			}
		});
		Cmd.AddCommand("soundinfo", new xcommand_t() {
			@Override
            public void execute() {
				SoundInfo_f();
			}
		});

		num_sfx = 0;

		Com.Println("sound sampling rate: 44100Hz");

		StopAllSounds();
		Com.Println("------------------------------------");
		return true;
	}
		
	private static void initOpenALExtensions() {
		if (al.alIsExtensionPresent("EAX2.0")) {
			try {
				eax = EAXFactory.getEAX();
				Com.Println("... using EAX2.0");
			} catch (Throwable e) {
				Com.Println(e.getMessage());
				Com.Println("... EAX2.0 not initialized");
				eax = null;
			}
		} else {
			Com.Println("... EAX2.0 not found");
			eax = null;
		}
	}
	
	
    
    
    private final ByteBuffer sfxDataBuffer = Lib.newByteBuffer(2 * 1024 * 1024);
    
    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#RegisterSound(jake2.sound.sfx_t)
     */
    private void initBuffer(byte[] samples, int bufferId, int freq) {
        ByteBuffer data = sfxDataBuffer.slice();
        data.put(samples).flip();
        al.alBufferData(buffers[bufferId], ALConstants.AL_FORMAT_MONO16,
                data, data.limit(), freq);
    }

	private static void checkError() {
		Com.DPrintf("AL Error: " + alErrorString() +'\n');
	}
	
	private static String alErrorString(){
		int error;
        String message = "";
		if ((error = al.alGetError()) != ALConstants.AL_NO_ERROR) {
			switch(error) {
				case ALConstants.AL_INVALID_OPERATION: message = "invalid operation"; break;
				case ALConstants.AL_INVALID_VALUE: message = "invalid value"; break;
				case ALConstants.AL_INVALID_ENUM: message = "invalid enum"; break;
				case ALConstants.AL_INVALID_NAME: message = "invalid name"; break;
				default: message = String.valueOf(error);
			}
		}
		return message; 
	}

	/* (non-Javadoc)
	 * @see jake2.sound.SoundImpl#Shutdown()
	 */
	@Override
    public void Shutdown() {
		StopAllSounds();
		Channel.shutdown();
		al.alDeleteBuffers(buffers.length, buffers, 0);

		Cmd.RemoveCommand("play");
		Cmd.RemoveCommand("stopsound");
		Cmd.RemoveCommand("soundlist");
		Cmd.RemoveCommand("soundinfo");

		
		for (int i = 0; i < num_sfx; i++) {
			if (known_sfx[i].name == null)
				continue;
			known_sfx[i].clear();
		}
		num_sfx = 0;
		alc.alcDestroyContext(alcContext);
		alc.alcCloseDevice(alcDevice);
	}
	
	/* (non-Javadoc)
	 * @see jake2.sound.SoundImpl#StartSound(float[], int, int, jake2.sound.sfx_t, float, float, float)
	 */
	@Override
    public void StartSound(float[] origin, int entnum, int entchannel, sfx_t sfx, float fvol, float attenuation, float timeofs) {

		if (sfx == null)
			return;
			
		if (sfx.name.charAt(0) == '*')
			sfx = RegisterSexedSound(Globals.cl_entities[entnum].current, sfx.name);
		
		if (LoadSound(sfx) == null)
			return; 

		if (attenuation != Defines.ATTN_STATIC)
			attenuation *= 0.5f;

		PlaySound.allocate(origin, entnum, entchannel, buffers[sfx.bufferId], fvol, attenuation, timeofs);
	}

	private final float[] listenerOrigin = {0, 0, 0};
	private final float[] listenerOrientation = {0, 0, 0, 0, 0, 0};
	private final IntBuffer eaxEnv = Lib.newIntBuffer(1);
	private int currentEnv = -1;

	/* (non-Javadoc)
	 * @see jake2.sound.SoundImpl#Update(float[], float[], float[], float[])
	 */
	@Override
    public void Update(float[] origin, float[] forward, float[] right, float[] up) {
		Channel.convertVector(origin, listenerOrigin);		
		al.alListenerfv(ALConstants.AL_POSITION, listenerOrigin, 0);

		Channel.convertOrientation(forward, up, listenerOrientation);		
		al.alListenerfv(ALConstants.AL_ORIENTATION, listenerOrientation, 0);
		
		
		al.alListenerf(ALConstants.AL_GAIN, s_volume.value);
		
		if (eax != null) {

            boolean changeEnv = true;
			if (currentEnv == -1) {
				eaxEnv.put(0, EAXConstants.EAX_ENVIRONMENT_UNDERWATER);
				eax.EAXSet(EAX.LISTENER, EAXConstants.DSPROPERTY_EAXLISTENER_ENVIRONMENT | EAXConstants.DSPROPERTY_EAXLISTENER_DEFERRED, 0, eaxEnv, 4);
				changeEnv = true;
			}

			if ((GameBase.gi.pointcontents.pointcontents(origin)& Defines.MASK_WATER)!= 0) {
				changeEnv = currentEnv != EAXConstants.EAX_ENVIRONMENT_UNDERWATER;
				currentEnv = EAXConstants.EAX_ENVIRONMENT_UNDERWATER;
			} else {
				changeEnv = currentEnv != EAXConstants.EAX_ENVIRONMENT_GENERIC;
				currentEnv = EAXConstants.EAX_ENVIRONMENT_GENERIC;
			}
			if (changeEnv) {
				eaxEnv.put(0, currentEnv);
				eax.EAXSet(EAX.LISTENER, EAXConstants.DSPROPERTY_EAXLISTENER_ENVIRONMENT | EAXConstants.DSPROPERTY_EAXLISTENER_DEFERRED, 0, eaxEnv, 4);
			}
		}

	    Channel.addLoopSounds();
	    Channel.addPlaySounds();
		Channel.playAllSounds(listenerOrigin);
	}

	/* (non-Javadoc)
	 * @see jake2.sound.SoundImpl#StopAllSounds()
	 */
	@Override
    public void StopAllSounds() {
		
		if (al!=null)
			al.alListenerf(ALConstants.AL_GAIN, 0);
	    PlaySound.reset();
	    Channel.reset();
	}
	
	/* (non-Javadoc)
	 * @see jake2.sound.Sound#getName()
	 */
	@Override
    public String getName() {
		return "joal";
	}

	int s_registration_sequence;
	boolean s_registering;

	/* (non-Javadoc)
	 * @see jake2.sound.Sound#BeginRegistration()
	 */
	@Override
    public void BeginRegistration() {
		s_registration_sequence++;
		s_registering = true;
	}

	/* (non-Javadoc)
	 * @see jake2.sound.Sound#RegisterSound(java.lang.String)
	 */
	@Override
    public sfx_t RegisterSound(String name) {
        sfx_t sfx = FindName(name, true);
		sfx.registration_sequence = s_registration_sequence;

		if (!s_registering)
			LoadSound(sfx);
			
		return sfx;
	}

	/* (non-Javadoc)
	 * @see jake2.sound.Sound#EndRegistration()
	 */
	@Override
    public void EndRegistration() {
		int i;
		sfx_t sfx;

		
		for (i = 0; i < num_sfx; i++) {
			sfx = known_sfx[i];
			if (sfx.name == null)
				continue;
			if (sfx.registration_sequence != s_registration_sequence) {
				
				sfx.clear();
			}
		}

		
		for (i = 0; i < num_sfx; i++) {
			sfx = known_sfx[i];
			if (sfx.name == null)
				continue;
			LoadSound(sfx);
		}

		s_registering = false;
	}
	
	sfx_t RegisterSexedSound(entity_state_t ent, String base) {


        String model = null;
        int n = Defines.CS_PLAYERSKINS + ent.number - 1;
		if (Globals.cl.configstrings[n] != null) {
            int p = Globals.cl.configstrings[n].indexOf('\\');
			if (p >= 0) {
				p++;
				model = Globals.cl.configstrings[n].substring(p);
				
				p = model.indexOf('/');
				if (p > 0)
					model = model.substring(0, p);
			}
		}
		
		if (model == null || model.length() == 0)
			model = "male";


        String sexedFilename = "#players/" + model + '/' + base.substring(1);

        sfx_t sfx = FindName(sexedFilename, false);

		if (sfx != null) return sfx;
		
		
		
		
		
		if (FS.FileLength(sexedFilename.substring(1)) > 0) {
			
			return RegisterSound(sexedFilename);
		}
	    
		if ("female".equalsIgnoreCase(model)) {
            String femaleFilename = "player/female/" + base.substring(1);
			if (FS.FileLength("sound/" + femaleFilename) > 0)
			    return AliasName(sexedFilename, femaleFilename);
		}

        String maleFilename = "player/male/" + base.substring(1);
		return AliasName(sexedFilename, maleFilename);
	}

	static final sfx_t[] known_sfx = new sfx_t[MAX_SFX];
	static {
		for (int i = 0; i< known_sfx.length; i++)
			known_sfx[i] = new sfx_t();
	}
	static int num_sfx;

	sfx_t FindName(String name, boolean create) {

        if (name == null)
			Com.Error(Defines.ERR_FATAL, "S_FindName: NULL\n");
		if (name.length() == 0)
			Com.Error(Defines.ERR_FATAL, "S_FindName: empty name\n");

		if (name.length() >= Defines.MAX_QPATH)
			Com.Error(Defines.ERR_FATAL, "Sound name too long: " + name);


        int i;
        for (i = 0; i < num_sfx; i++)
			if (name.equals(known_sfx[i].name)) {
				return known_sfx[i];
			}

		if (!create)
			return null;

		
		for (i = 0; i < num_sfx; i++)
			if (known_sfx[i].name == null)
				
				break;

		if (i == num_sfx) {
			if (num_sfx == MAX_SFX)
				Com.Error(Defines.ERR_FATAL, "S_FindName: out of sfx_t");
			num_sfx++;
		}

        sfx_t sfx = known_sfx[i];
		sfx.clear();
		sfx.name = name;
		sfx.registration_sequence = s_registration_sequence;
		sfx.bufferId = i;

		return sfx;
	}

	/*
	==================
	S_AliasName

	==================
	*/
	sfx_t AliasName(String aliasname, String truename)
	{
        int i;


        for (i=0 ; i < num_sfx ; i++)
			if (known_sfx[i].name == null)
				break;

		if (i == num_sfx)
		{
			if (num_sfx == MAX_SFX)
				Com.Error(Defines.ERR_FATAL, "S_FindName: out of sfx_t");
			num_sfx++;
		}

        sfx_t sfx = known_sfx[i];
		sfx.clear();
		sfx.name = aliasname;
		sfx.registration_sequence = s_registration_sequence;
        String s = truename;
        sfx.truename = s;
		
		sfx.bufferId = i;

		return sfx;
	}

	/*
	==============
	S_LoadSound
	==============
	*/
	public sfxcache_t LoadSound(sfx_t s) {
	    if (s.isCached) return s.cache;
        sfxcache_t sc = WaveLoader.LoadSound(s);
		if (sc != null) {
			initBuffer(sc.data, s.bufferId, sc.speed);
		    s.isCached = true;
		    
		    s.cache.data = null;
		}
		return sc;
	}

	/* (non-Javadoc)
	 * @see jake2.sound.Sound#StartLocalSound(java.lang.String)
	 */
	@Override
    public void StartLocalSound(String sound) {
        sfx_t sfx = RegisterSound(sound);
		if (sfx == null) {
			Com.Printf("S_StartLocalSound: can't cache " + sound + '\n');
			return;
		}
		StartSound(null, Globals.cl.playernum + 1, 0, sfx, 1, 1, 0.0f);		
	}

    private final ShortBuffer streamBuffer = sfxDataBuffer.slice().order(ByteOrder.BIG_ENDIAN).asShortBuffer();

    /* (non-Javadoc)
     * @see jake2.sound.Sound#RawSamples(int, int, int, int, byte[])
     */
    @Override
    public void RawSamples(int samples, int rate, int width, int channels, ByteBuffer data) {
        int format;
        if (channels == 2) {
            format = (width == 2) ? ALConstants.AL_FORMAT_STEREO16
                    : ALConstants.AL_FORMAT_STEREO8;
        } else {
            format = (width == 2) ? ALConstants.AL_FORMAT_MONO16
                    : ALConstants.AL_FORMAT_MONO8;
        }
        
        
        if (format == ALConstants.AL_FORMAT_MONO8) {
            ShortBuffer sampleData = streamBuffer;
            for (int i = 0; i < samples; i++) {
                int value = (data.get(i) & 0xFF) - 128;
                sampleData.put(i, (short) value);
            }
            format = ALConstants.AL_FORMAT_MONO16;
            width = 2;
            data = sfxDataBuffer.slice();
        }

        Channel.updateStream(data, samples * channels * width, format, rate);
    }
    
    @Override
    public void disableStreaming() {
        Channel.disableStreaming();
    }
	
	/*
	===============================================================================

	console functions

	===============================================================================
	*/

	void Play() {
        int i = 1;
        while (i < Cmd.Argc()) {
            String name = Cmd.Argv(i);
            if (name.indexOf('.') == -1)
				name += ".wav";

			RegisterSound(name);
			StartLocalSound(name);
			i++;
		}
	}

	static void SoundList() {

        int total = 0;
		for (int i = 0; i < num_sfx; i++) {
            sfx_t sfx = known_sfx[i];
            if (sfx.registration_sequence == 0)
				continue;
            sfxcache_t sc = sfx.cache;
            if (sc != null) {
                int size = sc.length * sc.width * (sc.stereo + 1);
                total += size;
				if (sc.loopstart >= 0)
					Com.Printf("L");
				else
					Com.Printf(" ");
				Com.Printf("(%2db) %6i : %s\n", new Vargs(3).add(sc.width * 8).add(size).add(sfx.name));
			} else {
				if (sfx.name.charAt(0) == '*')
					Com.Printf("  placeholder : " + sfx.name + '\n');
				else
					Com.Printf("  not loaded  : " + sfx.name + '\n');
			}
		}
		Com.Printf("Total resident: " + total + '\n');
	}
	
	static void SoundInfo_f() {
		Com.Printf("%5d stereo\n", new Vargs(1).add(1));
		Com.Printf("%5d samples\n", new Vargs(1).add(22050));
		Com.Printf("%5d samplebits\n", new Vargs(1).add(16));
		Com.Printf("%5d speed\n", new Vargs(1).add(44100));
	}
}

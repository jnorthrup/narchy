/*
 * S_DMA.java
 * Copyright (C) 2004
 * 
 * $Id: SND_DMA.java,v 1.3 2005-12-04 17:26:55 cawe Exp $
 */
/*
 Copyright (C) 1997-2001 Id Software, Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */


package jake2.sound.jsound;

import jake2.Defines;
import jake2.client.CL_ents;
import jake2.game.Cmd;
import jake2.game.cvar_t;
import jake2.game.entity_state_t;
import jake2.qcommon.Com;
import jake2.qcommon.Cvar;
import jake2.qcommon.FS;
import jake2.qcommon.xcommand_t;
import jake2.sound.WaveLoader;
import jake2.sound.sfx_t;
import jake2.sound.sfxcache_t;
import jake2.util.Math3D;
import jake2.util.Vargs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * SND_DMA TODO implement sound system
 */
public class SND_DMA extends SND_MIX {

    
    
    
    
    
    
    
    static final int SOUND_FULLVOLUME = 80;

    static final float SOUND_LOOPATTENUATE = 0.003f;

    static int s_registration_sequence;

    static boolean sound_started;

    static final float[] listener_origin = { 0, 0, 0 };

    static final float[] listener_forward = { 0, 0, 0 };

    static final float[] listener_right = { 0, 0, 0 };

    static final float[] listener_up = { 0, 0, 0 };

    static boolean s_registering;

    static int soundtime; 

    
    
    
    
    static final int MAX_SFX = (MAX_SOUNDS * 2);

    static final sfx_t[] known_sfx = new sfx_t[MAX_SFX];
    static {
        for (int i = 0; i < known_sfx.length; i++)
            known_sfx[i] = new sfx_t();
    }

    static int num_sfx;

    static final int MAX_PLAYSOUNDS = 128;

    static final playsound_t[] s_playsounds = new playsound_t[MAX_PLAYSOUNDS];
    static {
        for (int i = 0; i < MAX_PLAYSOUNDS; i++) {
            s_playsounds[i] = new playsound_t();
        }
    }

    static final playsound_t s_freeplays = new playsound_t();

    static int s_beginofs;

    static cvar_t s_testsound;

    static cvar_t s_loadas8bit;

    static cvar_t s_khz;

    static cvar_t s_show;

    static cvar_t s_mixahead;

    static cvar_t s_primary;

    
    
    
    
    
    
    
    
    

    static void SoundInfo_f() {
        if (!sound_started) {
            Com.Printf("sound system not started\n");
            return;
        }

        Com.Printf("%5d stereo\n", new Vargs(1).add(dma.channels - 1));
        Com.Printf("%5d samples\n", new Vargs(1).add(dma.samples));
        
        Com.Printf("%5d samplebits\n", new Vargs(1).add(dma.samplebits));
        Com.Printf("%5d submission_chunk\n", new Vargs(1)
                .add(dma.submission_chunk));
        Com.Printf("%5d speed\n", new Vargs(1).add(dma.speed));
    }

    /*
     * ================ S_Init ================
     */
    public static void Init() {

        Com.Printf("\n------- sound initialization -------\n");

        cvar_t cv = Cvar.Get("s_initsound", "0", 0);
        if (cv.value == 0.0f)
            Com.Printf("not initializing.\n");
        else {
            s_volume = Cvar.Get("s_volume", "0.7", CVAR_ARCHIVE);
            s_khz = Cvar.Get("s_khz", "11", CVAR_ARCHIVE);
            s_loadas8bit = Cvar.Get("s_loadas8bit", "1", CVAR_ARCHIVE);
            s_mixahead = Cvar.Get("s_mixahead", "0.2", CVAR_ARCHIVE);
            s_show = Cvar.Get("s_show", "0", 0);
            s_testsound = Cvar.Get("s_testsound", "0", 0);
            s_primary = Cvar.Get("s_primary", "0", CVAR_ARCHIVE); 
                                                                  

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

            if (!SNDDMA_Init())
                return;

            InitScaletable();

            sound_started = true;
            num_sfx = 0;

            soundtime = 0;
            paintedtime = 0;

            Com.Printf("sound sampling rate: " + dma.speed + '\n');

            StopAllSounds();
        }
        Com.Printf("------------------------------------\n");
    }

    
    
    

    public static void Shutdown() {

        if (!sound_started)
            return;

        SNDDMA_Shutdown();

        sound_started = false;

        Cmd.RemoveCommand("play");
        Cmd.RemoveCommand("stopsound");
        Cmd.RemoveCommand("soundlist");
        Cmd.RemoveCommand("soundinfo");


        sfx_t[] sfx;
        int i;
        for (i = 0, sfx = known_sfx; i < num_sfx; i++) {
            if (sfx[i].name == null)
                continue;

            
            sfx[i].clear();
        }

        num_sfx = 0;
    }

    
    
    

    /*
     * ================== S_FindName
     * 
     * ==================
     */
    static sfx_t FindName(String name, boolean create) {

        if (name == null)
            Com.Error(ERR_FATAL, "S_FindName: NULL\n");
        if (name.length() == 0)
            Com.Error(ERR_FATAL, "S_FindName: empty name\n");

        if (name.length() >= MAX_QPATH)
            Com.Error(ERR_FATAL, "Sound name too long: " + name);


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
                Com.Error(ERR_FATAL, "S_FindName: out of sfx_t");
            num_sfx++;
        }

        sfx_t sfx = known_sfx[i];
        
        sfx.clear();
        sfx.name = name;
        sfx.registration_sequence = s_registration_sequence;

        return sfx;
    }

    /*
     * ================== S_AliasName
     * 
     * ==================
     */
    static sfx_t AliasName(String aliasname, String truename) {

        int i;

        
        

        
        for (i = 0; i < num_sfx; i++)
            if (known_sfx[i].name == null)
                break;

        if (i == num_sfx) {
            if (num_sfx == MAX_SFX)
                Com.Error(ERR_FATAL, "S_FindName: out of sfx_t");
            num_sfx++;
        }

        sfx_t sfx = known_sfx[i];
        
        
        sfx.name = aliasname;
        sfx.registration_sequence = s_registration_sequence;
        sfx.truename = truename;

        return sfx;
    }

    /*
     * ===================== S_BeginRegistration
     * 
     * =====================
     */
    public static void BeginRegistration() {
        s_registration_sequence++;
        s_registering = true;
    }

    /*
     * ================== S_RegisterSound
     * 
     * ==================
     */
    public static sfx_t RegisterSound(String name) {

        if (!sound_started)
            return null;

        sfx_t sfx = FindName(name, true);
        sfx.registration_sequence = s_registration_sequence;

        if (!s_registering)
            WaveLoader.LoadSound(sfx);

        return sfx;
    }

    /*
     * ===================== S_EndRegistration
     * 
     * =====================
     */
    public static void EndRegistration() {
        int i;
        sfx_t sfx;
        int size;

        
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.name == null)
                continue;
            if (sfx.registration_sequence != s_registration_sequence) { 
                                                                        
                                                                        
                                                                        
                
                sfx.clear();
            } else {
                
                
                
                
                
                
            }

        }

        
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.name == null)
                continue;
            WaveLoader.LoadSound(sfx);
        }

        s_registering = false;
    }

    

    /*
     * ================= S_PickChannel =================
     */
    static channel_t PickChannel(int entnum, int entchannel) {

        if (entchannel < 0)
            Com.Error(ERR_DROP, "S_PickChannel: entchannel<0");


        int first_to_die = -1;
        int life_left = 0x7fffffff;
        for (int ch_idx = 0; ch_idx < MAX_CHANNELS; ch_idx++) {
            if (entchannel != 0 
                    && channels[ch_idx].entnum == entnum
                    && channels[ch_idx].entchannel == entchannel) { 
                                                                    
                                                                    
                                                                    
                                                                    
                first_to_die = ch_idx;
                break;
            }

            
            if ((channels[ch_idx].entnum == cl.playernum + 1)
                    && (entnum != cl.playernum + 1)
                    && channels[ch_idx].sfx != null)
                continue;

            if (channels[ch_idx].end - paintedtime < life_left) {
                life_left = channels[ch_idx].end - paintedtime;
                first_to_die = ch_idx;
            }
        }

        if (first_to_die == -1)
            return null;

        channel_t ch = channels[first_to_die];
        
        ch.clear();

        return ch;
    }

    /*
     * ================= S_SpatializeOrigin
     * 
     * Used for spatializing channels and autosounds =================
     */
    static void SpatializeOrigin(float[] origin, float master_vol,
            float dist_mult, channel_t ch) {

        if (cls.state != ca_active) {
            ch.leftvol = ch.rightvol = 255;
            return;
        }


        float[] source_vec = {0, 0, 0};
        Math3D.VectorSubtract(origin, listener_origin, source_vec);

        float dist = Math3D.VectorNormalize(source_vec);
        dist -= SOUND_FULLVOLUME;
        if (dist < 0)
            dist = 0; 
        dist *= dist_mult;

        float dot = Math3D.DotProduct(listener_right, source_vec);

        float rscale;
        float lscale;
        if (dma.channels == 1 || dist_mult == 0.0f) {
                                                      
            rscale = 1.0f;
            lscale = 1.0f;
        } else {
            rscale = 0.5f * (1.0f + dot);
            lscale = 0.5f * (1.0f - dot);
        }


        float scale = (1.0f - dist) * rscale;
        ch.rightvol = (int) (master_vol * scale);
        if (ch.rightvol < 0)
            ch.rightvol = 0;

        scale = (1.0f - dist) * lscale;
        ch.leftvol = (int) (master_vol * scale);
        if (ch.leftvol < 0)
            ch.leftvol = 0;
    }

    /*
     * ================= S_Spatialize =================
     */
    static void Spatialize(channel_t ch) {


        if (ch.entnum == cl.playernum + 1) {
            ch.leftvol = ch.master_vol;
            ch.rightvol = ch.master_vol;
            return;
        }

        float[] origin = {0, 0, 0};
        if (ch.fixed_origin) {
            Math3D.VectorCopy(ch.origin, origin);
        } else
            CL_ents.GetEntitySoundOrigin(ch.entnum, origin);

        SpatializeOrigin(origin, (float) ch.master_vol, ch.dist_mult, ch);
    }

    /*
     * ================= S_AllocPlaysound =================
     */
    static playsound_t AllocPlaysound() {

        playsound_t ps = s_freeplays.next;
        if (ps == s_freeplays)
            return null; 

        
        ps.prev.next = ps.next;
        ps.next.prev = ps.prev;

        return ps;
    }

    /*
     * ================= S_FreePlaysound =================
     */
    static void FreePlaysound(playsound_t ps) {
        
        ps.prev.next = ps.next;
        ps.next.prev = ps.prev;

        
        ps.next = s_freeplays.next;
        s_freeplays.next.prev = ps;
        ps.prev = s_freeplays;
        s_freeplays.next = ps;
    }

    /*
     * =============== S_IssuePlaysound
     * 
     * Take the next playsound and begin it on the channel This is never called
     * directly by S_Play*, but only by the update loop. ===============
     */
    static void IssuePlaysound(playsound_t ps) {

        if (s_show.value != 0.0f)
            Com.Printf("Issue " + ps.begin + '\n');

        channel_t ch = PickChannel(ps.entnum, ps.entchannel);
        if (ch == null) {
            FreePlaysound(ps);
            return;
        }

        
        if (ps.attenuation == ATTN_STATIC)
            ch.dist_mult = ps.attenuation * 0.001f;
        else
            ch.dist_mult = ps.attenuation * 0.0005f;
        ch.master_vol = (int) ps.volume;
        ch.entnum = ps.entnum;
        ch.entchannel = ps.entchannel;
        ch.sfx = ps.sfx;
        Math3D.VectorCopy(ps.origin, ch.origin);
        ch.fixed_origin = ps.fixed_origin;

        Spatialize(ch);

        ch.pos = 0;
        sfxcache_t sc = WaveLoader.LoadSound(ch.sfx);
        ch.end = paintedtime + sc.length;

        
        FreePlaysound(ps);
    }

    static sfx_t RegisterSexedSound(entity_state_t ent, String base) {


        String model = "male";
        int n = CS_PLAYERSKINS + ent.number - 1;
        if (cl.configstrings[n] != null) {
            int p = cl.configstrings[n].indexOf('\\');
            if (p >= 0) {
                p++;
                model = cl.configstrings[n].substring(p);
                
                p = model.indexOf('/');
                if (p > 0)
                    model = model.substring(0, p - 1);
            }
        }
        
        if (model == null || model.length() == 0)
            model = "male";

        
        String sexedFilename = "#players/" + model + '/' + base.substring(1);


        sfx_t sfx = FindName(sexedFilename, false);

        if (sfx == null) {
            
            RandomAccessFile f = null;
            try {
                f = FS.FOpenFile(sexedFilename.substring(1));
            } catch (IOException e) {
            }
            if (f != null) {
                
                try {
                    FS.FCloseFile(f);
                } catch (IOException e1) {
                }
                sfx = RegisterSound(sexedFilename);
            } else {
                
                
                
                String maleFilename = "player/male/" + base.substring(1);
                sfx = AliasName(sexedFilename, maleFilename);
            }
        }

        return sfx;
    }

    
    
    

    /*
     * ==================== S_StartSound
     * 
     * Validates the parms and ques the sound up if pos is NULL, the sound will
     * be dynamically sourced from the entity Entchannel 0 will never override a
     * playing sound ====================
     */
    public static void StartSound(float[] origin, int entnum, int entchannel,
            sfx_t sfx, float fvol, float attenuation, float timeofs) {

        if (!sound_started)
            return;

        if (sfx == null)
            return;

        if (sfx.name.charAt(0) == '*')
            sfx = RegisterSexedSound(cl_entities[entnum].current, sfx.name);

        
        sfxcache_t sc = WaveLoader.LoadSound(sfx);
        if (sc == null)
            return; 

        int vol = (int) (fvol * 255);

        
        playsound_t ps = AllocPlaysound();
        if (ps == null)
            return;

        if (origin != null) {
            Math3D.VectorCopy(origin, ps.origin);
            ps.fixed_origin = true;
        } else
            ps.fixed_origin = false;

        ps.entnum = entnum;
        ps.entchannel = entchannel;
        ps.attenuation = attenuation;
        ps.volume = vol;
        ps.sfx = sfx;

        
        int start = (int) (cl.frame.servertime * 0.001f * dma.speed + s_beginofs);
        if (start < paintedtime) {
            start = paintedtime;
            s_beginofs = (int) (start - (cl.frame.servertime * 0.001f * dma.speed));
        } else if (start > paintedtime + 0.3f * dma.speed) {
            start = (int) (paintedtime + 0.1f * dma.speed);
            s_beginofs = (int) (start - (cl.frame.servertime * 0.001f * dma.speed));
        } else {
            s_beginofs -= 10;
        }

        if (timeofs == 0.0f)
            ps.begin = paintedtime;
        else
            ps.begin = (long) (start + timeofs * dma.speed);

        
        playsound_t sort;
        for (sort = s_pendingplays.next; sort != s_pendingplays
                && sort.begin < ps.begin; sort = sort.next)
            ;

        ps.next = sort;
        ps.prev = sort.prev;

        ps.next.prev = ps;
        ps.prev.next = ps;
    }

    /*
     * ================== S_StartLocalSound ==================
     */
    public static void StartLocalSound(String sound) {

        if (!sound_started)
            return;

        sfx_t sfx = RegisterSound(sound);
        if (sfx == null) {
            Com.Printf("S_StartLocalSound: can't cache " + sound + '\n');
            return;
        }
        StartSound(null, cl.playernum + 1, 0, sfx, 1, 1, 0);
    }

    /*
     * ================== S_ClearBuffer ==================
     */
    static void ClearBuffer() {

        if (!sound_started)
            return;

        s_rawend = 0;

        int clear;
        if (dma.samplebits == 8)
            clear = 0x80;
        else
            clear = 0;

        SNDDMA_BeginPainting();
        if (dma.buffer != null)
            
            
            SNDDMA_Submit();
    }

    /*
     * ================== S_StopAllSounds ==================
     */
    public static void StopAllSounds() {

        if (!sound_started)
            return;

        
        
        s_freeplays.next = s_freeplays.prev = s_freeplays;
        s_pendingplays.next = s_pendingplays.prev = s_pendingplays;

        int i;
        for (i = 0; i < MAX_PLAYSOUNDS; i++) {
            s_playsounds[i].clear();
            s_playsounds[i].prev = s_freeplays;
            s_playsounds[i].next = s_freeplays.next;
            s_playsounds[i].prev.next = s_playsounds[i];
            s_playsounds[i].next.prev = s_playsounds[i];
        }

        
        
        for (i = 0; i < MAX_CHANNELS; i++)
            channels[i].clear();

        ClearBuffer();
    }

    /*
     * ================== S_AddLoopSounds
     * 
     * Entities with a ->sound field will generated looped sounds that are
     * automatically started, stopped, and merged together as the entities are
     * sent to the client ==================
     */
    static void AddLoopSounds() {
        int left, right;

        if (cl_paused.value != 0.0f)
            return;

        if (cls.state != ca_active)
            return;

        if (!cl.sound_prepped)
            return;

        entity_state_t ent;
        int num;
        int[] sounds = new int[Defines.MAX_EDICTS];
        int i;
        for (i = 0; i < cl.frame.num_entities; i++) {
            num = (cl.frame.parse_entities + i) & (MAX_PARSE_ENTITIES - 1);
            ent = cl_parse_entities[num];
            sounds[i] = ent.sound;
        }

        for (i = 0; i < cl.frame.num_entities; i++) {
            if (sounds[i] == 0)
                continue;

            sfx_t sfx = cl.sound_precache[sounds[i]];
            if (sfx == null)
                continue;
            sfxcache_t sc = sfx.cache;
            if (sc == null)
                continue;

            num = (cl.frame.parse_entities + i) & (MAX_PARSE_ENTITIES - 1);
            ent = cl_parse_entities[num];

            channel_t tch = new channel_t();
            
            SpatializeOrigin(ent.origin, 255.0f, SOUND_LOOPATTENUATE, tch);
            int left_total = tch.leftvol;
            int right_total = tch.rightvol;
            for (int j = i + 1; j < cl.frame.num_entities; j++) {
                if (sounds[j] != sounds[i])
                    continue;
                sounds[j] = 0; 

                num = (cl.frame.parse_entities + j) & (MAX_PARSE_ENTITIES - 1);
                ent = cl_parse_entities[num];

                SpatializeOrigin(ent.origin, 255.0f, SOUND_LOOPATTENUATE, tch);
                left_total += tch.leftvol;
                right_total += tch.rightvol;
            }

            if (left_total == 0 && right_total == 0)
                continue;


            channel_t ch = PickChannel(0, 0);
            if (ch == null)
                return;

            if (left_total > 255)
                left_total = 255;
            if (right_total > 255)
                right_total = 255;
            ch.leftvol = left_total;
            ch.rightvol = right_total;
            ch.autosound = true; 
            ch.sfx = sfx;
            ch.pos = paintedtime % sc.length;
            ch.end = paintedtime + sc.length - ch.pos;
        }
    }

    

    /*
     * ============ S_RawSamples
     * 
     * Cinematic streaming and voice over network ============
     */
    static void RawSamples(int samples, int rate, int width, int channels,
            ByteBuffer data) {

        int src, dst;

        if (!sound_started)
            return;

        if (s_rawend < paintedtime)
            s_rawend = paintedtime;
        float scale = (float) rate / dma.speed;


        int i;
        if (channels == 2 && width == 2) {
            if (scale == 1.0) { 
            
            
            
            
            
            
            
            
            
            } else {
                for (i = 0;; i++) {
                    
                    
                    
                    
                    
                    
                    
                    
                    
                }
            }
        } else if (channels == 1 && width == 2) {
            for (i = 0;; i++) {
                
                
                
                
                
                
                
                
                
            }
        } else if (channels == 2 && width == 1) {
            for (i = 0;; i++) {
                
                
                
                
                
                
                
                
                
            }
        } else if (channels == 1 && width == 1) {
            for (i = 0;; i++) {
                
                
                
                
                
                
                
                
            }
        }
    }

    
    

    /*
     * ============ S_Update
     * 
     * Called once each time through the main loop ============
     */
    public static void Update(float[] origin, float[] forward, float[] right,
            float[] up) {

        if (!sound_started)
            return;

        
        
        
        if (cls.disable_screen != 0.0f) {
            ClearBuffer();
            return;
        }

        
        if (s_volume.modified)
            InitScaletable();

        Math3D.VectorCopy(origin, listener_origin);
        Math3D.VectorCopy(forward, listener_forward);
        Math3D.VectorCopy(right, listener_right);
        Math3D.VectorCopy(up, listener_up);

        channel_t combine = null;

        
        channel_t ch;
        for (int i = 0; i < MAX_CHANNELS; i++) {
            ch = channels[i];
            if (ch.sfx == null)
                continue;
            if (ch.autosound) { 
                
                ch.clear();
                continue;
            }
            Spatialize(ch); 
            if (ch.leftvol == 0 && ch.rightvol == 0) {
                
                ch.clear();
                continue;
            }
        }

        
        AddLoopSounds();

        
        
        
        if (s_show.value != 0.0f) {
            int total = 0;

            for (int i = 0; i < MAX_CHANNELS; i++) {
                ch = channels[i];
                if (ch.sfx != null && (ch.leftvol != 0 || ch.rightvol != 0)) {
                    Com.Printf(ch.leftvol + " " + ch.rightvol + ' '
                            + ch.sfx.name + '\n');
                    total++;
                }
            }

            
            
        }

        
        Update_();
    }

    static int buffers;

    static int oldsamplepos;

    static void GetSoundtime() {


        int fullsamples = dma.samples / dma.channels;


        int samplepos = SNDDMA_GetDMAPos();

        if (samplepos < oldsamplepos) {
            buffers++; 

            if (paintedtime > 0x40000000) { 
                                            
                buffers = 0;
                paintedtime = fullsamples;
                StopAllSounds();
            }
        }
        oldsamplepos = samplepos;

        soundtime = buffers * fullsamples + samplepos / dma.channels;
    }

    static void Update_() {

        if (!sound_started)
            return;

        SNDDMA_BeginPainting();

        if (dma.buffer == null)
            return;

        
        GetSoundtime();

        
        if (paintedtime < soundtime) {
            Com.DPrintf("S_Update_ : overflow\n");
            paintedtime = soundtime;
        }


        int endtime = (int) (soundtime + s_mixahead.value * dma.speed);


        endtime = (endtime + dma.submission_chunk - 1)
                & ~(dma.submission_chunk - 1);
        int samps = dma.samples >> (dma.channels - 1);
        if (endtime - soundtime > samps)
            endtime = soundtime + samps;

        PaintChannels(endtime);

        SNDDMA_Submit();
    }

    /*
     * ===============================================================================
     * 
     * console functions
     * 
     * ===============================================================================
     */

    static void Play() {

        int i = 1;
        while (i < Cmd.Argc()) {
            String name = Cmd.Argv(i);
            if (name.indexOf('.') == -1)
                name += ".wav";

            sfx_t sfx = RegisterSound(name);
            StartSound(null, cl.playernum + 1, 0, sfx, 1.0f, 1.0f, 0.0f);
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
                Com.Printf("(%2db) %6i : %s\n", new Vargs(3).add(sc.width * 8)
                        .add(size).add(sfx.name));
            } else {
                if (sfx.name.charAt(0) == '*')
                    Com.Printf("  placeholder : " + sfx.name + '\n');
                else
                    Com.Printf("  not loaded  : " + sfx.name + '\n');
            }
        }
        Com.Printf("Total resident: " + total + '\n');
    }

}
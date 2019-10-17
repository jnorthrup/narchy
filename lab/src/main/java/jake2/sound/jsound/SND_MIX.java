/*
 * SND_MIX.java
 * Copyright (C) 2004
 * 
 * $Id: SND_MIX.java,v 1.2 2004-09-22 19:22:09 salomo Exp $
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

import jake2.game.cvar_t;
import jake2.sound.WaveLoader;
import jake2.sound.sfx_t;
import jake2.sound.sfxcache_t;
import jake2.util.Math3D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * SND_MIX
 */
public class SND_MIX extends SND_JAVA {

    static final int MAX_CHANNELS = 32;

    static final int MAX_RAW_SAMPLES = 8192;

    static class playsound_t {
        playsound_t prev, next;

        sfx_t sfx;

        float volume;

        float attenuation;

        int entnum;

        int entchannel;

        boolean fixed_origin; 

        final float[] origin = { 0, 0, 0 };

        long begin; 

        public void clear() {
            prev = next = null;
            sfx = null;
            volume = attenuation = begin = entnum = entchannel = 0;
            fixed_origin = false;
            Math3D.VectorClear(origin);
        }
    }

    static class channel_t {
        sfx_t sfx; 

        int leftvol; 

        int rightvol; 

        int end; 

        int pos; 

        int looping; 

        int entnum; 

        int entchannel; 

        final float[] origin = { 0, 0, 0 }; 

        float dist_mult; 

        int master_vol; 

        boolean fixed_origin; 

        boolean autosound; 

        void clear() {
            sfx = null;
            dist_mult = leftvol = rightvol = end = pos = looping = entnum = entchannel = master_vol = 0;
            Math3D.VectorClear(origin);
            fixed_origin = autosound = false;
        }
    }

    static class portable_samplepair_t {
        int left;

        int right;
    }

    static cvar_t s_volume;

    static int s_rawend;

    
    
    
    
    
    static final int PAINTBUFFER_SIZE = 2048;

    
    
    static final IntBuffer paintbuffer = IntBuffer.allocate(PAINTBUFFER_SIZE * 2);

    static final int[][] snd_scaletable = new int[32][256];

    
    
    static IntBuffer snd_p;

    static ShortBuffer snd_out;

    static int snd_linear_count;

    static int snd_vol;

    public static int paintedtime;

    static final playsound_t s_pendingplays = new playsound_t();

    
    
    static final IntBuffer s_rawsamples = IntBuffer.allocate(MAX_RAW_SAMPLES * 2);

    static final channel_t[] channels = new channel_t[MAX_CHANNELS];
    static {
        for (int i = 0; i < MAX_CHANNELS; i++)
            channels[i] = new channel_t();
    }

    static void WriteLinearBlastStereo16() {
        int i;
        int val;

        for (i = 0; i < snd_linear_count; i += 2) {
            val = snd_p.get(i) >> 8;
            if (val > 0x7fff)
                snd_out.put(i, (short) 0x7fff);
            else if (val < (short) 0x8000)
                snd_out.put(i, (short) 0x8000);
            else
                snd_out.put(i, (short) val);

            val = snd_p.get(i + 1) >> 8;
            if (val > 0x7fff)
                snd_out.put(i + 1, (short) 0x7fff);
            else if (val < (short) 0x8000)
                snd_out.put(i + 1, (short) 0x8000);
            else
                snd_out.put(i + 1, (short) val);
        }
    }

    static void TransferStereo16(ByteBuffer pbuf, int endtime) {
        int lpos;

        snd_p = paintbuffer;
        int lpaintedtime = paintedtime;

        while (lpaintedtime < endtime) {
            
            lpos = lpaintedtime & ((dma.samples >> 1) - 1);

            
            snd_out = pbuf.asShortBuffer();
            snd_out.position(lpos << 1);
            snd_out = snd_out.slice();

            snd_linear_count = (dma.samples >> 1) - lpos;
            if (lpaintedtime + snd_linear_count > endtime)
                snd_linear_count = endtime - lpaintedtime;

            snd_linear_count <<= 1;

            
            WriteLinearBlastStereo16();

            
            paintbuffer.position(snd_linear_count);
            snd_p = paintbuffer.slice();

            lpaintedtime += (snd_linear_count >> 1);
        }
    }

    /*
     * =================== S_TransferPaintBuffer
     * 
     * ===================
     */
    static void TransferPaintBuffer(int endtime) {
        int out_idx;
        int count;
        int out_mask;
        int p;
        int step;
        int val;
        

        ByteBuffer pbuf = ByteBuffer.wrap(dma.buffer);
        pbuf.order(ByteOrder.LITTLE_ENDIAN);

        if (SND_DMA.s_testsound.value != 0.0f) {
            int i;


            int count2 = (endtime - paintedtime) * 2;
            int v;
            for (i = 0; i < count2; i += 2) {
                v = (int) (Math.sin((paintedtime + i) * 0.1) * 20000 * 256);
                paintbuffer.put(i, v);
                paintbuffer.put(i + 1, v);
            }
        }

        if (dma.samplebits == 16 && dma.channels == 2) { 
            TransferStereo16(pbuf, endtime);
        } else { 
            p = 0;
            count = (endtime - paintedtime) * dma.channels;
            out_mask = dma.samples - 1;
            out_idx = paintedtime * dma.channels & out_mask;
            step = 3 - dma.channels;

            if (dma.samplebits == 16) {
                
                ShortBuffer out = pbuf.asShortBuffer();
                while (count-- > 0) {
                    val = paintbuffer.get(p) >> 8;
                    p += step;
                    if (val > 0x7fff)
                        val = 0x7fff;
                    else if (val < (short) 0x8000)
                        val = (short) 0x8000;
                    out.put(out_idx, (short) val);
                    
                    out_idx = (out_idx + 1) & out_mask;
                }
            } else if (dma.samplebits == 8) {
                
                while (count-- > 0) {
                    val = paintbuffer.get(p) >> 8;
                    p += step;
                    if (val > 0x7fff)
                        val = 0x7fff;
                    else if (val < (short) 0x8000)
                        val = (short) 0x8000;
                    pbuf.put(out_idx, (byte) (val >>> 8));
                    out_idx = (out_idx + 1) & out_mask;
                }
            }
        }
    }

    /*
     * ===============================================================================
     * 
     * CHANNEL MIXING
     * 
     * ===============================================================================
     */
    static void PaintChannels(int endtime) {
        int i;
        int end;
        channel_t ch;
        sfxcache_t sc;
        int ltime, count;
        playsound_t ps;

        snd_vol = (int) (s_volume.value * 256);

        
        while (paintedtime < endtime) {
            
            end = endtime;
            if (endtime - paintedtime > PAINTBUFFER_SIZE)
                end = paintedtime + PAINTBUFFER_SIZE;

            
            while (true) {
                ps = s_pendingplays.next;
                if (ps == s_pendingplays)
                    break; 
                if (ps.begin <= paintedtime) {
                    SND_DMA.IssuePlaysound(ps);
                    continue;
                }

                if (ps.begin < end)
                    end = (int) ps.begin; 
                break;
            }

            
            if (s_rawend < paintedtime) {
                
                for (i = 0; i < (end - paintedtime) * 2; i++) {
                    paintbuffer.put(i, 0);
                }
                
                
            } else { 
                int s;

                int stop = (end < s_rawend) ? end : s_rawend;

                for (i = paintedtime; i < stop; i++) {
                    s = i & (MAX_RAW_SAMPLES - 1);
                    
                    paintbuffer.put((i - paintedtime) * 2, s_rawsamples
                            .get(2 * s));
                    paintbuffer.put((i - paintedtime) * 2 + 1, s_rawsamples
                            .get(2 * s) + 1);
                }
                
                
                
                
                for (; i < end; i++) {
                    
                    
                    paintbuffer.put((i - paintedtime) * 2, 0);
                    paintbuffer.put((i - paintedtime) * 2 + 1, 0);
                }
            }

            
            
            for (i = 0; i < MAX_CHANNELS; i++) {
                ch = channels[i];
                ltime = paintedtime;

                while (ltime < end) {
                    if (ch.sfx == null || (ch.leftvol == 0 && ch.rightvol == 0))
                        break;

                    
                    count = end - ltime;

                    
                    if (ch.end - ltime < count)
                        count = ch.end - ltime;

                    sc = WaveLoader.LoadSound(ch.sfx);
                    if (sc == null)
                        break;

                    if (count > 0 && ch.sfx != null) {
                        if (sc.width == 1)
                            PaintChannelFrom8(ch, sc, count, ltime
                                    - paintedtime);
                        else
                            PaintChannelFrom16(ch, sc, count, ltime
                                    - paintedtime);

                        ltime += count;
                    }

                    
                    if (ltime >= ch.end) {
                        if (ch.autosound) { 
                                            
                            ch.pos = 0;
                            ch.end = ltime + sc.length;
                        } else if (sc.loopstart >= 0) {
                            ch.pos = sc.loopstart;
                            ch.end = ltime + sc.length - ch.pos;
                        } else { 
                            ch.sfx = null;
                        }
                    }
                }

            }

            
            TransferPaintBuffer(end);
            paintedtime = end;
        }
    }

    static void InitScaletable() {
        int i, j;
        int scale;

        s_volume.modified = false;
        for (i = 0; i < 32; i++) {
            scale = (int) (i * 8 * 256 * s_volume.value);
            for (j = 0; j < 256; j++)
                snd_scaletable[i][j] = ((byte) j) * scale;
        }
    }

    static void PaintChannelFrom8(channel_t ch, sfxcache_t sc, int count,
            int offset) {
        int data;
        int i;
        portable_samplepair_t samp;

        if (ch.leftvol > 255)
            ch.leftvol = 255;
        if (ch.rightvol > 255)
            ch.rightvol = 255;


        int[] lscale = snd_scaletable[ch.leftvol >> 3];
        int[] rscale = snd_scaletable[ch.rightvol >> 3];
        int sfx = ch.pos;

        

        for (i = 0; i < count; i++, offset++) {
            int left = paintbuffer.get(offset * 2);
            int right = paintbuffer.get(offset * 2 + 1);
            data = sc.data[sfx + i];
            left += lscale[data];
            right += rscale[data];
            paintbuffer.put(offset * 2, left);
            paintbuffer.put(offset * 2 + 1, right);
        }

        ch.pos += count;
    }

    private static ByteBuffer bb;

    static void PaintChannelFrom16(channel_t ch, sfxcache_t sc, int count,
            int offset) {
        int data;
        int left, right;
        int i;
        portable_samplepair_t samp;

        int leftvol = ch.leftvol * snd_vol;
        int rightvol = ch.rightvol * snd_vol;
        ByteBuffer bb = ByteBuffer.wrap(sc.data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sb = bb.asShortBuffer();
        int sfx = ch.pos;

        
        for (i = 0; i < count; i++, offset++) {
            left = paintbuffer.get(offset * 2);
            right = paintbuffer.get(offset * 2 + 1);
            data = sb.get(sfx + i);
            left += (data * leftvol) >> 8;
            right += (data * rightvol) >> 8;
            paintbuffer.put(offset * 2, left);
            paintbuffer.put(offset * 2 + 1, right);
        }

        ch.pos += count;
    }

}
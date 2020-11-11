/*
 * Copyright (C) 2014 Federico Dossena.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.dosse.bwentrain.renderers.isochronic;

import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.IRenderer;
import com.dosse.bwentrain.sound.ISoundDevice;
import java.io.DataInputStream;

/**
 * Render a Preset as Isochronic pulses and noise
 *
 * @author dosse
 */
public class IsochronicRenderer extends Thread implements IRenderer {

    private static float[] NOISE_SAMPLE = null;

    static {
        //load noise from noise.dat
        new Thread() {
            @Override
            public void run() {
                try {
                    DataInputStream dis = new DataInputStream(IsochronicRenderer.class.getResourceAsStream("/com/dosse/bwentrain/renderers/isochronic/noise.dat"));
                    //the format of noise.dat is very simple: an int (4 bytes) tell the length in samples of the sample, and then a bunch of float 0-1, which are the actual samples
                    int len = dis.readInt();
                    float[] noiseSample = new float[len];
                    for (int i = 0; i < len; i++) {
                        noiseSample[i] = dis.readFloat() * 0.5f; //0.5f is preamp
                    }
                    NOISE_SAMPLE = noiseSample;
                } catch (Throwable t) {
                    //something went wrong, use empty sample
                    NOISE_SAMPLE = new float[1];
                }
            }
        }.start();
    }

    private boolean playing = false, //playing or paused
            stopASAP = false; //must stop and close sound device
    private final Preset p; //the preset to render
    private final ISoundDevice speaker; //the sound device
    private static final int BASE_BUFFER_SIZE = 1024; //size of the buffer at 44100Hz (actual size differs depending on actual sample rate). larger buffer = better performance but lower precision in EntrainmentTrackRenderer; smaller buffer = the exact opposite; 1024 seems to be a good compromise
    private int loopCounter; //how many times do we still have to loop (if the preset loops)
    private double t = 0; //time in Preset
    private EntrainmentTrackRenderer[] renderers; //renderers of isochronic pulses

    /**
     * Render a Preset as Isochronic pulses and noise.<br>
     * Remember to call play() to start playing.
     *
     * @param p the Preset to render
     * @param speaker the sound device
     * @param loopCounter how many times should the preset loop (only for
     * presets that loop): 0=no loop, -1=loop infinitely, n=loop n times
     */
    public IsochronicRenderer(Preset p, ISoundDevice speaker, int loopCounter) {
        this.p = p;
        this.speaker = speaker;
        this.loopCounter = loopCounter;
        start();
    }

    @Override
    public float getVolume() {
        return speaker.getVolume();
    }

    @Override
    public void setVolume(float vol) {
        speaker.setVolume(vol);
    }

    @Override
    public void play() {
        playing = true;
    }

    @Override
    public void pause() {
        playing = false;
    }

    @Override
    public void stopPlaying() {
        playing = false;
        stopASAP = true;
        while (isAlive()) { //wait for thread to terminate
            try {
                sleep(1);
            } catch (InterruptedException ex) {
            }
        }
    }

    @Override
    public boolean isPlaying() {
        return playing && isAlive();
    }

    @Override
    public boolean isClosed() {
        return !isAlive();
    }

    @Override
    public float getPosition() {
        return (float) t;
    }

    @Override
    public void setPosition(float t) {
        this.t = t < 0 ? 0 : t > p.getLength() ? p.getLength() : t;
        if (renderers != null) {
            for (EntrainmentTrackRenderer r : renderers) {
                r.setT(t);
            }
        }
    }

    @Override
    public float getLength() {
        return p.getLength();
    }

    @Override
    public void run() {
        while (!playing || NOISE_SAMPLE == null) { //wait for play() to be called the first time AND for noise to be loaded
            try {
                Thread.sleep(1); //this is so fucking brutal
            } catch (InterruptedException ex) {
            }
            if (stopASAP) {
                return; //stopped before even starting
            }
        }
        speaker.open(); //start sound device
        int bufferSize = (int) Math.ceil(BASE_BUFFER_SIZE * (speaker.getSampleRate() / 44100f)); //calculate actual buffer size
        float[] buffer = new float[bufferSize]; //we'll render the output here
        boolean multiChannel = speaker.getChannelCount() != 1; //if not mono, enable code for multiple channels
        float[] multiChannelBuffer = new float[bufferSize * speaker.getChannelCount()]; //only used if channels > 1
        double tStep = 1.0 / speaker.getSampleRate(); //length of a sample in seconds
        renderers = new EntrainmentTrackRenderer[p.getEntrainmentTrackCount()]; //all the EntrainmentTrackRenderers, one for each EntrainmentTrack in the preset
        float totalTrackVolume = 0;
        for (int i = 0; i < p.getEntrainmentTrackCount(); i++) {
            totalTrackVolume += p.getEntrainmentTrack(i).getTrackVolume();
        }
        if (totalTrackVolume <= 1) { //no need to normalize volume if total volume <=1
            totalTrackVolume = 1;
        }
        for (int i = 0; i < renderers.length; i++) { //initialize renderers
            renderers[i] = new EntrainmentTrackRenderer(p.getEntrainmentTrack(i), speaker.getSampleRate(), 0.5f * (p.getEntrainmentTrack(i).getTrackVolume() / totalTrackVolume)); //total volume will be limited to half the amplitude
            renderers[i].setT((float) t); //just in case time was changed before playback was started
        }
        float[][] renderersOutput = new float[renderers.length][]; //we'll keep the output of each EntrainmentTrackRenderer here
        double nT = 0; //used for internal calculations
        for (;;) {
            if (playing) { //not paused, must render
                for (int i = 0; i < renderers.length; i++) { //render a buffer for each EntrainmentTrack
                    renderersOutput[i] = renderers[i].render(bufferSize);
                }
                int j;
                float noiseV = p.getNoise((float) t); //get noise volume at current time (calculated once per buffer to improve performance)
                for (int i = 0; i < bufferSize; i++) { //for each sample
                    buffer[i] = NOISE_SAMPLE[(int) ((nT % 1) * NOISE_SAMPLE.length)] * noiseV; //get a noise sample and apply volume
                    nT += tStep;
                    for (j = 0; j < renderersOutput.length; j++) {
                        buffer[i] += renderersOutput[j][i]; //mix the EntrainmentTrackRenderers' outputs together
                    }
                }
                t += tStep * bufferSize;
                if (multiChannel) { //adapt for channels >1, takes time so mono is recommended
                    int ch = speaker.getChannelCount();
                    for (int i = 0; i < bufferSize; i++) {
                        for (j = 0; j < ch; j++) { //copy the same sample to all channels
                            multiChannelBuffer[i * ch + j] = buffer[i];
                        }
                    }
                }
                if (t >= p.getLength()) { //we're at the end of the Preset
                    if (!p.loops() || (loopCounter-- == 0)) { //no need to loop
                        setPosition(0);
                        playing = false;
                    } else { //must loop
                        t = p.getLoop();
                        for (EntrainmentTrackRenderer etr : renderers) {
                            etr.setT(p.getLoop());
                        }
                    }
                }
                speaker.write(multiChannel ? multiChannelBuffer : buffer); //write to the sound device: buffer if mono, multichannel buffer otherwise
            } else {
                //is paused
                if (stopASAP) { //must stop and close device
                    speaker.close();
                    return;
                } else { //otherwise, just wait
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
    }

    @Override
    public Preset getPreset() {
        return p;
    }
}

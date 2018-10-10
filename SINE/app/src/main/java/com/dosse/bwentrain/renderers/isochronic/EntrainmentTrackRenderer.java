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

import com.dosse.bwentrain.core.EntrainmentTrack;

/**
 * Renders an EntrainmentTrack as Isochronic pulses
 *
 * @author dosse
 */
public class EntrainmentTrackRenderer {

    // SINE LUT STUFF
    private final static int SINE_LUT_SIZE = 32768;
    private static float[] SINE = null;

    static {
        // init SINE LUT
        new Thread() {
            @Override
            public void run() {
                float[] sineSample=new float[SINE_LUT_SIZE];
                double step = (Math.PI * 2) / SINE_LUT_SIZE;
                for (int i = 0; i < SINE_LUT_SIZE; i++) {
                    sineSample[i] = (float) Math.sin(step * i);
                }
                SINE=sineSample;
            }
        }.start();
    }

    private final EntrainmentTrack e; //the EntrainmentTrack to render
    private double t = 0, //current time in EntrainmentTrack
            cT = 0, ecT = 0; //used for internal calculations
    private final float sampleRate;
    private static final int PULSE_SIZE = 8192;
    private static float[] PULSE = null; //actual isochronic pulse sample @1Hz will be generated later

    static {
        //init pulse sample
        new Thread() {
            @Override
            public void run() {
                float[] p = SmoothPulseGenerator.generate(PULSE_SIZE);
                PULSE=p;
            }
        }.start();
    }
    private final float trackMultiplier; //volume of this track as float 0-1 (controlled by IsochronicRenderer to avoid clipping)

    /**
     * initializes the renderer
     *
     * @param e EntrainmentTrack to render
     * @param sampleRate sample rate
     * @param trackMultiplier track volume as float 0-1 (controlled by
     * IsochronicRenderer to avoid clipping)
     */
    public EntrainmentTrackRenderer(EntrainmentTrack e, float sampleRate, float trackMultiplier) {
        this.e = e;
        this.sampleRate = sampleRate;
        this.trackMultiplier = trackMultiplier;
    }

    private float[] tempBuffer = null; //used to avoid reallocating memory each time render is called, as long as it's always called with the same len parameter

    /**
     * renders a piece of the EntrainmentTrack.<br>
     * in order to improve performance, the calculations are done only once, so
     * it is recommended to use small values for the len parameter: 1024 is
     * recommended
     *
     * @param len how many samples?. if &lt;=0 it will be changed to 1
     * @return float[len] with the samples as float 0-1
     */
    public float[] render(int len) {
        while(SINE==null||PULSE==null){ try {
            //wait for all data to be ready
            Thread.sleep(1);
            } catch (InterruptedException ex) {
            }
        }
        if (len <= 0) { //invalid length, fix it
            len = 1;
        }
        if (tempBuffer == null || tempBuffer.length != len) { //must reallocate buffer
            tempBuffer = new float[len];
        }
        double tStep = 1.0 / sampleRate; //length of a sample in seconds
        //now, we'll actualluy render the samples. to increase performance, the base frequency, the track volume, the entrainment frequency and the entrainment volume are only calculated once per buffer
        float baseF = e.getBaseFrequency((float) t), vol = e.getVolume((float) t), trackVol = e.getTrackVolume() * trackMultiplier, entF = e.getEntrainmentFrequency((float) t);
        if (vol == 0 || trackVol == 0) {
            //volume is 0, nothing to render
            for (int i = 0; i < len; i++) {
                tempBuffer[i] = 0;
            }
        } else {
            //do actual rendering
            for (int i = 0; i < len; i++) {
                tempBuffer[i] = trackVol * vol * SINE[(int) ((cT % 1) * SINE_LUT_SIZE)] * PULSE[(int) ((ecT % 1) * PULSE_SIZE)];
                cT += tStep * baseF;
                ecT += tStep * entF;
            }
        }
        t += tStep * len;
        return tempBuffer;
    }

    /**
     * get current time in preset
     *
     * @return time in seconds
     */
    public float getT() {
        return (float) t;
    }

    /**
     * set time in preset
     *
     * @param t new time in seconds. if &lt;0 or &gt;length, it will be changed
     * accordingly
     */
    public void setT(float t) {
        this.t = t < 0 ? 0 : t > e.getLength() ? e.getLength() : t;
    }
}

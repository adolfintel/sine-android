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

/**
 * This class is used by EntrainmentTrackRenderer to generate a single pulse,
 * which is then played back at a varying frequency to create the isochronic
 * pulses
 *
 * @author dosse
 */
public class SmoothPulseGenerator {

    private static final double SQRT_2PI = Math.sqrt(2 * Math.PI);
    private static final float DEFAULT_SMOOTH = 0.7f, DEFAULT_WIDTH = 0.15f, DEFAULT_FLOOR = 0.1f, DEFAULT_CEIL = 1;

    private static final float gauss(float x, float mu, float sigma) {
        return (float) ((1 / (sigma * SQRT_2PI)) * Math.exp(-(Math.pow(x - mu, 2) / (2 * Math.pow(sigma, 2)))));
    }

    /**
     * generate a pulse. uses a gaussian function
     * @param length length in samples
     * @param smooth &lt;1=ramp up fast, decrease slowly; 1=gaussian bell; &gt;1=ramp up slowly, decrease fast. must be &gt;=0.01
     * @param width how wide is the bell of the gaussian function. must be &gt;=0.01 
     * @param floor minimum value of the bell (0-1)
     * @param ceil maximum value of the bell (0-1)
     * @return float[length] with the required pulse. each sample is a float 0-1
     */
    public static float[] generate(int length, float smooth, float width, float floor, float ceil) {
        long t=System.nanoTime();
        if (length <= 0) {
            length = 0;
        }
        smooth = smooth < 0.01f ? 0.01f : smooth;
        width = width < 0.01f ? 0.01f : width;
        floor = floor < 0 ? 0 : floor > 1 ? 1 : floor;
        ceil = ceil < 0 ? 0 : ceil > 1 ? 1 : ceil;
        if (floor > ceil) {
            float temp = floor;
            floor = ceil;
            ceil = temp;
        }
        float[] out = new float[length];
        float max = -1;
        for (int i = 0; i < length; i++) {
            float x = (float) i / (float) length;
            out[i] = gauss((float) Math.pow(x, smooth), 0.5f, width);
            if (out[i] > max) {
                max = out[i];
            }
        }
        for (int i = 0; i < length; i++) {
            out[i] = floor + (ceil - floor) * (out[i] / max);
        }
        long diff=System.nanoTime()-t;
        return out;
    }

    /**
     * generate a pulse of given legth with default parameters.
     * @param length length in samples
     * @return float[length] with the required pulse. each sample is a float 0-1
     */
    public static float[] generate(int length) {
        return generate(length, DEFAULT_SMOOTH, DEFAULT_WIDTH, DEFAULT_FLOOR, DEFAULT_CEIL);
    }
}

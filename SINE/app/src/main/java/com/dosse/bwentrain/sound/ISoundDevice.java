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
package com.dosse.bwentrain.sound;

/**
 * Interface for sound backends<br>
 * All sound backends must implements the methods in this class<br>
 * See com.dosse.bwentrain.sound.backends.PCAudioOut for an example
 *
 * @author dosse
 */
public interface ISoundDevice {

    /**
     * is the device closed?
     *
     * @return true if it's closed, false otherwise
     */
    public boolean isClosed();

    /**
     * close the device (stops playback and cannot be used again)
     */
    public void close();

    /**
     * start the device. may throw exceptions if the device was closed
     */
    public void open();

    /**
     * get number of channels, for instance 1=mono, 2=stereo
     *
     * @return number of channels
     */
    public int getChannelCount();

    /**
     * get number of bits per sample, usually 16
     *
     * @return bits
     */
    public int getBitsPerSample();

    /**
     * get sample rate, usually 44100
     *
     * @return sample rate
     */
    public float getSampleRate();

    /**
     * writes data to the sound device.<br>
     * format: each element in the array is a float 0-1; for multichannel
     * devices, channel data should be interleaved (es. for stereo,
     * data[0]=first sample of left channel, data[1]=first sample of right
     * channel)
     *
     * @param data samples
     */
    public void write(float[] data);
    /**
     * set the new volume
     * @param vol as float 0-1
     */
    public void setVolume(float vol);
    /**
     * get the volume
     * @return current volume as float 0-1
     */
    public float getVolume();
}

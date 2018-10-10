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
package com.dosse.bwentrain.renderers;

import com.dosse.bwentrain.core.Preset;

/**
 * Renderer interface.<br>
 * A renderer class must implement all the methods in this class.<br>
 * See package com.dosse.bwentrain.renderers.isochronic for an example
 *
 * @author dosse
 */
public interface IRenderer {

    /**
     * start rendering
     */
    public void play();

    /**
     * pause rendering
     */
    public void pause();

    /**
     * stop rendering (and close any sound backend)
     */
    public void stopPlaying();

    /**
     * is it rendering?
     *
     * @return true if not paused or stopped
     */
    public boolean isPlaying();

    /**
     * is it closed
     *
     * @return true if rendering is stopped (and sound backend closed)
     */
    public boolean isClosed();

    /**
     * get position in preset
     *
     * @return position in seconds
     */
    public float getPosition();

    /**
     * set new position in preset
     *
     * @param t new position in seconds
     */
    public void setPosition(float t);

    /**
     * get preset length. does not count looping
     *
     * @return length in seconds
     */
    public float getLength();

    /**
     * get volume
     *
     * @return volume as float 0-1
     */
    public float getVolume();

    /**
     * set volume
     *
     * @param vol new volume as float 0-1
     */
    public void setVolume(float vol);
    /**
     * get Preset being rendered
     * @return the Preset
     */
    public Preset getPreset();
}

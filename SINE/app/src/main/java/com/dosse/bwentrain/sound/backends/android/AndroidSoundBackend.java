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
package com.dosse.bwentrain.sound.backends.android;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.dosse.bwentrain.sound.ISoundDevice;

public class AndroidSoundBackend implements ISoundDevice {
	private AudioTrack speaker;
	private float volume=1;
	public AndroidSoundBackend(float sampleRate, int nChannels) throws Exception{
		if(nChannels<1||nChannels>2) throw new Exception("Only mono and stereo are supported");
		int sr=(int)sampleRate, cConfig=nChannels==1?AudioFormat.CHANNEL_OUT_MONO:nChannels==2?AudioFormat.CHANNEL_OUT_STEREO:AudioFormat.CHANNEL_OUT_DEFAULT;
		int bSize=AudioTrack.getMinBufferSize(sr, cConfig, AudioFormat.ENCODING_PCM_16BIT);
		speaker=new AudioTrack(AudioManager.STREAM_MUSIC, sr, cConfig, AudioFormat.ENCODING_PCM_16BIT, bSize, AudioTrack.MODE_STREAM);
		
	}
	@Override
	public void close() {
		if(isClosed()) return;
		speaker.stop();	
	}

	@Override
	public int getBitsPerSample() {
		return 16;
	}

	@Override
	public int getChannelCount() {
		return speaker.getChannelCount();
	}

	@Override
	public float getSampleRate() {
		return speaker.getSampleRate();
	}

	@Override
	public float getVolume() {
		return volume;
	}

	@Override
	public boolean isClosed() {
		return speaker.getPlayState()==AudioTrack.PLAYSTATE_STOPPED;
	}

	@Override
	public void open() {
		speaker.play();
	}

	@Override
	public void setVolume(float v) {
		volume=v<0?0:v>1?1:v;
	}
	private short[] tempBuffer=null; //used to avoid reallocating memory every time write is called, as long as write is always called with an array of the same size
	@Override
	public void write(float[] data) {
		if(isClosed()) return;
		if(tempBuffer==null||tempBuffer.length!=data.length){
			tempBuffer=new short[data.length];
		}
		for(int i=0;i<data.length;i++){
			tempBuffer[i] = (short) (data[i] * volume * Short.MAX_VALUE);
		}
		speaker.write(tempBuffer, 0, tempBuffer.length);
	}

}

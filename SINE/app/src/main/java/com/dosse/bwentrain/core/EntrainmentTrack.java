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
package com.dosse.bwentrain.core;

import java.io.Serializable;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A 3-tuple: Entrainment Frequency, Entrainment Volume and Base Frequency
 * envelopes<br>
 * Also has a Track Volume
 *
 * @author dosse
 */
public class EntrainmentTrack implements Serializable {

    private static final long serialVersionUID = 1L;
    private float length = 0; //length of the Track in seconds
    private Envelope ent, vol, baseFreq; //the 3 Envelopes (Entrainment Frequency, Entrainment Volume, Base Frequency)
    private float trackVolume = 1; //Track volume (0-1)

    /**
     * creates a new Entrainment Track with the specified length<br>
     * Initially, all 3 envelopes will have 1 Point with t=0 and val=0 (can't
     * hear anything)<br>
     * Track Volume is 1 by default (max)
     *
     * @param length initial length of the Track
     */
    public EntrainmentTrack(float length) {
        this.length = length;
        ent = new Envelope("entrainmentFrequency", length);
        vol = new Envelope("volume", length);
        baseFreq = new Envelope("baseFrequency", length);
    }

    /**
     * creates an EntrainmentTrack from a node in an XML document
     *
     * @param xmlNode the EntrainmentTrack node
     * @throws Exception if something goes wrong while parsing the XML
     */
    public EntrainmentTrack(Element xmlNode) throws Exception {
        try {
            length = Float.parseFloat(xmlNode.getAttribute("length"));
            if (length < 0) { //invalid length
                throw new Exception();
            }
            trackVolume = Float.parseFloat(xmlNode.getAttribute("trackVolume"));
            if (trackVolume < 0 || trackVolume > 1) { //invalid Track Volume
                throw new Exception();
            }
            //read and parse a list of Envelopes. Must find 3 named respectively entrainmentFrequency, volume and baseFrequency.
            NodeList envelopes = xmlNode.getChildNodes();
            for (int i = 0; i < envelopes.getLength(); i++) {
                Element e = (Element) envelopes.item(i);
                if (e.getAttribute("name").equalsIgnoreCase("entrainmentFrequency")) { //found entrainment frequency
                    if (ent == null) {
                        ent = new Envelope(e);
                    } else {//duplicate entrainment frequency
                        throw new Exception();
                    }
                }
                if (e.getAttribute("name").equalsIgnoreCase("volume")) { //found volume
                    if (vol == null) {
                        vol = new Envelope(e);
                    } else { //dupliate volume
                        throw new Exception();
                    }
                }
                if (e.getAttribute("name").equalsIgnoreCase("baseFrequency")) { //found base frequency
                    if (baseFreq == null) {
                        baseFreq = new Envelope(e);
                    } else { //duplicate base frequency
                        throw new Exception();
                    }
                }
            }
            if (ent == null || vol == null || baseFreq == null) { //didn't find one of the 3 Envelopes
                throw new Exception();
            }
            if (ent.getLength() != length || vol.getLength() != length || baseFreq.getLength() != length) { //invalid length for one of the Envelopes
                throw new Exception();
            }
        } catch (Throwable t) {
            throw new Exception();
        }
    }

    /**
     * changes the length of the Track, and all 3 Envelopes.
     *
     * @param newLength new length (if &lt;0, it will be changed to 0)
     */
    protected void setLength(float newLength) {
        if (newLength < 0) {
            newLength = 0;
        }
        if (newLength == length) { //nothing to do
            return;
        }
        //set length for all 3 envelopes
        ent.setLength(newLength);
        vol.setLength(newLength);
        baseFreq.setLength(newLength);
        //update Track length
        length = newLength;
    }

    /**
     * get Track length
     *
     * @return track length in seconds
     */
    public float getLength() {
        return length;
    }

    /**
     * get entrainment frequency at time t
     *
     * @param t time
     * @return frequency
     */
    public float getEntrainmentFrequency(float t) {
        return ent.get(t);
    }

    /**
     * get entrainment volume at time t
     *
     * @param t time
     * @return volume
     */
    public float getVolume(float t) {
        return vol.get(t);
    }

    /**
     * get base frequency at time t
     *
     * @param t time
     * @return frequency
     */
    public float getBaseFrequency(float t) {
        return baseFreq.get(t);
    }

    /**
     * get track volume
     *
     * @return volume as float 0-1
     */
    public float getTrackVolume() {
        return trackVolume;
    }

    /**
     * set track volume
     *
     * @param trackVolume volume as float 0-1; if &lt;0, it will be changed to
     * 0; if &gt;1, it will be changed to 1
     */
    public void setTrackVolume(float trackVolume) {
        this.trackVolume = trackVolume < 0 ? 0 : trackVolume > 1 ? 1 : trackVolume;
    }

    /**
     * get the entrainment frequency Envelope to add/remove/edit points
     *
     * @return entrainment frequency Envelope
     */
    public Envelope getEntrainmentFrequencyEnvelope() {
        return ent;
    }

    /**
     * get the volume Envelope to add/remove/edit points
     *
     * @return volume Envelope
     */
    public Envelope getVolumeEnvelope() {
        return vol;
    }

    /**
     * get the base frequency Envelope to add/remove/edit points
     *
     * @return base frequency Envelope
     */
    public Envelope getBaseFrequencyEnvelope() {
        return baseFreq;
    }

    /**
     * outputs the entire Track as an XML element<br>
     * Example: &lt;EntrainmentTrack length='60.0' trackVolume='0.8'&gt;<br>
     * ... 3 ENVELOPES, SEE CLASS ENVELOPE FOR EXAMPLE ...<br>
     * &lt;/EntrainmentTrack&gt;
     *
     * @return xml
     */
    public Element toXML() {
        try {
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element t = d.createElement("EntrainmentTrack");
            d.appendChild(t);
            t.setAttribute("length", "" + length);
            t.setAttribute("trackVolume", "" + trackVolume);
            t.appendChild(d.adoptNode(ent.toXML()));
            t.appendChild(d.adoptNode(vol.toXML()));
            t.appendChild(d.adoptNode(baseFreq.toXML()));
            return t;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EntrainmentTrack)) {
            return false;
        }
        EntrainmentTrack e = (EntrainmentTrack) o;
        return e.trackVolume == trackVolume && e.baseFreq.equals(baseFreq) && e.ent.equals(ent) && e.vol.equals(vol) && e.length == length;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Float.floatToIntBits(this.length);
        hash = 67 * hash + (this.ent != null ? this.ent.hashCode() : 0);
        hash = 67 * hash + (this.vol != null ? this.vol.hashCode() : 0);
        hash = 67 * hash + (this.baseFreq != null ? this.baseFreq.hashCode() : 0);
        hash = 67 * hash + Float.floatToIntBits(this.trackVolume);
        return hash;
    }

    /**
     * removes (most) useless points from the track's envelopes.a point is
     * useless if it has the same value as the point before and the point after
     * it. values are considered identical if their difference is &lt;tolerance
     *
     * @param tolerance 2 points are considered identical if their difference is
     * &lt;tolerance
     * @return number of points removed
     */
    public int optimize(float tolerance) {
        return ent.optimize(tolerance) + vol.optimize(tolerance) + baseFreq.optimize(tolerance);
    }

    /**
     * a track is considered useless if it's constantly at volume/frequency/base
     * frequency 0.
     *
     * @return true if useless, false otherwise
     */
    public boolean isUseless() {
        return trackVolume == 0 || (vol.getPointCount() == 1 && vol.getVal(0) == 0) || (baseFreq.getPointCount() == 1 && baseFreq.getVal(0) == 0) || (ent.getPointCount() == 1 && ent.getVal(0) == 0);
    }

    @Override
    public EntrainmentTrack clone() {
        EntrainmentTrack e = new EntrainmentTrack(length);
        e.baseFreq = baseFreq.clone();
        e.ent = ent.clone();
        e.vol = vol.clone();
        e.trackVolume = trackVolume;
        return e;
    }
}

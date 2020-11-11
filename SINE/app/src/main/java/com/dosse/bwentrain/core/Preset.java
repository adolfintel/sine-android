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

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * a brainwave entrainment preset<br>
 * has 1 Envelope for the Noise and 1+ EntrainmentTracks
 *
 * @author dosse
 */
public class Preset implements Serializable {

    private static final long serialVersionUID = 1L;

    private EntrainmentTrack[] ent = new EntrainmentTrack[]{new EntrainmentTrack(0)}; //the tracks; again, manually managed memory for speed and initalized with an empty EntrainmentTrack

    private Envelope noise = new Envelope("noise", 0); //noise Envelope

    private float length = 0;//length in seconds of the entire preset

    private float loop = -1; //where looping starts: -1=no loop, >0=loop at that position

    private String title, author, description;

    /**
     * creates a Preset
     *
     * @param length length in seconds
     * @param loop where the loop begins (-1 for no looping, invalid values=no
     * looping)
     * @param title preset title
     * @param author preset author
     * @param description preset description
     */
    public Preset(float length, float loop, String title, String author, String description) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.loop = loop < 0 || loop > length ? -1 : loop;
        setLength(length);
    }

    /**
     * creates a Preset from a node in an XML document
     *
     * @param xmlNode the Preset node
     * @throws Exception if something goes wrong while parsing the XML
     */
    public Preset(Element xmlNode) throws Exception {
        try {
            length = Float.parseFloat(xmlNode.getAttribute("length"));
            if (length < 0) {
                throw new Exception(); //invalid length
            }
            String lop = xmlNode.getAttribute("loop");
            if (lop.isEmpty()) {
                loop = -1;
            } else {
                loop = Float.parseFloat(lop);
                if (loop < 0 || loop > length) { //invalid loop marker
                    throw new Exception();
                }
            }
            NodeList temp;
            Element tempElement;
            temp = xmlNode.getElementsByTagName("PresetInfos"); //look for the PresetInfos node
            if (temp.getLength() != 1) { //there is none
                throw new Exception();
            }
            //there it is!
            Element presetInfos = (Element) temp.item(0);
            if (presetInfos.getParentNode() != xmlNode) { //not in the right place
                throw new Exception();
            }
            temp = presetInfos.getElementsByTagName("Title");  //find the title
            if (temp.getLength() != 1) { //no title
                throw new Exception();
            }
            tempElement = (Element) temp.item(0);//there it is!
            if (tempElement.getParentNode() != presetInfos) { //not in the right place
                throw new Exception();
            }
            try {
                title = tempElement.getFirstChild().getNodeValue(); //read the title
            } catch (Throwable t) { //title is empty
                title = "";
            }
            temp = presetInfos.getElementsByTagName("Author"); //find the author
            if (temp.getLength() != 1) { //no author
                throw new Exception();
            }
            tempElement = (Element) temp.item(0);//there it is!
            if (tempElement.getParentNode() != presetInfos) { //not in the right place
                throw new Exception();
            }
            try {
                author = tempElement.getFirstChild().getNodeValue(); //read the author
            } catch (Throwable t) { //author is empty
                author = "";
            }
            temp = presetInfos.getElementsByTagName("Description"); //find the description
            if (temp.getLength() != 1) { //no description
                throw new Exception();
            }
            tempElement = (Element) temp.item(0);//there it is!
            if (tempElement.getParentNode() != presetInfos) { //not in the right place
                throw new Exception();
            }
            try {
                description = tempElement.getFirstChild().getNodeValue(); //read the description
            } catch (Throwable t) { //description is empty
                description = "";
            }
            //find noise Envelope
            temp = xmlNode.getElementsByTagName("Envelope"); //get all Envelopes
            Element noize = null;
            for (int i = 0; i < temp.getLength(); i++) {
                Element e = (Element) temp.item(i);
                if (e.getParentNode() != xmlNode) { //not the one we were looking for
                    continue;
                }
                if (e.getAttribute("name").equalsIgnoreCase("noise")) { //there it is
                    if (noize == null) {
                        noize = e;
                    } else { //duplicate noise
                        throw new Exception();
                    }
                }
            }
            if (noize == null) { //no noise Envelope
                throw new Exception();
            }
            if (noize.getParentNode() != xmlNode) { //not in the right place
                throw new Exception();
            }
            noise = new Envelope(noize); //read noise Envelope
            temp = xmlNode.getElementsByTagName("EntrainmentTrack"); //find all EntrainmentTracks
            if (temp.getLength() < 1) { //there are none
                throw new Exception();
            }
            ArrayList<EntrainmentTrack> ets = new ArrayList<EntrainmentTrack>();
            for (int i = 0; i < temp.getLength(); i++) {
                Element e = (Element) temp.item(i);
                if (e.getParentNode() != xmlNode) { //not what we were looking for
                    continue;
                }
                EntrainmentTrack et = new EntrainmentTrack(e); //found it
                if (et.getLength() != length) { //wrong length
                    throw new Exception();
                }
                ets.add(et);
            }
            if (ets.size() < 1) { //there must be at least 1 track
                throw new Exception();
            }
            //store the tracks
            EntrainmentTrack[] newEnt = new EntrainmentTrack[ets.size()];
            for (int i = 0; i < ets.size(); i++) {
                newEnt[i] = ets.get(i);
            }
            ent = newEnt;
        } catch (Throwable t) {
            throw new Exception("Invalid XML");
        }
    }

    /**
     * get the number of EntrainmentTracks
     *
     * @return number of EntrainmentTracks
     */
    public int getEntrainmentTrackCount() {
        return ent.length;
    }

    /**
     * gets the length of the preset
     *
     * @return length in seconds
     */
    public float getLength() {
        return length;
    }

    /**
     * get loop start position
     *
     * @return loop start position in seconds or -1
     */
    public float getLoop() {
        return loop;
    }

    /**
     * does the preset loop?
     *
     * @return true of it loops, false if it doesn't
     */
    public boolean loops() {
        return loop != -1;
    }

    /**
     * set looping position
     *
     * @param loop loop marker in seconds. -1 or invalid values=no loop
     */
    public void setLoop(float loop) {
        this.loop = loop < 0 || loop > length ? -1 : loop;
    }

    /**
     * adds an empty EntrainmentTrack
     */
    public void addEntrainmentTrack() {
        EntrainmentTrack e = new EntrainmentTrack(length);
        EntrainmentTrack[] newEnt = new EntrainmentTrack[ent.length + 1];
        System.arraycopy(ent, 0, newEnt, 0, ent.length);
        newEnt[newEnt.length - 1] = e;
        ent = newEnt;
    }

    /**
     * clones an EntrainmentTrack. does nothing if parameter is invalid
     *
     * @param i Track id. if &lt;0 or &gt;=track count does nothing
     */
    public void cloneTrack(int i) {
        if (i < 0 || i >= ent.length) {
            return;
        }
        try {
            EntrainmentTrack e = new EntrainmentTrack(ent[i].toXML());
            EntrainmentTrack[] newEnt = new EntrainmentTrack[ent.length + 1];
            System.arraycopy(ent, 0, newEnt, 0, ent.length);
            newEnt[newEnt.length - 1] = e;
            ent = newEnt;
        } catch (Throwable ex) {
            //this will never happen
        }
    }

    /**
     * removes the specified EntrainemntTrack<br>
     * Note: the last Track cannot be removed. Invalid values will do nothing
     *
     * @param i Track id
     */
    public void removeEntrainmentTrack(int i) {
        if (ent.length == 1 || i >= ent.length) {
            return;
        }
        //copy the Tracks except for Track i
        EntrainmentTrack[] newEnt = new EntrainmentTrack[ent.length - 1];
        for (int j = 0, idx = 0; j < ent.length; j++) {
            if (j != i) {
                newEnt[idx++] = ent[j];
            }
        }
        ent = newEnt;
    }

    /**
     * changes the length<br>
     * if the loop marker is now outside the Preset, looping is disabled
     *
     * @param newLength new length in seconds (if &lt;0, will be set to 0)
     */
    public void setLength(float newLength) {
        if (newLength < 0) {
            newLength = 0;
        }
        if (newLength == length) { //nothing to do
            return;
        }
        //set length for noise
        noise.setLength(newLength);
        //set length for all EntrainmentTracks
        for (EntrainmentTrack e : ent) {
            e.setLength(newLength);
        }
        length = newLength;
        if (loop >= length) {
            loop = -1; //if the loop marker is outside the Preset, disable looping
        }
    }

    /**
     * get one of the EntrainmentTracks<br>
     *
     * @param i Track id. if invalid, returns null
     * @return the EntrainmentTrack
     */
    public EntrainmentTrack getEntrainmentTrack(int i) {
        if (i < 0 || i >= ent.length) {
            return null;
        }
        return ent[i];
    }

    /**
     * get the noise Envelope
     *
     * @return noise Envelope
     */
    public Envelope getNoiseEnvelope() {
        return noise;
    }

    /**
     * get noise volume at t
     *
     * @param t time in seconds
     * @return noise volume as float 0-1
     */
    public float getNoise(float t) {
        return noise.get(t);
    }

    /**
     * get the title
     *
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * change the title
     *
     * @param title new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * get the author
     *
     * @return author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * change the author
     *
     * @param author new author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * get the description
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * change the description
     *
     * @param description new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * ouptuts the Preset as an XML element. Example output:<br>
     * &lt;Preset length='60.0' loop='40.0'&gt;<br>
     * &lt;PresetInfos&gt;<br>
     * &lt;Title&gt;Title, escaped&lt;/Title&gt;<br>
     * &lt;Author&gt;Author, escaped&lt;/Author&gt;<br>
     * &lt;Description&gt;Description, escaped&lt;/Description&gt;<br>
     * &lt;/PresetInfos&gt;<br>
     * ...NOISE ENVELOPE, SEE CLASS ENVELOPE FOR EXAMPLE...<br>
     * ...LIST OF 1+ ENTRAINEMNTTRACKS, SEE CLASS ENTRAINMENTTRACK FOR
     * EXAMPLE...<br>
     * &lt;/Preset&gt;
     *
     * @return xml
     */
    public Element toXML() {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("Preset");
            doc.appendChild(root);
            root.setAttribute("length", "" + length);
            if (loop != -1) {
                root.setAttribute("loop", "" + loop);
            }
            Element pInfos = doc.createElement("PresetInfos");
            root.appendChild(pInfos);
            Element pTitle = doc.createElement("Title");
            pInfos.appendChild(pTitle);
            pTitle.appendChild(doc.createTextNode(title));
            Element pAuthor = doc.createElement("Author");
            pInfos.appendChild(pAuthor);
            pAuthor.appendChild(doc.createTextNode(author));
            Element pDesc = doc.createElement("Description");
            pInfos.appendChild(pDesc);
            pDesc.appendChild(doc.createTextNode(description));
            root.appendChild(doc.adoptNode(noise.toXML()));
            for (EntrainmentTrack e : ent) {
                root.appendChild(doc.adoptNode(e.toXML()));
            }
            return root;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * returns the preset as an XML string
     *
     * @return xml
     */
    @Override
    public String toString() {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.transform(new DOMSource(toXML()), new StreamResult(baos));
            return new String(baos.toByteArray());
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Preset)) {
            return false;
        }
        Preset p = (Preset) o;
        if (p.ent.length != ent.length) {
            return false;
        }
        for (int i = 0; i < ent.length; i++) {
            if (!(ent[i].equals(p.ent[i]))) {
                return false;
            }
        }
        if (!p.getNoiseEnvelope().equals(noise)) {
            return false;
        }
        return title.equals(p.title) && description.equals(p.description) && author.equals(p.author);
    }

    /**
     * removes (most) useless points from the envelopes. a point is useless if
     * it has the same value as the point before and the point after it. values
     * are considered identical if their difference is &lt;tolerance
     *
     * @param tolerance 2 points are considered identical if their difference is
     * &lt;tolerance
     * @return number of points removed
     */
    public int optimizePoints(float tolerance) {
        int removedPoints = 0;
        for (EntrainmentTrack e : ent) {
            removedPoints += e.optimize(tolerance);
        }
        removedPoints += noise.optimize(tolerance);
        return removedPoints;
    }

    /**
     * removes useless tracks. a track is considered useless if it is identical
     * to another one in the Preset, or if it's constantly at
     * volume/frequency/base frequency 0.
     *
     * @return number of tracks removed
     */
    public int removeUselessTracks() {
        int removedTracks = 0;
        for (int i = 1; i < ent.length; i++) {
            for (int j = 0; j < ent.length; j++) {
                if ((i != j && ent[i].equals(ent[j])) || ent[i].isUseless()) {
                    removeEntrainmentTrack(i--);
                    removedTracks++;
                    break;
                }
            }
        }
        return removedTracks;
    }

    /**
     * complexity depends mostly on the density of points in an a short amount
     * of time, so here's how it's calculated: the preset is split into
     * (length/60)+1 parts, each point of each envelope is added to the (t/60)th
     * part and the 2 parts before and after it. the complexity is calculated as
     * the number of points in the part with the most points multiplied by 15,
     * +10*entrainmentTrackCount, +5, +15 if the preset loops (because it causes
     * cache invalidation)
     *
     * @return complexity
     */
    public int complexity() {
        int complexity = 0;
        int[] nPoints = new int[(int) (getLength() / 60) + 1];
        Envelope noise = getNoiseEnvelope();
        for (int i = 0; i < noise.getPointCount(); i++) {
            int n = (int) noise.getT(i) / 60;
            if (n > 0) {
                nPoints[n - 1]++;
            }
            nPoints[n]++;
            if (n < nPoints.length - 1) {
                nPoints[n + 1]++;
            }
        }
        complexity += 5;
        for (int e = 0; e < getEntrainmentTrackCount(); e++) {
            EntrainmentTrack et = getEntrainmentTrack(e);
            Envelope vol = et.getVolumeEnvelope(), baseF = et.getBaseFrequencyEnvelope(), ent = et.getEntrainmentFrequencyEnvelope();
            for (int i = 0; i < baseF.getPointCount(); i++) {
                int n = (int) baseF.getT(i) / 60;
                if (n > 0) {
                    nPoints[n - 1]++;
                }
                nPoints[n]++;
                if (n < nPoints.length - 1) {
                    nPoints[n + 1]++;
                }
            }
            for (int i = 0; i < ent.getPointCount(); i++) {
                int n = (int) ent.getT(i) / 60;
                if (n > 0) {
                    nPoints[n - 1]++;
                }
                nPoints[n]++;
                if (n < nPoints.length - 1) {
                    nPoints[n + 1]++;
                }
            }
            for (int i = 0; i < vol.getPointCount(); i++) {
                int n = (int) vol.getT(i) / 60;
                if (n > 0) {
                    nPoints[n - 1]++;
                }
                nPoints[n]++;
                if (n < nPoints.length - 1) {
                    nPoints[n + 1]++;
                }
            }
            complexity += 10;
        }
        int maxDensity = -1;
        for (int i = 0; i < nPoints.length; i++) {
            if (nPoints[i] > maxDensity) {
                maxDensity = nPoints[i];
            }
        }
        if (loops()) {
            complexity += 15;
        }
        complexity += maxDensity * 15;
        return complexity;
    }

    public Preset() {
    }

    @Override
    public Preset clone() {
        Preset x = new Preset(length, loop, title, author, description);
        x.ent = new EntrainmentTrack[ent.length];
        for (int i = 0; i < ent.length; i++) {
            x.ent[i] = ent[i].clone();
        }
        x.noise = noise.clone();
        return x;
    }
}

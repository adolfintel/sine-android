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
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * An envelope of Points.<br>
 * Time unit is SECOND<br> Time goes from 0 to unlimited (32 bit float
 * precision)
 *
 * @author dosse
 */
public class Envelope implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String name;
    /**
     * the list of points in the envelope will be stored here. notice that I
     * used an array instead of ArrayList or other such classes (which makes the
     * whole thing harder to manage and read) because I want it to be as fast as
     * possible p is initialized with a single Point in it, with t=0 and val=0
     */
    private Point[] p = new Point[]{new Point(0, 0)};

    private float length = 0; //length in seconds

    /**
     * creates a new envelope. it initially has 1 point with t=0 and val=0
     *
     * @param name name of the envelope
     * @param length initial length (can be changed later)
     */
    public Envelope(String name, float length) {
        this.length = length < 0 ? 0 : length;
        this.name = name;
    }

    /**
     * creates an Envelope from a node in an XML document
     *
     * @param xmlNode the Envelope node
     * @throws Exception if something goes wrong while parsing the XML
     */
    public Envelope(Element xmlNode) throws Exception {
        try {
            length = Float.parseFloat(xmlNode.getAttribute("length"));
            if (length < 0) {
                throw new Exception(); //invalid length
            }
            name = xmlNode.getAttribute("name");
            //read a list of points. note that they MUST be Points, you can't put anything else as a child of an Envelope node in the XML file
            NodeList points = xmlNode.getChildNodes();
            if (points.getLength() < 1) { //there must be at least 1 Point
                throw new Exception();
            }
            p = new Point[points.getLength()];
            for (int i = 0; i < points.getLength(); i++) { //parse all the Points
                Element n = (Element) points.item(i);
                Point x = new Point(n);
                if (x.t < 0 || x.t > length) { //invalid time for the Point x
                    throw new Exception();
                }
                p[i] = x;
            }
            Arrays.sort(p); //sort the array, just in case it isn't sorted
            if (p[0].t != 0) {
                throw new Exception();
            }
        } catch (Throwable t) {
            throw new Exception();
        }
    }

    /**
     * adds a point to the Envelope<br>
     *
     * @param t time (if &lt;0, it will be changed to 0; if &gt;length, it will
     * be changed to length)
     * @param val value
     */
    public void addPoint(float t, float val) {
        cache_lastX = -1;
        if (t < 0) {
            t = 0;
        }
        if (t > length) {
            t = length;
        }
        Point toAdd = new Point(t, val);
        Point[] newP = new Point[p.length + 1];
        System.arraycopy(p, 0, newP, 0, p.length);
        newP[newP.length - 1] = toAdd;
        Arrays.sort(newP);
        p = newP;
    }

    /**
     *
     * @return length of the Envelope in seconds
     */
    public float getLength() {
        return length;
    }

    /**
     * changes the Envelope's length.<br>
     * If the new length is shorter, some Points may be lost.<br>
     *
     * @param newLength new length (if &lt;0, it will be changed to 0)
     */
    protected void setLength(float newLength) {
        cache_lastX = -1; //invalidate cache
        if (newLength < 0) {
            newLength = 0;
        }
        if (newLength == length) { //nothing to change
            return;
        }
        if (newLength < length && p.length >= 2) { //must reduce length
            ArrayList<Point> toPreserve = new ArrayList<Point>();
            for (Point x : p) {
                if (x.t <= newLength) {
                    toPreserve.add(x);
                } else {
                    break;
                }
            }
            if (toPreserve.get(toPreserve.size() - 1).t != newLength) { //we "cut a line in half", must add a new Point
                toPreserve.add(new Point(newLength, get(newLength)));
            }
            //update list of Points
            Point[] newP = new Point[toPreserve.size()];
            for (int i = 0; i < toPreserve.size(); i++) {
                newP[i] = toPreserve.get(i);
            }
            p = newP;
        }
        length = newLength;
    }

    /**
     * removes a point from the Envelope.<br>
     * Note: Point 0 cannot be removed.
     *
     * @param i Point id. If &lt;=0 or &gt;=number of points, the method does
     * nothing
     */
    public void removePoint(int i) {
        cache_lastX = -1;
        if (i <= 0 || i >= p.length) {
            return;
        }
        //copy all the Points except the one at position i
        Point[] newP = new Point[p.length - 1];
        for (int j = 0, idx = 0; j < p.length; j++) {
            if (j != i) {
                newP[idx++] = p[j];
            }
        }
        p = newP;
    }

    private int cache_lastX = -1; //cache: keeps the index of the last Point found with the method get with time<=t (improves performance)

    /**
     * returns the value of the envelope at time t. does linear interpolation.
     *
     * @param t time
     * @return value at t
     */
    public float get(float t) {
        if (p.length == 1) { //only 1 Point, return its value
            return p[0].val;
        }
        if (t <= 0) { //before beginning of the Envelope
            return p[0].val;
        }
        if (t >= p[p.length - 1].t) { //after the end of the Envelope
            return p[p.length - 1].val;
        }
        int x = -1; //keeps the index of the Point with the lowest time<=t
        int cacheCopy = cache_lastX; //copied for partial thread safety
        if (cacheCopy != -1 && p[cacheCopy].t <= t && p[cacheCopy + 1].t >= t) { //cache hit!
            x = cacheCopy;
        } else { //cache miss, must search :(
            for (int i = 0; i < p.length - 1; i++) {
                if (p[i].t <= t && p[i + 1].t >= t) {
                    x = i;
                    break;
                }
            }
            cache_lastX = x; //store it in the cache for later
        }
        if (t == p[x].t || p[x].val == p[x + 1].val || p[x].t == p[x + 1].t) { //no interpolation needed
            return p[x].val;
        }
        //linear interpolation
        float f = (t - p[x].t) / (p[x + 1].t - p[x].t);
        return p[x + 1].val * f + p[x].val * (1 - f);
    }

    /**
     *
     * @return the number of Points in the Envelope
     */
    public int getPointCount() {
        return p.length;
    }

    /**
     * sets the value of one of the Points. does nothing if i&lt;0 or
     * i&gt;=number of Points
     *
     * @param i Point id
     * @param val new value
     */
    public void setVal(int i, float val) {
        if (i < 0 || i >= p.length) {
            return;
        }
        p[i].val = val;
    }

    /**
     * sets the time of one of the Points. does nothing if i&lt;=0 or
     * i&gt;=number of Points
     *
     * @param i Point id
     * @param t new time (if it's &lt;0 it will be changed to 0; if it's
     * &gt;length, it will be changed to length)
     */
    public void setT(int i, float t) {
        cache_lastX = -1; //invalidate cache
        if (i <= 0 || i >= p.length) {
            return;
        }
        if (t < 0) {
            t = 0;
        }
        if (t > length) {
            t = length;
        }
        p[i].t = t;
        Arrays.sort(p); //sort the array if necessary
    }

    /**
     * gets the value of a specified Point in the Envelope
     *
     * @param i Point id. if &lt;0, returns the value of the Point 0; if
     * &gt;number of Points, returns the value of the last point
     * @return value
     */
    public float getVal(int i) {
        i = i < 0 ? 0 : i >= p.length ? p.length - 1 : i;
        return p[i].val;
    }

    /**
     * gets the time of a specified Point in the Envelope
     *
     * @param i Point id. if &lt;0, returns the value of the Point 0; if
     * &gt;number of Points, returns the value of the last point
     * @return time
     */
    public float getT(int i) {
        i = i < 0 ? 0 : i >= p.length ? p.length - 1 : i;
        return p[i].t;
    }

    /**
     * outputs the Envelope as an XML element<br>
     * Example:<br>
     * &lt;Envelope name='test' length='60.0'&gt;<br>
     * ... LIST OF POINTS, SEE CLASS POINT FOR EXAMPLE ...<br>
     * &lt;/Envelope&gt;
     *
     * @return xml
     */
    public Element toXML() {
        try {
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element env = d.createElement("Envelope");
            d.appendChild(env);
            env.setAttribute("name", name);
            env.setAttribute("length", "" + length);
            for (Point x : p) {
                env.appendChild(d.adoptNode(x.toXML()));
            }
            return env;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Envelope)) {
            return false;
        }
        Envelope e = (Envelope) o;
        if (e.p.length != p.length) {
            return false;
        }
        for (int i = 0; i < p.length; i++) {
            if (!(p[i].equals(e.p[i]))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 83 * hash + Arrays.deepHashCode(this.p);
        hash = 83 * hash + Float.floatToIntBits(this.length);
        return hash;
    }

    /**
     * removes (most) useless points from the envelope. a point is useless if it
     * has the same value as the point before and the point after it. values are
     * considered identical if their difference is &lt;tolerance
     *
     * @param tolerance 2 points are considered identical if their difference is
     * &lt;tolerance
     * @return number of points removed
     */
    public int optimize(float tolerance) {
        int removedPoints = 0;
        for (int i = 1; i < p.length - 1; i++) {
            float v0 = p[i - 1].getVal(), v1 = p[i].getVal(), v2 = p[i + 1].getVal();
            if (Math.abs(v0 - v1) < tolerance && Math.abs(v1 - v2) < tolerance) { //this point is useless and can be removed
                removePoint(i--);
                removedPoints++;
            }
        }
        if (p.length >= 2) {
            if (Math.abs(p[p.length - 1].getVal() - p[p.length - 2].getVal()) < tolerance) {
                removePoint(p.length - 1);
                removedPoints++;
            }
        }
        return removedPoints;
    }

    @Override
    public Envelope clone() {
        Envelope e = new Envelope(name, length);
        e.p = new Point[0];
        for (Point x : p) {
            e.addPoint(x.t, x.val);
        }
        return e;
    }

}

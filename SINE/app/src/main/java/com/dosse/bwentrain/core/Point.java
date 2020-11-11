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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A point in an Envelope.<br>
 * Has a time (t) and a value (val), both floats.
 *
 * @author dosse
 */
public class Point implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;
    /**
     * t= time, val=value at that time.<br>
     * Protected instead of private so they can be quickly accessed from other
     * classes in the core package
     */
    protected float t, val;

    /**
     *
     * @param t time
     * @param val value at said time
     */
    public Point(float t, float val) {
        this.t = t;
        this.val = val;
    }

    /**
     * creates a Point from a node in an XML document
     *
     * @param xmlNode the Envelope node
     * @throws Exception if something goes wrong while parsing the XML
     */
    public Point(Element xmlNode) throws Exception {
        try {
            t = Float.parseFloat(xmlNode.getAttribute("time"));
            val = Float.parseFloat(xmlNode.getAttribute("value"));
            if (t < 0) {
                throw new Exception();
            }
        } catch (Throwable t) {
            throw new Exception();
        }
    }

    /**
     *
     * @return time
     */
    public float getT() {
        return t;
    }

    /**
     *
     * @return value at t
     */
    public float getVal() {
        return val;
    }

    /**
     * outputs the Point as an XML element<br>
     * Example output: &lt;Point time=&lt;10.0&lt; value=&lt;1.0&lt; /&gt;
     *
     * @return xml
     */
    public Element toXML(){
        try {
            Document d=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element p=d.createElement("Point");
            d.appendChild(p);
            p.setAttribute("time", ""+t);
            p.setAttribute("value", ""+val);
            return p;
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * compares the Point to another Object.<br>
     * Used for automatic sorting
     *
     * @param o the other Object
     * @return 0 if the Object isn't a Point, 1 if it should come after, -1 if
     * it should come before
     */
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof Point)) {
            return 0;
        }
        float d = ((Point) o).t - t;
        return d == 0 ? 0 : d > 0 ? -1 : 1;
    }
    
    @Override
    public boolean equals(Object o){
        if(!(o instanceof Point)) return false;
        Point p=(Point)o;
        return p.t==t&&p.val==val;
    }
    
    @Override
    public Point clone(){
        return new Point(t, val);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Float.floatToIntBits(this.t);
        hash = 11 * hash + Float.floatToIntBits(this.val);
        return hash;
    }

}

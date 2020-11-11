package com.dosse.bwentrain.androidPlayer;

public class Utils {
	//converts float (in seconds) to a String HH:MM:SS
	public static final String toHMS(float t) {
        int h = (int) (t / 3600);
        t %= 3600;
        int m = (int) (t / 60);
        t %= 60;
        int s = (int) t;
        return "" + (h < 10 ? ("0" + h) : h) + ":" + (m < 10 ? ("0" + m) : m) + ":" + (s < 10 ? ("0" + s) : s); //bloody hell, code salad
    }
}

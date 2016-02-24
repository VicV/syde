package com.jarone.litterary.datatypes;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by V on 2/23/2016.
 * <p/>
 * Model for a message from the debugger.
 */
public class DebugItem {

    public enum DebugLevel {
        DEBUG, WARN, ERROR
    }

    public DebugItem(DebugLevel debugLevel, String text, long time) {
        this.debugLevel = debugLevel;
        this.text = text;
        this.dateText = new SimpleDateFormat("hh:mm:ss.SSS").format(new Date(time));
    }

    public DebugLevel getDebugLevel() {
        return debugLevel;
    }

    public void setDebugLevel(DebugLevel debugLevel) {
        this.debugLevel = debugLevel;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    private DebugLevel debugLevel;
    private String text;

    public String getDateString() {
        return dateText;
    }

    private String dateText;

}

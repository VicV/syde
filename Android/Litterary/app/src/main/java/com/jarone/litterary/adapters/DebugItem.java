package com.jarone.litterary.adapters;

/**
 * Created by V on 2/23/2016.
 */
public class DebugItem {

    public enum DebugLevel {
        DEBUG, WARN, ERROR
    }

    public DebugItem(DebugLevel debugLevel, String text) {
        this.debugLevel = debugLevel;
        this.text = text;
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
    private long date;

}

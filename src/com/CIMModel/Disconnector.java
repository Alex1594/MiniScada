package com.CIMModel;

public class Disconnector extends Switch{
    private Terminal terminal0;
    private Terminal terminal1;
    private String normalOpen;

    public Disconnector(String id) {
        super(id);
        this.terminal0 = null;
        this.terminal1 = null;
    }

    public Terminal getTerminal0() {
        return terminal0;
    }

    public void setTerminal0(Terminal terminal0) {
        this.terminal0 = terminal0;
    }

    public Terminal getTerminal1() {
        return terminal1;
    }

    public void setTerminal1(Terminal terminal1) {
        this.terminal1 = terminal1;
    }

    public String getNormalOpen() {
        return normalOpen;
    }

    public void setNormalOpen(String normalOpen) {
        this.normalOpen = normalOpen;
    }
}

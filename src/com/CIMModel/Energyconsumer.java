package com.CIMModel;

public class Energyconsumer extends ConductingEquipment{
    private Terminal terminal0;

    protected String fixed ;

    protected String fexp;

    public Energyconsumer(String id) {
        super(id);
        this.terminal0 = null;
    }

    public Terminal getTerminal0() {
        return terminal0;
    }

    public void setTerminal0(Terminal terminal0) {
        this.terminal0 = terminal0;
    }

    public String getPfixed() {
        return fixed;
    }

    public String getQfixed() {
        return fexp;
    }
}

package com.bloomreach.trafficgenerator.visitor;

// Signal object used by visitorCreator thread to notify site listener thread
public class VisitorSignal {

    private Visitor visitor;

    public VisitorSignal () {
    }

    protected void setVisitor (Visitor visitor) {
        this.visitor = visitor;
    }

    public Visitor getVisitor () {
        return this.visitor;
    }
}

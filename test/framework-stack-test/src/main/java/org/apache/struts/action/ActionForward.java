package org.apache.struts.action;

public class ActionForward {
    private final String path;

    public ActionForward(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}

package ysoserial.payloads.util;

import ysoserial.secmgr.ExecSink;

public class GetterBean {
    public String getOutputProperties() {
        return ExecSink.exec("rome-getter");
    }
}

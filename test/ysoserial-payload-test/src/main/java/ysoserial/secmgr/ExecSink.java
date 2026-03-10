package ysoserial.secmgr;

public final class ExecSink {
    private ExecSink() {
    }

    public static String exec(String command) {
        return command == null ? "exec:null" : "exec:" + command;
    }
}

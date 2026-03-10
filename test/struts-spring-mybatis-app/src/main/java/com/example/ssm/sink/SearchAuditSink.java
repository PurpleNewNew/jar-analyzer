package com.example.ssm.sink;

public class SearchAuditSink {
    public void record(String keyword) {
        if (keyword == null) {
            return;
        }
        keyword.length();
    }
}

package com.monitorSQLInMethod;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class MonitorSQLInMethodFactor {

    private OffsetDateTime invokeTime;

    private String thread;

    private String method;

    private long invokeDuration;

    private Deque<SqlInfo> sqlInfoList = new LinkedList<>();

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public long getInvokeDuration() {
        return invokeDuration;
    }

    public void setInvokeDuration(long invokeDuration) {
        this.invokeDuration = invokeDuration;
    }

    public OffsetDateTime getInvokeTime() {
        return invokeTime;
    }

    public void setInvokeTime(OffsetDateTime invokeTime) {
        this.invokeTime = invokeTime;
    }

    public Deque<SqlInfo> getSqlInfoList() {
        return sqlInfoList;
    }

    public void setSqlInfoList(Deque<SqlInfo> sqlInfoList) {
        this.sqlInfoList = sqlInfoList;
    }

    public static class SqlInfo{

        private String sql;
        private long millSeconds;
        private OffsetDateTime executeTime;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public long getMillSeconds() {
            return millSeconds;
        }

        public void setMillSeconds(long millSeconds) {
            this.millSeconds = millSeconds;
        }

        public OffsetDateTime getExecuteTime() {
            return executeTime;
        }

        public void setExecuteTime(OffsetDateTime executeTime) {
            this.executeTime = executeTime;
        }
    }
}

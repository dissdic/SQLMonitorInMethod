package com.monitorSQLInMethod;

import org.springframework.context.ApplicationEvent;

public class MonitorSQLInMethodEvent extends ApplicationEvent {

    public MonitorSQLInMethodEvent(MonitorSQLInMethodFactor factor) {
        super(factor);
    }
}

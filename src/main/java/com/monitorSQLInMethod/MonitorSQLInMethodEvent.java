package com.monitorSQLInMethod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationEvent;

public class MonitorSQLInMethodEvent extends ApplicationEvent {

    public MonitorSQLInMethodEvent(MonitorSQLInMethodFactor factor) {
        super(factor);
    }
}

package com.bszn.mq;

public enum MQEnum {

    MONITOR_CMD("direct.exchange.monitor.cmd",
            MQConstants.MONITOR_CMD,
            "direct.routingKey.monitor.cmd");


    private String exchange;
    private String name;
    private String routeKey;

    private MQEnum(String exchange, String name, String routeKey) {
        this.exchange = exchange;
        this.name = name;
        this.routeKey = routeKey;
    }

    public String getExchange() {
        return this.exchange;
    }

    public String getName() {
        return this.name;
    }

    public String getRouteKey() {
        return this.routeKey;
    }
}

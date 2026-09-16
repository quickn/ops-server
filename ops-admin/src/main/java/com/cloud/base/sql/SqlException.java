package com.cloud.base.sql;

/**
 * 封装的业务类异常，业务有问题抛出该异常
 */
public class SqlException extends RuntimeException {

    public SqlException() {
        super();
    }

    public SqlException(String message) {
        super(message);
    }

    public SqlException(String title, String message) {
        super(title + " " + message);
    }

    public SqlException(Integer code, String message) {
        super(message);
    }

    public SqlException(String message, Throwable e) {
        super(message, e);
    }

    public SqlException(Throwable e) {
        super(e);
    }

}

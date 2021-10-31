package dev.keva.ioc.exception;

public class IoCCircularDepException extends Exception {
    public IoCCircularDepException(String message) {
        super(message);
    }
}

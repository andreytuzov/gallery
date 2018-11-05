package ru.railway.dc.routes.utils;

public class TryMe implements Thread.UncaughtExceptionHandler {

    Thread.UncaughtExceptionHandler oldHandler;

    public TryMe() {
        oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        System.err.println("exception: " + ex);
        if (oldHandler != null) {
            oldHandler.uncaughtException(thread, ex);
        }
    }
}

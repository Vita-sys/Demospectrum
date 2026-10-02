package ru.miet.demospectrum.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ErrMsgLog {

    private static List<Exception> errList = new ArrayList<>();
    private static Logger log;

    public ErrMsgLog() throws IOException {
        LogManager.getLogManager().readConfiguration(
                ErrMsgLog.class.getResourceAsStream("/logging.properties"));
        log = Logger.getLogger(ErrMsgLog.class.getName());
    }

    public int addErr(Exception e) {
        errList.add(e);
        return errList.size();
    }

    public int addErrWithLog(Exception e) {
        errList.add(e);
        if (log != null) {
            log.log(Level.FINE, e.getMessage(), e);
        }
        return errList.size();
    }

    public int getErrCount() {
        return errList.size();
    }

    public List<Exception> getErrList() {
        return errList;
    }

    public void showErrText(Exception e) {
        System.err.println("Ошибка: " + e.getMessage());
    }

    public Exception makeErr(Exception e) {
        addErr(e);
        return new Exception(e);
    }

    public static void main(String[] args) throws IOException {
        ErrMsgLog logger = new ErrMsgLog();

        System.out.println("Ошибок до: " + logger.getErrCount());

        logger.addErrWithLog(new Exception("Тестовая ошибка №1"));
        logger.addErrWithLog(new Exception("Тестовая ошибка №2"));

        System.out.println("Ошибок после: " + logger.getErrCount());
    }

}
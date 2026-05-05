package org.example;

import org.drinkless.tdlib.Client;
import org.example.telegram.TelegramService;

public class Main {
    public static void main(String[] args) throws Client.ExecutionException, InterruptedException {
        TelegramService service =
                new TelegramService();

        service.init();

        // GIỮ APP KHÔNG THOÁT
        Thread.currentThread().join();
    }
}
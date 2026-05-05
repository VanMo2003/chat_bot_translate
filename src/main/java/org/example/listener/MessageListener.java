package org.example.listener;

public interface MessageListener {

    void onMessage(
            long chatId,
            String sender,
            String message
    );
}

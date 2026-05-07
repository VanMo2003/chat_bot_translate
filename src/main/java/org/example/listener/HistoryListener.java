package org.example.listener;

public interface HistoryListener {

    void onHistoryMessage(
            long chatId,
            String sender,
            String message
    );
}

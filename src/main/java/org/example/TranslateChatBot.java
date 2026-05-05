package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

public class TranslateChatBot extends TelegramLongPollingBot {

    private final Map<Long, String> userLanguages = new HashMap<>();

    @Override
    public String getBotUsername() {
        return "YOUR_BOT_NAME";
    }

    @Override
    public String getBotToken() {
        return "YOUR_BOT_TOKEN";
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage()) return;

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        try {

            // set language
            if (text.startsWith("/lang")) {

                String lang = text.split(" ")[1];

                userLanguages.put(chatId, lang);

                send(chatId, "Saved language: " + lang);
                return;
            }

            String lang = userLanguages.getOrDefault(chatId, "en");

            // fake translate
            String translated = "[Translated to " + lang + "] " + text;

            send(chatId, translated);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(Long chatId, String text) throws Exception {

        SendMessage message = new SendMessage();

        message.setChatId(chatId.toString());
        message.setText(text);

        execute(message);
    }
}
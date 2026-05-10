package org.example.model;

public class Customer {

    private long chatId;

    private String name;

    // en, zh, ja, ko...
    private String languageCode;

    public Customer() {}

    public Customer(
            long chatId,
            String name,
            String languageCode
    ) {
        this.chatId = chatId;
        this.name = name;
        this.languageCode = languageCode;
    }

    public long getChatId() {
        return chatId;
    }

    public String getName() {
        return name;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    @Override
    public String toString() {
        return name + " (" + languageCode + ")";
    }
}

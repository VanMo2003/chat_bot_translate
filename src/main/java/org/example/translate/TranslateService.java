package org.example.translate;

public class TranslateService {

    public String translate(
            String text,
            String targetLanguage
    ) {

        // TODO: OpenAI translate

        return "["
                + targetLanguage
                + "] "
                + text;
    }
}

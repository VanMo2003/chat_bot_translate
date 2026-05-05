package org.example.openai;

import okhttp3.*;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class OpenAIService {

    private final OkHttpClient client =
            new OkHttpClient();

    public String translateText(String text, String language) {

        RequestBody formBody = new FormBody.Builder()
                .add("q", text)
                .add("source", "auto")
                .add("target", getLanguageCode(language))
                .add("format", "text")
                .build();

        Request request = new Request.Builder()
                .url("https://libretranslate.com/translate")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (response.body() == null) {
                return text;
            }

            String responseBody = response.body().string();

            System.out.println("TRANSLATE RESPONSE => " + responseBody);

            if (!response.isSuccessful()) {
                return text;
            }

            JSONObject json = new JSONObject(responseBody);
            return json.getString("translatedText");

        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    private String getLanguageCode(
            String language
    ) {

        return switch (language) {

            case "English" -> "en";

            case "Japanese" -> "ja";

            case "Korean" -> "ko";

            case "Chinese" -> "zh";

            case "Thai" -> "th";

            case "French" -> "fr";

            case "German" -> "de";

            case "Vietnamese" -> "vi";

            default -> "en";
        };
    }
}
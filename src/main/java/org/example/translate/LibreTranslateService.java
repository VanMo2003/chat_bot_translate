package org.example.translate;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class LibreTranslateService {

    private final OkHttpClient client = new OkHttpClient();

    public Map<String, String> getSupportedLanguages() {

        Map<String, String> languages = new LinkedHashMap<>();

        try {

            Request request = new Request.Builder()
                    .url("http://localhost:5000/languages")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    return languages;
                }

                String responseBody = response.body().string();

                System.out.println("LANGUAGES => " + responseBody);

                JSONArray array = new JSONArray(responseBody);

                for (int i = 0; i < array.length(); i++) {

                    JSONObject obj = array.getJSONObject(i);

                    String code = obj.getString("code");
                    String name = obj.getString("name");

                    languages.put(name, code);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return languages;
    }

    public String translateText(String text, String language) {

        try {

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("q", text);
            jsonBody.put("source", "vi");
            jsonBody.put("target", getLanguageCode(language));

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("http://localhost:5000/translate")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    return text;
                }

                String responseBody = response.body().string();

                System.out.println("TRANSLATE RESPONSE => " + responseBody);

                JSONObject json = new JSONObject(responseBody);

                return json.optString("translatedText", text);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    private String getLanguageCode(String language) {

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
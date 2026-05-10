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
                    .url("https://divisions-embassy-way-gentle.trycloudflare.com//languages")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return languages;
                }
                String responseBody = response.body().string();
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

    // Thêm tham số source để hỗ trợ tự động phát hiện ngôn ngữ khi dịch sang Tiếng Việt
    public String translateText(String text, String source, String target) {
        try {
            String normalized = hardCode(target, text.trim().toLowerCase());
            if (!normalized.isEmpty()) {
                return normalized;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("q", text);
            jsonBody.put("source", source); // "vi" hoặc "auto"
            jsonBody.put("target", target);
            jsonBody.put("format", "text");

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://divisions-embassy-way-gentle.trycloudflare.com//translate")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return text;
                }
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                return json.optString("translatedText", text);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }

    String hardCode(String language, String normalized) {
        if (language.equals("ja")) {
            switch (normalized) {
                case "xin chào":
                    return "こんにちは";
                case "cảm ơn":
                    return "ありがとうございます";
                case "tạm biệt":
                    return "さようなら";
            }
        }
        return "";
    }
}
package org.example.translate;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class LibreTranslateService {

    private final OkHttpClient client = new OkHttpClient();
    private String URL_BASE = "https://divisions-embassy-way-gentle.trycloudflare.com";

    public Map<String, String> getSupportedLanguages() {
        System.out.println("[Translate] Đang lấy danh sách ngôn ngữ hỗ trợ...");
        Map<String, String> languages = new LinkedHashMap<>();
        try {
            Request request = new Request.Builder()
                    .url(URL_BASE + "//languages")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("[Translate] Lỗi khi lấy danh sách ngôn ngữ. HTTP Code: " + response.code());
                    return languages;
                }
                String responseBody = response.body().string();
                System.out.println("[Translate] Dữ liệu ngôn ngữ thô: " + responseBody);
                JSONArray array = new JSONArray(responseBody);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String code = obj.getString("code");
                    String name = obj.getString("name");
                    languages.put(name, code);
                }
                System.out.println("[Translate] Đã tải thành công " + languages.size() + " ngôn ngữ.");
            }
        } catch (Exception e) {
            System.err.println("[Translate] Exception khi lấy danh sách ngôn ngữ:");
            e.printStackTrace();
        }
        return languages;
    }

    public String translateText(String text, String source, String target) {
        System.out.println("[Translate] Bắt đầu dịch: [" + source + " -> " + target + "] Text: '" + text + "'");
        try {
            String normalized = hardCode(target, text.trim().toLowerCase());
            if (!normalized.isEmpty()) {
                System.out.println("[Translate] Sử dụng kết quả hardcode: " + normalized);
                return normalized;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("q", text);
            jsonBody.put("source", source);
            jsonBody.put("target", target);
            jsonBody.put("format", "text");

            System.out.println("[Translate] Request Payload: " + jsonBody.toString());

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_BASE + "//translate")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("[Translate] Lỗi API dịch. HTTP Code: " + response.code());
                    return text;
                }
                String responseBody = response.body().string();
                System.out.println("[Translate] Response Body: " + responseBody);
                JSONObject json = new JSONObject(responseBody);
                String result = json.optString("translatedText", text);
                System.out.println("[Translate] Kết quả dịch: " + result);
                return result;
            }

        } catch (Exception e) {
            System.err.println("[Translate] Exception khi gọi API dịch:");
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
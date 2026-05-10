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
                    String englishName = obj.getString("name");

                    // ĐÃ SỬA: Chuyển đổi tên ngôn ngữ sang Tiếng Việt
                    String viName = getVietnameseName(englishName);

                    languages.put(viName, code);
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

    // ĐÃ THÊM: Bộ từ điển map tên ngôn ngữ sang Tiếng Việt
    private String getVietnameseName(String englishName) {
        switch (englishName.toLowerCase().trim()) {
            case "english": return "Tiếng Anh";
            case "vietnamese": return "Tiếng Việt";
            case "chinese": return "Tiếng Trung";
            case "chinese (traditional)": return "Tiếng Trung (Phồn thể)";
            case "japanese": return "Tiếng Nhật";
            case "korean": return "Tiếng Hàn";
            case "french": return "Tiếng Pháp";
            case "spanish": return "Tiếng Tây Ban Nha";
            case "russian": return "Tiếng Nga";
            case "german": return "Tiếng Đức";
            case "italian": return "Tiếng Ý";
            case "portuguese": return "Tiếng Bồ Đào Nha";
            case "arabic": return "Tiếng Ả Rập";
            case "hindi": return "Tiếng Hindi";
            case "thai": return "Tiếng Thái";
            case "indonesian": return "Tiếng Indonesia";
            case "malay": return "Tiếng Mã Lai";
            case "dutch": return "Tiếng Hà Lan";
            case "turkish": return "Tiếng Thổ Nhĩ Kỳ";
            case "polish": return "Tiếng Ba Lan";
            case "swedish": return "Tiếng Thụy Điển";
            case "ukrainian": return "Tiếng Ukraina";
            case "czech": return "Tiếng Séc";
            case "danish": return "Tiếng Đan Mạch";
            case "finnish": return "Tiếng Phần Lan";
            case "greek": return "Tiếng Hy Lạp";
            case "hungarian": return "Tiếng Hungary";
            case "norwegian": return "Tiếng Na Uy";
            case "romanian": return "Tiếng Romania";
            case "slovak": return "Tiếng Slovakia";
            case "bengali": return "Tiếng Bengal";
            case "persian": return "Tiếng Ba Tư";
            case "hebrew": return "Tiếng Do Thái";
            case "tagalog": return "Tiếng Tagalog";
            case "urdu": return "Tiếng Urdu";
            case "catalan": return "Tiếng Catalan";
            case "croatian": return "Tiếng Croatia";
            case "esperanto": return "Tiếng Esperanto";
            case "estonian": return "Tiếng Estonia";
            case "latvian": return "Tiếng Latvia";
            case "lithuanian": return "Tiếng Litva";
            case "serbian": return "Tiếng Serbia";
            case "slovenian": return "Tiếng Slovenia";
            case "albanian": return "Tiếng Albania";
            case "azerbaijani": return "Tiếng Azerbaijan";
            case "bulgarian": return "Tiếng Bulgaria";
            default: return englishName; // Nếu ngôn ngữ lạ chưa có trong từ điển thì giữ nguyên
        }
    }
}
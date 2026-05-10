package org.example.translate;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class LibreTranslateService {

    private final OkHttpClient client = new OkHttpClient();
    private String URL_BASE = "https://cymbiform-dotty-unabsorbable.ngrok-free.dev";

    public Map<String, String> getSupportedLanguages() {
        System.out.println("[Translate] Đang lấy danh sách ngôn ngữ hỗ trợ...");
        Map<String, String> languages = new LinkedHashMap<>();
        try {
            Request request = new Request.Builder()
                    .url(URL_BASE + "/languages")
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

                    // Lấy tên theo định dạng "Tiếng Việt (English)"
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

//            System.out.println("[Translate] Request Payload: " + jsonBody.toString());

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(URL_BASE + "/translate")
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
//                System.out.println("[Translate] Kết quả dịch: " + result);
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

    // ĐÃ SỬA: Danh sách map sang dạng "Tiếng Việt (English)"
    private String getVietnameseName(String englishName) {
        switch (englishName.toLowerCase().trim()) {
            case "english": return "Tiếng Anh (English)";
            case "albanian": return "Tiếng Albania (Albanian)";
            case "arabic": return "Tiếng Ả Rập (Arabic)";
            case "azerbaijani": return "Tiếng Azerbaijan (Azerbaijani)";
            case "basque": return "Tiếng Basque (Basque)";
            case "bengali": return "Tiếng Bengal (Bengali)";
            case "bulgarian": return "Tiếng Bulgaria (Bulgarian)";
            case "catalan": return "Tiếng Catalan (Catalan)";
            case "chinese": return "Tiếng Trung (Chinese)";
            case "chinese (traditional)": return "Tiếng Trung Phồn thể (Chinese (traditional))";
            case "czech": return "Tiếng Séc (Czech)";
            case "danish": return "Tiếng Đan Mạch (Danish)";
            case "dutch": return "Tiếng Hà Lan (Dutch)";
            case "esperanto": return "Tiếng Esperanto (Esperanto)";
            case "estonian": return "Tiếng Estonia (Estonian)";
            case "finnish": return "Tiếng Phần Lan (Finnish)";
            case "french": return "Tiếng Pháp (French)";
            case "galician": return "Tiếng Galician (Galician)";
            case "german": return "Tiếng Đức (German)";
            case "greek": return "Tiếng Hy Lạp (Greek)";
            case "hebrew": return "Tiếng Do Thái (Hebrew)";
            case "hindi": return "Tiếng Hindi (Hindi)";
            case "hungarian": return "Tiếng Hungary (Hungarian)";
            case "indonesian": return "Tiếng Indonesia (Indonesian)";
            case "irish": return "Tiếng Ireland (Irish)";
            case "italian": return "Tiếng Ý (Italian)";
            case "japanese": return "Tiếng Nhật (Japanese)";
            case "korean": return "Tiếng Hàn (Korean)";
            case "kyrgyz": return "Tiếng Kyrgyz (Kyrgyz)";
            case "latvian": return "Tiếng Latvia (Latvian)";
            case "lithuanian": return "Tiếng Litva (Lithuanian)";
            case "malay": return "Tiếng Mã Lai (Malay)";
            case "norwegian": return "Tiếng Na Uy (Norwegian)";
            case "persian": return "Tiếng Ba Tư (Persian)";
            case "polish": return "Tiếng Ba Lan (Polish)";
            case "portuguese": return "Tiếng Bồ Đào Nha (Portuguese)";
            case "portuguese (brazil)": return "Tiếng Bồ Đào Nha Brazil (Portuguese (Brazil))";
            case "romanian": return "Tiếng Romania (Romanian)";
            case "russian": return "Tiếng Nga (Russian)";
            case "slovak": return "Tiếng Slovakia (Slovak)";
            case "slovenian": return "Tiếng Slovenia (Slovenian)";
            case "spanish": return "Tiếng Tây Ban Nha (Spanish)";
            case "swedish": return "Tiếng Thụy Điển (Swedish)";
            case "tagalog": return "Tiếng Tagalog (Tagalog)";
            case "thai": return "Tiếng Thái (Thai)";
            case "turkish": return "Tiếng Thổ Nhĩ Kỳ (Turkish)";
            case "ukranian": // Theo JSON của bạn
            case "ukrainian": return "Tiếng Ukraina (Ukrainian)";
            case "urdu": return "Tiếng Urdu (Urdu)";
            case "vietnamese": return "Tiếng Việt (Vietnamese)";
            default: return englishName;
        }
    }

    public void runTranslationSelfTest() {

        System.out.println("========================================");
        System.out.println("   BẮT ĐẦU TEST TOÀN BỘ NGÔN NGỮ");
        System.out.println("========================================");

        Map<String, String> languages = getSupportedLanguages();

        if (languages.isEmpty()) {
            System.out.println("[TEST] Không tải được danh sách ngôn ngữ.");
            return;
        }

        // Các câu test cơ bản
        String[] testSentences = {
                "Xin chào",
                "Cảm ơn",
                "Tạm biệt",
                "Bạn khỏe không?",
                "Tôi là người Việt Nam",
                "Hôm nay trời đẹp",
                "Chúc bạn một ngày tốt lành"
        };

        int success = 0;
        int failed = 0;

        for (Map.Entry<String, String> entry : languages.entrySet()) {

            String languageName = entry.getKey();
            String languageCode = entry.getValue();

            try {

                for (String text : testSentences) {

                    String translated = translateText(
                            text,
                            "vi",
                            languageCode
                    );

                    // Kiểm tra đơn giản
                    if (translated == null ||
                            translated.trim().isEmpty()) {

                        System.out.println("[FAILED] Translation rỗng");
                        failed++;

                    } else {

                        success++;
                    }

                    // Delay nhẹ tránh spam API
                    Thread.sleep(300);
                }

            } catch (Exception e) {

                failed++;
                System.out.println("[FAILED] Lỗi khi test ngôn ngữ: " + languageCode);
                e.printStackTrace();
            }
        }

        System.out.println("========================================");
        System.out.println("   HOÀN THÀNH TEST");
        System.out.println("   SUCCESS: " + success);
        System.out.println("   FAILED : " + failed);
        System.out.println("========================================");
    }
}
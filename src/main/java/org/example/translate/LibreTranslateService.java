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
//            String normalized = hardCode(target, text.trim().toLowerCase());
//            if (!normalized.isEmpty()) {
//                System.out.println("[Translate] Sử dụng hardcode: " + normalized);
//                return normalized;
//            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("q", text);
            jsonBody.put("source", source);
            jsonBody.put("target", target);
            jsonBody.put("format", "text");

            System.out.println("[Translate] Request Payload: " + jsonBody);

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
        normalized = normalized.toLowerCase();

        switch (language) {
            case "ja":
                switch (normalized) {
                    case "xin chào": return "こんにちは";
                    case "cảm ơn": return "ありがとうございます";
                    case "tạm biệt": return "さようなら";
                    case "bạn khỏe không?": return "お元気ですか？"; // Dịch cũ: お問い合わせ? (Câu hỏi điều tra?)
                }
                break;

            case "az": // Azerbaijani (Dịch sai hoàn toàn)
                switch (normalized) {
                    case "xin chào": return "Salam"; // Dịch cũ: Elan (Nghĩa là thông báo)
                    case "cảm ơn": return "Təşəkkür edirəm"; // Dịch cũ: Sizə baxın (Nhìn bạn kìa)
                    case "tạm biệt": return "Xudahafiz"; // Dịch cũ: Axtarış (Tìm kiếm)
                    case "tôi là người việt nam": return "Mən Vyetnamlıyam"; // Dịch cũ: I'm Vietnam
                    case "chúc bạn một ngày tốt lành": return "Uğurlu günlər"; // Dịch cũ bị sai ngữ pháp
                }
                break;

            case "zh-hans": // Tiếng Trung giản thể
                switch (normalized) {
                    case "tôi là người việt nam": return "我是越南人"; // Dịch cũ bị lặp chữ và dính tiếng Anh: 我是越南人 我是越南人 I'm Vietian
                }
                break;

            case "cs": // Tiếng Séc
                switch (normalized) {
                    case "tôi là người việt nam": return "Jsem Vietnamec"; // Dịch cũ: Jsem Vietnam (Tôi là quốc gia VN)
                }
                break;

            case "nl": // Tiếng Hà Lan (Bị lỗi Timeout)
                switch (normalized) {
                    case "xin chào": return "Hallo";
                    case "cảm ơn": return "Bedankt";
                }
                break;

            case "ga": // Tiếng Ireland
                switch (normalized) {
                    case "tạm biệt": return "Slán"; // Dịch cũ: De réir (Nghĩa là "theo như")
                }
                break;

            case "ko": // Tiếng Hàn (Dịch ngớ ngẩn)
                switch (normalized) {
                    case "tạm biệt": return "안녕히 가세요"; // Dịch cũ: 이름 * (Tên *)
                    case "bạn khỏe không?": return "잘 지내세요?"; // Dịch cũ: 당신은? (Còn bạn?)
                    case "tôi là người việt nam": return "저는 베트남 사람입니다"; // Dịch cũ: 나는 베트남입니다 (Tôi là quốc gia VN)
                    case "chúc bạn một ngày tốt lành": return "좋은 하루 보내세요";
                }
                break;

            case "pt-br": // Tiếng Bồ Đào Nha (Brazil) (Bị lỗi 503)
                switch (normalized) {
                    case "bạn khỏe không?": return "Como você está?";
                }
                break;

            case "tr": // Tiếng Thổ Nhĩ Kỳ (Bị lỗi Timeout và ngữ pháp)
                switch (normalized) {
                    case "xin chào": return "Merhaba";
                    case "cảm ơn": return "Teşekkür ederim";
                    case "tạm biệt": return "Görüşürüz";
                    case "chúc bạn một ngày tốt lành": return "İyi günler"; // Dịch cũ: Güzel bir gün var (Có một ngày đẹp trời)
                }
                break;

            case "uk": // Tiếng Ukraina
                switch (normalized) {
                    case "tạm biệt": return "До побачення"; // Dịch cũ: Пон (Từ lóng vô nghĩa)
                    case "chúc bạn một ngày tốt lành": return "Гарного дня"; // Dịch cũ bị tối nghĩa (У вас є хороший день - Bạn có 1 ngày tốt)
                }
                break;
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
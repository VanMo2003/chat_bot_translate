package org.example.translate;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            String normalized = hardCode(language, text.trim().toLowerCase());
            if (!normalized.isEmpty()) {
                return  normalized;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("q", text);
            jsonBody.put("source", "vi");
            jsonBody.put("target", language);
            jsonBody.put("format","text");

            System.out.println(
                    "TRANSLATE REQUEST => "
                            + jsonBody
            );

            RequestBody body =
                    RequestBody.create(
                            jsonBody.toString(),
                            MediaType.parse(
                                    "application/json; charset=utf-8"
                            )
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    "https://sherman-hello-hosts-distributor.trycloudflare.com//translate"
                            )
                            .post(body)
                            .addHeader(
                                    "Content-Type",
                                    "application/json"
                            )
                            .build();

            try (Response response =
                         client.newCall(request)
                                 .execute()) {

                if (!response.isSuccessful()) {

                    System.out.println(
                            "TRANSLATE ERROR => "
                                    + response.code()
                    );

                    return text;
                }

                String responseBody =
                        response.body().string();

                System.out.println(
                        "TRANSLATE RESPONSE => "
                                + responseBody
                );

                JSONObject json =
                        new JSONObject(
                                responseBody
                        );

                return json.optString(
                        "translatedText",
                        text
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            return text;
        }
    }

    String hardCode(String language, String normalized){
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

//    public String detectLanguage(String text) {
//
//        try {
//
//            URL url =
//                    new URL(
//                            baseUrl + "/detect"
//                    );
//
//            HttpURLConnection conn =
//                    (HttpURLConnection)
//                            url.openConnection();
//
//            conn.setRequestMethod("POST");
//
//            conn.setRequestProperty(
//                    "Content-Type",
//                    "application/json"
//            );
//
//            conn.setDoOutput(true);
//
//            String body =
//                    """
//                    {
//                      "q":"%s"
//                    }
//                    """.formatted(
//                            text.replace("\"", "\\\"")
//                    );
//
//            try (OutputStream os =
//                         conn.getOutputStream()) {
//
//                os.write(
//                        body.getBytes(
//                                StandardCharsets.UTF_8
//                        )
//                );
//            }
//
//            String response;
//
//            try (BufferedReader br =
//                         new BufferedReader(
//                                 new InputStreamReader(
//                                         conn.getInputStream(),
//                                         StandardCharsets.UTF_8
//                                 )
//                         )) {
//
//                response =
//                        br.lines()
//                                .reduce("", String::concat);
//            }
//
//            // VERY SIMPLE PARSE
//            // [{"confidence":100,"language":"ja"}]
//
//            int idx =
//                    response.indexOf(
//                            "\"language\":\""
//                    );
//
//            if (idx == -1) {
//                return "en";
//            }
//
//            String remain =
//                    response.substring(
//                            idx + 12
//                    );
//
//            return remain.substring(
//                    0,
//                    remain.indexOf("\"")
//            );
//
//        } catch (Exception e) {
//
//            e.printStackTrace();
//
//            return "en";
//        }
//    }
}
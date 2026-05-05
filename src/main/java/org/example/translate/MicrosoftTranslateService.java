package org.example.translate;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class MicrosoftTranslateService {

    private static final String API_KEY =
            "YOUR_AZURE_TRANSLATOR_KEY";

    private static final String ENDPOINT =
            "https://api.cognitive.microsofttranslator.com";

    private static final String REGION =
            "southeastasia";

    private final OkHttpClient client =
            new OkHttpClient();

    public String translate(
            String text,
            String language
    ) {

        try {

            String url =
                    ENDPOINT
                            + "/translate"
                            + "?api-version=3.0"
                            + "&from=vi"
                            + "&to="
                            + getLanguageCode(language);

            JSONArray requestArray =
                    new JSONArray();

            JSONObject requestObject =
                    new JSONObject();

            requestObject.put(
                    "Text",
                    text
            );

            requestArray.put(
                    requestObject
            );

            MediaType mediaType =
                    MediaType.parse(
                            "application/json"
                    );

            RequestBody body =
                    RequestBody.create(
                            requestArray.toString(),
                            mediaType
                    );

            Request request =
                    new Request.Builder()

                            .url(url)

                            .post(body)

                            .addHeader(
                                    "Ocp-Apim-Subscription-Key",
                                    API_KEY
                            )

                            .addHeader(
                                    "Ocp-Apim-Subscription-Region",
                                    REGION
                            )

                            .addHeader(
                                    "Content-Type",
                                    "application/json"
                            )

                            .build();

            Response response =
                    client.newCall(request)
                            .execute();

            if (!response.isSuccessful()) {

                System.out.println(
                        response.body().string()
                );

                return text;
            }

            String responseBody =
                    response.body().string();

            JSONArray array =
                    new JSONArray(responseBody);

            return array
                    .getJSONObject(0)
                    .getJSONArray("translations")
                    .getJSONObject(0)
                    .getString("text");

        } catch (IOException e) {

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

            case "Chinese" -> "zh-Hans";

            case "Thai" -> "th";

            case "French" -> "fr";

            case "German" -> "de";

            default -> "en";
        };
    }
}

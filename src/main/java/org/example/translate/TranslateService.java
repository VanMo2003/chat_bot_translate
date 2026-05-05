package org.example.translate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TranslateService {

    public String translate(
            String text,
            String targetLang
    ) {

        try {

            URL url =
                    new URL(
                            "https://libretranslate.com/translate"
                    );

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
            );

            connection.setDoOutput(true);

            String json =
                    """
                    {
                      "q": "%s",
                      "source": "auto",
                      "target": "%s",
                      "format": "text"
                    }
                    """.formatted(
                            text.replace("\"", "\\\""),
                            targetLang
                    );

            try (OutputStream os =
                         connection.getOutputStream()) {

                byte[] input =
                        json.getBytes(
                                StandardCharsets.UTF_8
                        );

                os.write(input, 0, input.length);
            }

            BufferedReader br =
                    new BufferedReader(

                            new InputStreamReader(
                                    connection.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder response =
                    new StringBuilder();

            String line;

            while ((line = br.readLine()) != null) {

                response.append(line.trim());
            }

            System.out.println(response);

            ObjectMapper mapper =
                    new ObjectMapper();

            JsonNode node =
                    mapper.readTree(
                            response.toString()
                    );

            return node
                    .get("translatedText")
                    .asText();

        } catch (Exception e) {

            e.printStackTrace();

            return text;
        }
    }
}
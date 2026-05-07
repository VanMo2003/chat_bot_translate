package org.example.uitls;

import java.awt.*;
import java.io.InputStream;

public class FontUtils {

    private static Font defaultFont;

    static {

        try {

            loadFont("/fonts/NotoSans-Regular.ttf");

            loadFont("/fonts/NotoSans-Bold.ttf");

            loadFont("/fonts/NotoSansThai-Regular.ttf");

            loadFont("/fonts/NotoSansThai-Bold.ttf");

//            loadFont("/fonts/NotoSansCJKjp-Regular.otf");

            loadFont("/fonts/NotoColorEmoji-Regular.otf");

            defaultFont =
                    new Font("Noto Sans", Font.PLAIN, 16);

        } catch (Exception e) {

            e.printStackTrace();

            defaultFont =
                    new Font("Dialog", Font.PLAIN, 16);
        }
    }

    private static void loadFont(String path)
            throws Exception {

        InputStream is =
                FontUtils.class.getResourceAsStream(path);

        if (is == null) {
            return;
        }

        Font font =
                Font.createFont(
                        Font.TRUETYPE_FONT,
                        is
                );

        GraphicsEnvironment ge =
                GraphicsEnvironment
                        .getLocalGraphicsEnvironment();

        ge.registerFont(font);
    }

    public static Font getFont(float size) {

        return defaultFont.deriveFont(size);
    }
}
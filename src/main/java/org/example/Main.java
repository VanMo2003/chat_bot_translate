package org.example;

import org.drinkless.tdlib.Client;
import org.example.ui.MainFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            MainFrame frame =
                    null;
            try {
                frame = new MainFrame();
            } catch (Client.ExecutionException e) {
                throw new RuntimeException(e);
            }

            frame.setVisible(true);
        });
    }
}

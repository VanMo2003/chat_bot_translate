package org.example.ui;

import org.drinkless.tdlib.Client;
import org.example.telegram.TelegramService;
import org.example.translate.LibreTranslateService;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final TelegramService telegramService = new TelegramService();
    private final LibreTranslateService translateService = new LibreTranslateService();

    private final JTextArea chatArea = new JTextArea();

    private final JTextField phoneField = new JTextField();
    private final JTextField otpField = new JTextField();
    private final JTextField passwordField = new JTextField();
    private final JTextField chatIdField = new JTextField();

    private final JTextField messageField = new JTextField();
    private final JTextField translatedField = new JTextField();

    private final JComboBox<String> languageBox = new JComboBox<>();
    private Map<String, String> languageMap = new LinkedHashMap<>();

    private final JButton loginButton = new JButton("Login");
    private final JButton otpButton = new JButton("Send OTP");
    private final JButton passwordButton = new JButton("2FA");
    private final JButton loadChatButton = new JButton("Load Chat");

    private final JButton translateButton = new JButton("Translate");
    private final JButton sendOriginalButton = new JButton("Send (Original)");
    private final JButton sendTranslatedButton = new JButton("Send (Translated)");

    private long currentChatId = 0;

    public MainFrame() throws Client.ExecutionException {
        setTitle("Telegram CSKH");
        setSize(1000, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        initTelegram();
    }

    private void initUI() {

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(7, 3, 10, 10));

        // PHONE
        topPanel.add(new JLabel("Phone"));
        topPanel.add(phoneField);
        topPanel.add(loginButton);

        // OTP
        topPanel.add(new JLabel("OTP"));
        topPanel.add(otpField);
        topPanel.add(otpButton);

        // PASSWORD
        topPanel.add(new JLabel("2FA Password"));
        topPanel.add(passwordField);
        topPanel.add(passwordButton);

        // CHAT ID
        topPanel.add(new JLabel("Chat ID"));
        topPanel.add(chatIdField);
        topPanel.add(loadChatButton);

        // LANGUAGE
        topPanel.add(new JLabel("Language"));
        topPanel.add(languageBox);
        topPanel.add(new JLabel());

        // MESSAGE GỐC
        topPanel.add(new JLabel("Message (VN)"));
        topPanel.add(messageField);
        topPanel.add(translateButton);

        // MESSAGE DỊCH
        topPanel.add(new JLabel("Translated"));
        topPanel.add(translatedField);

        JPanel sendPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        sendPanel.add(sendOriginalButton);
        sendPanel.add(sendTranslatedButton);

        topPanel.add(sendPanel);

        add(topPanel, BorderLayout.NORTH);

        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // ACTIONS
        loginButton.addActionListener(e ->
                telegramService.setPhoneNumber(phoneField.getText())
        );

        otpButton.addActionListener(e ->
                telegramService.checkCode(otpField.getText())
        );

        passwordButton.addActionListener(e ->
                telegramService.checkPassword(passwordField.getText())
        );

        loadChatButton.addActionListener(e -> loadChat());

        translateButton.addActionListener(e -> translate());

        sendOriginalButton.addActionListener(e -> sendOriginal());

        sendTranslatedButton.addActionListener(e -> sendTranslated());

        messageField.addActionListener(e -> translate());

        loadLanguages();
    }

    private void initTelegram() throws Client.ExecutionException {

        telegramService.setMessageListener((chatId, sender, message) -> {

            if (chatId != currentChatId) return;

            SwingUtilities.invokeLater(() ->
                    chatArea.append(sender + ": " + message + "\n")
            );
        });

        telegramService.setHistoryListener((chatId, sender, message) -> {

            if (chatId != currentChatId) return;

            SwingUtilities.invokeLater(() ->
                    chatArea.append(sender + ": " + message + "\n")
            );
        });

        telegramService.init();
    }

    private void loadLanguages() {

        languageMap = translateService.getSupportedLanguages();

        languageBox.removeAllItems();

        for (String name : languageMap.keySet()) {
            languageBox.addItem(name);
        }

        if (languageBox.getItemCount() > 0) {
            languageBox.setSelectedIndex(0);
        }
    }

    private void loadChat() {

        try {
            currentChatId = Long.parseLong(chatIdField.getText());
            chatArea.setText("");
            telegramService.loadChatHistory(currentChatId);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Chat ID không hợp lệ");
        }
    }

    private void translate() {

        try {

            String text = messageField.getText();

            if (text.isBlank()) return;

            String langName = languageBox.getSelectedItem().toString();
            String langCode = languageMap.get(langName);

            String translated = translateService.translateText(text, langCode);

            translatedField.setText(translated);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi dịch");
        }
    }

    private void sendOriginal() {

        try {

            long chatId = Long.parseLong(chatIdField.getText());
            String text = messageField.getText();

            if (text.isBlank()) return;

            telegramService.sendMessage(chatId, text);

            chatArea.append("Tôi (VN): " + text + "\n");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi message gốc");
        }
    }

    private void sendTranslated() {

        try {

            long chatId = Long.parseLong(chatIdField.getText());
            String text = translatedField.getText();

            if (text.isBlank()) return;

            telegramService.sendMessage(chatId, text);

            chatArea.append("Tôi (Translated): " + text + "\n");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi bản dịch");
        }
    }
}
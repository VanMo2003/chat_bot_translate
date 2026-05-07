package org.example.ui;

import org.drinkless.tdlib.Client;
import org.example.model.Customer;
import org.example.telegram.TelegramService;
import org.example.translate.LibreTranslateService;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final TelegramService telegramService =
            new TelegramService();

    private final LibreTranslateService translateService =
            new LibreTranslateService();

    // =========================
    // UI
    // =========================

    private final JTextArea chatArea =
            new JTextArea();

    private final JTextField phoneField =
            new JTextField();

    private final JTextField otpField =
            new JTextField();

    private final JTextField passwordField =
            new JTextField();

    private final JTextField messageField =
            new JTextField();

    private final JTextField translatedField =
            new JTextField();

    private final JComboBox<String> languageBox =
            new JComboBox<>();

    private final JComboBox<Customer> customerBox =
            new JComboBox<>();

    // =========================
    // BUTTON
    // =========================

    private final JButton loginButton =
            new JButton("Login");

    private final JButton otpButton =
            new JButton("Send OTP");

    private final JButton passwordButton =
            new JButton("2FA");

    private final JButton refreshCustomerButton =
            new JButton("Refresh Users");

    private final JButton loadChatButton =
            new JButton("Load Chat");

    private final JButton translateButton =
            new JButton("Translate");

    private final JButton sendOriginalButton =
            new JButton("Send Original");

    private final JButton sendTranslatedButton =
            new JButton("Send Translated");

    // =========================
    // DATA
    // =========================

    private final Map<String, String> languageMap =
            new LinkedHashMap<>();

    private long currentChatId = 0;

    // =========================
    // CONSTRUCTOR
    // =========================

    public MainFrame()
            throws Client.ExecutionException {

        setTitle("Telegram CSKH");

        setSize(1000, 750);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        initUI();

        initTelegram();

        // AUTO LOAD
        loadLanguages();

        loadCustomers();
    }

    // =========================
    // INIT UI
    // =========================

    private void initUI() {

        setLayout(new BorderLayout());

        JPanel topPanel =
                new JPanel(
                        new GridLayout(
                                7,
                                3,
                                10,
                                10
                        )
                );

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

        // CUSTOMER
        topPanel.add(new JLabel("Customer"));
        topPanel.add(customerBox);
        topPanel.add(refreshCustomerButton);

        // LANGUAGE
        topPanel.add(new JLabel("Language"));
        topPanel.add(languageBox);
        topPanel.add(loadChatButton);

        // MESSAGE
        topPanel.add(new JLabel("Message (VN)"));
        topPanel.add(messageField);
        topPanel.add(translateButton);

        // TRANSLATED
        topPanel.add(new JLabel("Translated"));
        topPanel.add(translatedField);

        JPanel sendPanel =
                new JPanel(
                        new GridLayout(1, 2, 5, 0)
                );

        sendPanel.add(sendOriginalButton);

        sendPanel.add(sendTranslatedButton);

        topPanel.add(sendPanel);

        add(topPanel, BorderLayout.NORTH);

        // CHAT AREA
        chatArea.setEditable(false);

        chatArea.setFont(
                new Font(
                        "Arial",
                        Font.PLAIN,
                        16
                )
        );

        JScrollPane scrollPane =
                new JScrollPane(chatArea);

        add(scrollPane, BorderLayout.CENTER);

        // =========================
        // ACTION
        // =========================

        loginButton.addActionListener(e ->
                telegramService.setPhoneNumber(
                        phoneField.getText()
                )
        );

        otpButton.addActionListener(e ->
                telegramService.checkCode(
                        otpField.getText()
                )
        );

        passwordButton.addActionListener(e ->
                telegramService.checkPassword(
                        passwordField.getText()
                )
        );

        refreshCustomerButton.addActionListener(e ->
                loadCustomers()
        );

        loadChatButton.addActionListener(e ->
                loadSelectedChat()
        );

        translateButton.addActionListener(e ->
                translate()
        );

        sendOriginalButton.addActionListener(e ->
                sendOriginal()
        );

        sendTranslatedButton.addActionListener(e ->
                sendTranslated()
        );

        // ENTER => TRANSLATE
        messageField.addActionListener(e ->
                translate()
        );

        // CUSTOMER SELECT
        customerBox.addActionListener(e -> {

            Customer customer =
                    (Customer)
                            customerBox.getSelectedItem();

            if (customer == null) return;

            currentChatId =
                    customer.getChatId();

            selectLanguage(
                    customer.getLanguageCode()
            );
        });
    }

    // =========================
    // INIT TELEGRAM
    // =========================

    private void initTelegram()
            throws Client.ExecutionException {

        telegramService.setMessageListener(
                (chatId, sender, message) -> {

                    if (chatId != currentChatId) {
                        return;
                    }

                    SwingUtilities.invokeLater(() ->

                            chatArea.append(
                                    sender
                                            + ": "
                                            + message
                                            + "\n"
                            )
                    );
                }
        );

        telegramService.setHistoryListener(
                (chatId, sender, message) -> {

                    if (chatId != currentChatId) {
                        return;
                    }

                    SwingUtilities.invokeLater(() ->

                            chatArea.append(
                                    sender
                                            + ": "
                                            + message
                                            + "\n"
                            )
                    );
                }
        );

        telegramService.init();
    }

    // =========================
    // LOAD LANGUAGE
    // =========================

    private void loadLanguages() {

        languageMap.clear();

        languageMap.putAll(
                translateService.getSupportedLanguages()
        );

        languageBox.removeAllItems();

        for (String name : languageMap.keySet()) {

            languageBox.addItem(name);
        }

        if (languageBox.getItemCount() > 0) {

            languageBox.setSelectedIndex(0);
        }
    }

    // =========================
    // LOAD CUSTOMER
    // =========================

    private void loadCustomers() {

        refreshCustomerButton.setEnabled(false);

        refreshCustomerButton.setText("Loading...");

        telegramService.getCustomers(customers -> {

            SwingUtilities.invokeLater(() -> {

                customerBox.removeAllItems();

                for (Customer customer : customers) {

                    customerBox.addItem(customer);
                }

                // AUTO SELECT FIRST
                if (customerBox.getItemCount() > 0) {

                    customerBox.setSelectedIndex(0);

                    Customer customer =
                            (Customer)
                                    customerBox.getSelectedItem();

                    if (customer != null) {

                        currentChatId =
                                customer.getChatId();
                    }
                }

                refreshCustomerButton.setEnabled(true);

                refreshCustomerButton.setText(
                        "Refresh Users"
                );
            });
        });
    }

    // =========================
    // LOAD CHAT
    // =========================

    private void loadSelectedChat() {

        try {

            Customer customer =
                    (Customer)
                            customerBox.getSelectedItem();

            if (customer == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please select customer"
                );

                return;
            }

            currentChatId =
                    customer.getChatId();

            chatArea.setText("");

            telegramService.loadChatHistory(
                    currentChatId
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Cannot load chat"
            );

            e.printStackTrace();
        }
    }

    // =========================
    // TRANSLATE
    // =========================

    private void translate() {

        try {

            String text =
                    messageField.getText();

            if (text == null
                    || text.isBlank()) {

                return;
            }

            Object selected =
                    languageBox.getSelectedItem();

            if (selected == null) {
                return;
            }

            String langName =
                    selected.toString();

            String langCode =
                    languageMap.get(langName);

            String translated =
                    translateService.translateText(
                            text,
                            langCode
                    );

            translatedField.setText(
                    translated
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi dịch"
            );

            e.printStackTrace();
        }
    }

    // =========================
    // SEND ORIGINAL
    // =========================

    private void sendOriginal() {

        try {

            if (currentChatId == 0) {
                return;
            }

            String text =
                    messageField.getText();

            if (text == null
                    || text.isBlank()) {

                return;
            }

            telegramService.sendMessage(
                    currentChatId,
                    text
            );

            chatArea.append(
                    "Tôi (VN): "
                            + text
                            + "\n"
            );

            messageField.setText("");

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi gửi message gốc"
            );

            e.printStackTrace();
        }
    }

    // =========================
    // SEND TRANSLATED
    // =========================

    private void sendTranslated() {

        try {

            if (currentChatId == 0) {
                return;
            }

            String text =
                    translatedField.getText();

            if (text == null
                    || text.isBlank()) {

                return;
            }

            telegramService.sendMessage(
                    currentChatId,
                    text
            );

            chatArea.append(
                    "Tôi (Translated): "
                            + text
                            + "\n"
            );

            translatedField.setText("");

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi gửi bản dịch"
            );

            e.printStackTrace();
        }
    }

    // =========================
    // SELECT LANGUAGE
    // =========================

    private void selectLanguage(
            String code
    ) {

        for (Map.Entry<String, String> entry
                : languageMap.entrySet()) {

            if (entry.getValue().equals(code)) {

                languageBox.setSelectedItem(
                        entry.getKey()
                );

                break;
            }
        }
    }
}

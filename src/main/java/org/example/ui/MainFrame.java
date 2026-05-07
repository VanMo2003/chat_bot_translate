package org.example.ui;

import org.drinkless.tdlib.Client;
import org.example.model.Customer;
import org.example.telegram.TelegramService;
import org.example.translate.LibreTranslateService;
import org.example.uitls.FontUtils;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    // =========================
    // FONT
    // =========================
    private final Font unicodeFont = FontUtils.getFont(16f);

    // =========================
    // SERVICE
    // =========================

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

    // MULTI LINE
    private final JTextArea messageField =
            new JTextArea(4, 20);

    private final JTextArea translatedField =
            new JTextArea(4, 20);

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

        setSize(1100, 800);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        initUI();

        initTelegram();

        loadLanguages();

        loadCustomers();
    }

    // =========================
    // INIT UI
    // =========================

    private void initUI() {

        setLayout(new BorderLayout());

        JPanel topPanel =
                new JPanel(new GridBagLayout());

        topPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(5, 5, 5, 5);

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.weightx = 1;

        int row = 0;

        // =========================
        // FONT
        // =========================

        phoneField.setFont(unicodeFont);

        otpField.setFont(unicodeFont);

        passwordField.setFont(unicodeFont);

        messageField.setFont(unicodeFont);

        translatedField.setFont(unicodeFont);

        languageBox.setFont(unicodeFont);

        customerBox.setFont(unicodeFont);

        chatArea.setFont(unicodeFont);

        // =========================
        // MESSAGE AREA CONFIG
        // =========================

        messageField.setLineWrap(true);

        messageField.setWrapStyleWord(true);

        translatedField.setLineWrap(true);

        translatedField.setWrapStyleWord(true);

        // =========================
        // PHONE
        // =========================

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;

        topPanel.add(
                new JLabel("Phone"),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        topPanel.add(phoneField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;

        topPanel.add(loginButton, gbc);

        row++;

        // =========================
        // OTP
        // =========================

        gbc.gridx = 0;
        gbc.gridy = row;

        topPanel.add(
                new JLabel("OTP"),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        topPanel.add(otpField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;

        topPanel.add(otpButton, gbc);

        row++;

        // =========================
        // PASSWORD
        // =========================

        gbc.gridx = 0;
        gbc.gridy = row;

        topPanel.add(
                new JLabel("2FA Password"),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        topPanel.add(passwordField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;

        topPanel.add(passwordButton, gbc);

        row++;

        // =========================
        // CUSTOMER
        // =========================

        gbc.gridx = 0;
        gbc.gridy = row;

        topPanel.add(
                new JLabel("Customer"),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        topPanel.add(customerBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;

        topPanel.add(refreshCustomerButton, gbc);

        row++;

        // =========================
        // LANGUAGE
        // =========================

        gbc.gridx = 0;
        gbc.gridy = row;

        topPanel.add(
                new JLabel("Language"),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        topPanel.add(languageBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;

        topPanel.add(loadChatButton, gbc);

        row++;

        // =========================
        // MESSAGE AREA
        // =========================

        JScrollPane messageScroll =
                new JScrollPane(messageField);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTH;

        topPanel.add(
                new JLabel("Message (VN)"),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;

        topPanel.add(messageScroll, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        topPanel.add(translateButton, gbc);

        row++;

        // =========================
        // TRANSLATED AREA
        // =========================

        JScrollPane translatedScroll =
                new JScrollPane(translatedField);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTH;

        topPanel.add(
                new JLabel("Translated"),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;

        topPanel.add(translatedScroll, gbc);

        JPanel sendPanel =
                new JPanel(
                        new GridLayout(1, 2, 5, 0)
                );

        sendPanel.add(sendOriginalButton);

        sendPanel.add(sendTranslatedButton);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        topPanel.add(sendPanel, gbc);

        add(topPanel, BorderLayout.NORTH);

        // =========================
        // CHAT AREA
        // =========================

        chatArea.setEditable(false);

        chatArea.setLineWrap(true);

        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane =
                new JScrollPane(chatArea);

        add(scrollPane, BorderLayout.CENTER);

        // AUTO SCROLL

        DefaultCaret caret =
                (DefaultCaret)
                        chatArea.getCaret();

        caret.setUpdatePolicy(
                DefaultCaret.ALWAYS_UPDATE
        );

        // =========================
        // ACTION
        // =========================

        loginButton.addActionListener(e ->
                telegramService.resetSessionAndLogin(
                        phoneField.getText().replaceAll("0", "+84")
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

        // CTRL + ENTER => TRANSLATE

        messageField.getInputMap().put(
                KeyStroke.getKeyStroke(
                        "ctrl ENTER"
                ),
                "translate"
        );

        messageField.getActionMap().put(
                "translate",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(
                            java.awt.event.ActionEvent e
                    ) {

                        translate();
                    }
                }
        );

        // CUSTOMER SELECT

        customerBox.addActionListener(e -> {

            Customer customer =
                    (Customer)
                            customerBox.getSelectedItem();

            if (customer == null) {
                return;
            }

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
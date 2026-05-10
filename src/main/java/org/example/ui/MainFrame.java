package org.example.ui;

import org.drinkless.tdlib.Client;
import org.example.model.Customer;
import org.example.telegram.TelegramService;
import org.example.translate.LibreTranslateService;
import org.example.uitls.FontUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private final Font unicodeFont = FontUtils.getFont(15f);

    private final TelegramService telegramService = new TelegramService();
    private final LibreTranslateService translateService = new LibreTranslateService();

    // Dữ liệu
    private final Map<String, String> languageMap = new LinkedHashMap<>();
    private final List<Customer> allCustomers = new ArrayList<>();
    private long currentChatId = 0;
    private StringBuilder chatHtmlBuilder = new StringBuilder();

    // UI Left Panel
    private final DefaultListModel<Customer> customerListModel = new DefaultListModel<>();
    private final JList<Customer> customerList = new JList<>(customerListModel);
    private final JTextField searchUserField = new JTextField();
    private final JButton refreshCustomerButton = new JButton("🔄 Tải lại User");

    // UI Right Panel (Top)
    private final JTextField searchLangField = new JTextField();
    private final JComboBox<String> languageBox = new JComboBox<>();
    private final JButton translateAllButton = new JButton("Dịch toàn bộ sang Tiếng Việt");
    private final JButton reloadChatButton = new JButton("Tải lại Chat");
    private final JButton showLoginButton = new JButton("Đăng nhập");

    // UI Right Panel (Center)
    private final JTextPane chatPane = new JTextPane();

    // UI Right Panel (Bottom)
    private final JTextArea messageField = new JTextArea(3, 20);
    private final JTextArea translatedField = new JTextArea(3, 20);
    private final JButton translateButton = new JButton("Dịch (Ctrl+Enter)");
    private final JButton sendOriginalButton = new JButton("Gửi gốc");
    private final JButton sendTranslatedButton = new JButton("Gửi bản dịch");

    public MainFrame() throws Client.ExecutionException {
        setTitle("Telegram Chat Bot Translate");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();

        // Load user tự động khi Auth báo Ready
        telegramService.setOnAuthReady(() -> SwingUtilities.invokeLater(this::loadCustomers));

        initTelegram();
        loadLanguages();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // CHIA ĐÔI MÀN HÌNH
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createLeftPanel());
        splitPane.setRightComponent(createRightPanel());
        splitPane.setDividerLocation(300); // Kích thước menu trái
        splitPane.setResizeWeight(0.2);

        add(splitPane, BorderLayout.CENTER);
        setupActions();
    }

    // ======================================
    // LAYOUT BÊN TRÁI (DANH SÁCH USER)
    // ======================================
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        searchUserField.setFont(unicodeFont);
        searchUserField.setToolTipText("Tìm kiếm người dùng...");
        searchUserField.setPreferredSize(new Dimension(150, 30));
        topPanel.add(new JLabel("Tìm user: "), BorderLayout.WEST);
        topPanel.add(searchUserField, BorderLayout.CENTER);
        topPanel.add(refreshCustomerButton, BorderLayout.SOUTH);

        customerList.setFont(unicodeFont);
        customerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(customerList);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ======================================
    // LAYOUT BÊN PHẢI (CHAT & TRANSLATE)
    // ======================================
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        searchLangField.setFont(unicodeFont);
        searchLangField.setToolTipText("Tìm NN");
        searchLangField.setPreferredSize(new Dimension(150, 30));

        languageBox.setFont(unicodeFont);
        languageBox.setPreferredSize(new Dimension(150, 30));

        topPanel.add(new JLabel("Tìm ngôn ngữ:"));
        topPanel.add(searchLangField);
        topPanel.add(languageBox);
        topPanel.add(translateAllButton);
        topPanel.add(reloadChatButton);

        JPanel wrapperTop = new JPanel(new BorderLayout());
        wrapperTop.add(topPanel, BorderLayout.CENTER);
        JPanel rightLoginPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightLoginPanel.add(showLoginButton);
        wrapperTop.add(rightLoginPanel, BorderLayout.EAST);

        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.setFont(unicodeFont);
        JScrollPane chatScroll = new JScrollPane(chatPane);
        resetChatHtml();

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;

        messageField.setFont(unicodeFont);
        messageField.setLineWrap(true);
        messageField.setWrapStyleWord(true);

        translatedField.setFont(unicodeFont);
        translatedField.setLineWrap(true);
        translatedField.setWrapStyleWord(true);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0.5;
        bottomPanel.add(new JScrollPane(messageField), gbc);

        JPanel btnPanel1 = new JPanel(new GridLayout(2, 1, 0, 5));
        btnPanel1.add(translateButton);
        btnPanel1.add(sendOriginalButton);
        gbc.gridx = 1; gbc.weightx = 0;
        bottomPanel.add(btnPanel1, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1.0; gbc.weighty = 0.5;
        bottomPanel.add(new JScrollPane(translatedField), gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        bottomPanel.add(sendTranslatedButton, gbc);

        panel.add(wrapperTop, BorderLayout.NORTH);
        panel.add(chatScroll, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ======================================
    // CÀI ĐẶT SỰ KIỆN (ACTIONS)
    // ======================================
    private void setupActions() {
        showLoginButton.addActionListener(e -> {
            LoginDialog dialog = new LoginDialog(this, telegramService, unicodeFont);
            dialog.setVisible(true);
        });

        refreshCustomerButton.addActionListener(e -> loadCustomers());

        searchUserField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterUsers(); }
            public void removeUpdate(DocumentEvent e) { filterUsers(); }
            public void changedUpdate(DocumentEvent e) { filterUsers(); }
        });

        searchLangField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterLanguages(); }
            public void removeUpdate(DocumentEvent e) { filterLanguages(); }
            public void changedUpdate(DocumentEvent e) { filterLanguages(); }
        });

        customerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedChat();
            }
        });

        reloadChatButton.addActionListener(e -> loadSelectedChat());
        translateAllButton.addActionListener(e -> translateAllChatToVietnamese());

        translateButton.addActionListener(e -> translateMessage());
        sendOriginalButton.addActionListener(e -> sendOriginal());
        sendTranslatedButton.addActionListener(e -> sendTranslated());

        messageField.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "translate");
        messageField.getActionMap().put("translate", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                translateMessage();
            }
        });
    }

    // ======================================
    // LOGIC TELEGRAM & TRANSLATE
    // ======================================
    private void initTelegram() throws Client.ExecutionException {
        telegramService.setMessageListener((chatId, sender, message) -> {
            if (chatId != currentChatId) return;
            SwingUtilities.invokeLater(() -> appendChatMessage(sender, message));
        });

        telegramService.setHistoryListener((chatId, sender, message) -> {
            if (chatId != currentChatId) return;
            SwingUtilities.invokeLater(() -> appendChatMessage(sender, message));
        });

        telegramService.init();
    }

    private void loadLanguages() {
        languageMap.clear();
        languageMap.putAll(translateService.getSupportedLanguages());
        filterLanguages();
    }

    private void filterLanguages() {
        String query = searchLangField.getText().toLowerCase();
        languageBox.removeAllItems();
        for (String name : languageMap.keySet()) {
            if (name.toLowerCase().contains(query)) {
                languageBox.addItem(name);
            }
        }
        if (languageBox.getItemCount() > 0) languageBox.setSelectedIndex(0);
    }

    private void loadCustomers() {
        refreshCustomerButton.setEnabled(false);
        refreshCustomerButton.setText("Đang tải...");

        telegramService.getCustomers(customers -> SwingUtilities.invokeLater(() -> {
            allCustomers.clear();
            allCustomers.addAll(customers);
            filterUsers();
            refreshCustomerButton.setEnabled(true);
            refreshCustomerButton.setText("🔄 Tải lại User");
        }));
    }

    private void filterUsers() {
        String query = searchUserField.getText().toLowerCase();
        customerListModel.clear();
        for (Customer c : allCustomers) {
            if (c.getName().toLowerCase().contains(query)) {
                customerListModel.addElement(c);
            }
        }
    }

    private void loadSelectedChat() {
        Customer customer = customerList.getSelectedValue();
        if (customer == null) return;

        currentChatId = customer.getChatId();

        telegramService.loadChatHistory(currentChatId, this::resetChatHtml);
    }

    private void translateMessage() {
        try {
            String text = messageField.getText();
            if (text == null || text.isBlank()) return;

            Object selected = languageBox.getSelectedItem();
            if (selected == null) return;

            String langCode = languageMap.get(selected.toString());
            String translated = translateService.translateText(text, "vi", langCode);
            translatedField.setText(translated);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi dịch thuật");
            e.printStackTrace();
        }
    }

    private void translateAllChatToVietnamese() {
        new Thread(() -> {
            try {
                telegramService.setHistoryListener((chatId, sender, message) -> {
                    if (chatId != currentChatId) return;
                    String translatedMsg = translateService.translateText(message, "auto", "vi");
                    SwingUtilities.invokeLater(() -> appendChatMessage(sender, translatedMsg));
                });

                // Pass hàm callback để báo hiệu "Đang dịch"
                telegramService.loadChatHistory(currentChatId, () -> {
                    resetChatHtml();
                    appendChatMessage("Hệ thống", "<i>Đang tải và dịch toàn bộ tin nhắn...</i>");
                });

                Thread.sleep(4000);
                telegramService.setHistoryListener((chatId, sender, message) -> {
                    if (chatId != currentChatId) return;
                    SwingUtilities.invokeLater(() -> appendChatMessage(sender, message));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendOriginal() {
        String text = messageField.getText();
        if (text == null || text.isBlank() || currentChatId == 0) return;

        telegramService.sendMessage(currentChatId, text);

        messageField.setText("");
    }

    private void sendTranslated() {
        String text = translatedField.getText();
        if (text == null || text.isBlank() || currentChatId == 0) return;

        telegramService.sendMessage(currentChatId, text);

        translatedField.setText("");
        messageField.setText("");
    }

    // ======================================
    // HIỂN THỊ CHAT BẰNG HTML (BONG BÓNG)
    // ======================================
    private void resetChatHtml() {
        chatHtmlBuilder = new StringBuilder();
        chatHtmlBuilder.append("<html><body style='font-family: sans-serif; font-size: 14px; margin: 10px;'>");
        chatPane.setText(chatHtmlBuilder.toString() + "</body></html>");
    }

    private void appendChatMessage(String sender, String text) {
        String escapedText = text.replace("\n", "<br>").replace("<", "&lt;").replace(">", "&gt;");

        if (sender.equals("Tôi")) {
            chatHtmlBuilder.append("<div style='text-align: right; margin-bottom: 8px;'>")
                    .append("<span style='background-color: #DCF8C6; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%; text-align: left;'>")
                    .append(escapedText)
                    .append("</span></div>");
        } else if (sender.equals("Hệ thống")) {
            chatHtmlBuilder.append("<div style='text-align: center; margin-bottom: 8px; color: gray;'>")
                    .append(escapedText)
                    .append("</div>");
        } else {
            chatHtmlBuilder.append("<div style='text-align: left; margin-bottom: 8px;'>")
                    .append("<span style='background-color: #F1F0F0; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%;'>")
                    .append("<b>").append(sender).append("</b><br>")
                    .append(escapedText)
                    .append("</span></div>");
        }

        chatPane.setText(chatHtmlBuilder.toString() + "</body></html>");
        SwingUtilities.invokeLater(() -> chatPane.setCaretPosition(chatPane.getDocument().getLength()));
    }

    // ======================================
    // LỚP DIALOG ĐĂNG NHẬP (INNER CLASS)
    // ======================================
    class LoginDialog extends JDialog {
        private JTextField phoneField = new JTextField();
        private JTextField otpField = new JTextField();
        private JTextField passwordField = new JTextField();

        public LoginDialog(JFrame parent, TelegramService telegramService, Font font) {
            super(parent, "Đăng nhập Telegram", true);
            setLayout(new GridBagLayout());

            setSize(450, 220);
            setLocationRelativeTo(parent);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            phoneField.setFont(font);
            otpField.setFont(font);
            passwordField.setFont(font);

            phoneField.setPreferredSize(new Dimension(150, 30));
            otpField.setPreferredSize(new Dimension(150, 30));
            passwordField.setPreferredSize(new Dimension(150, 30));

            JButton loginBtn = new JButton("Gửi SĐT");
            JButton otpBtn = new JButton("Gửi OTP");
            JButton passBtn = new JButton("Gửi 2FA Pass");

            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
            add(new JLabel("SĐT:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; add(phoneField, gbc);
            gbc.gridx = 2; gbc.weightx = 0; add(loginBtn, gbc);

            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
            add(new JLabel("OTP:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; add(otpField, gbc);
            gbc.gridx = 2; gbc.weightx = 0; add(otpBtn, gbc);

            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
            add(new JLabel("2FA:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; add(passwordField, gbc);
            gbc.gridx = 2; gbc.weightx = 0; add(passBtn, gbc);

            loginBtn.addActionListener(e ->
                    telegramService.resetSessionAndLogin(phoneField.getText().replaceAll("^0", "+84"))
            );

            otpBtn.addActionListener(e ->
                    telegramService.checkCode(otpField.getText())
            );

            passBtn.addActionListener(e -> {
                telegramService.checkPassword(passwordField.getText());
                dispose();
            });
        }
    }
}
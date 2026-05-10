package org.example.ui;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.example.model.Customer;
import org.example.telegram.TelegramService;
import org.example.translate.LibreTranslateService;
import org.example.uitls.FontUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<Long, Integer> unreadCounts = new LinkedHashMap<>();

    // ĐÃ THÊM: Bộ nhớ đệm (Cache) để lưu trữ nội dung chat đã dịch
    private final Map<Long, StringBuilder> chatHtmlCache = new HashMap<>();
    private long currentChatId = 0;

    // UI Left Panel
    private final DefaultListModel<Customer> customerListModel = new DefaultListModel<>();
    private final JList<Customer> customerList = new JList<>(customerListModel);
    private final JTextField searchUserField = new JTextField();
    private final JButton refreshCustomerButton = new JButton("🔄 Tải lại User");

    // UI Right Panel (Top)
    private final JTextField searchLangField = new JTextField();
    private final JComboBox<String> languageBox = new JComboBox<>();
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

        telegramService.setOnAuthReady(() -> SwingUtilities.invokeLater(this::loadCustomers));

        initTelegram();
        loadLanguages();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createLeftPanel());
        splitPane.setRightComponent(createRightPanel());
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.2);

        add(splitPane, BorderLayout.CENTER);
        setupActions();
    }

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

        customerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Customer) {
                    Customer customer = (Customer) value;
                    int unread = unreadCounts.getOrDefault(customer.getChatId(), 0);

                    String displayText = value.toString().replace("<", "&lt;").replace(">", "&gt;");

                    if (unread > 0) {
                        setText("<html><b>[" + unread + "] " + displayText + "</b></html>");
                        if (!isSelected) {
                            setForeground(new Color(200, 0, 0));
                        }
                    } else {
                        setText(displayText);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(customerList);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

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
//        topPanel.add(translateAllButton);
        topPanel.add(reloadChatButton);

        JPanel wrapperTop = new JPanel(new BorderLayout());
        wrapperTop.add(topPanel, BorderLayout.CENTER);
        JPanel rightLoginPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightLoginPanel.add(showLoginButton);
        wrapperTop.add(rightLoginPanel, BorderLayout.EAST);

        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.setFont(unicodeFont);
        chatPane.setText("<html><body style='font-family: sans-serif; font-size: 14px; margin: 10px; color: gray;'><i>Chọn người dùng để bắt đầu...</i></body></html>");
        JScrollPane chatScroll = new JScrollPane(chatPane);

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

        // Xóa cache của Chat hiện tại để ép tải & dịch lại từ đầu
        reloadChatButton.addActionListener(e -> {
            if (currentChatId != 0) {
                chatHtmlCache.remove(currentChatId);
                loadAndTranslateHistory(currentChatId);
            }
        });

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

    private void initTelegram() throws Client.ExecutionException {
        telegramService.setMessageListener((chatId, sender, message) -> {
            new Thread(() -> {
                // Chỉ dịch tin nhắn của Khách, giữ nguyên tin nhắn mình gửi
                String displayMsg = message;
                if ("Khách".equals(sender)) {
                    displayMsg = translateService.translateText(message, "auto", "vi");
                }

                final String finalMsg = displayMsg;

                SwingUtilities.invokeLater(() -> {
                    // Nếu đoạn chat này đã có trong Cache, tự động nối tin nhắn mới vào đáy
                    if (chatHtmlCache.containsKey(chatId)) {
                        appendBubbleToBuilder(chatId, sender, finalMsg, false);
                    }

                    // Cập nhật số đếm tin nhắn chưa đọc
                    if (chatId != currentChatId && "Khách".equals(sender)) {
                        int count = unreadCounts.getOrDefault(chatId, 0);
                        unreadCounts.put(chatId, count + 1);
                        customerList.repaint();
                    }
                });
            }).start();
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

        unreadCounts.put(currentChatId, 0);
        customerList.repaint();

        loadAndTranslateHistory(currentChatId);
    }

    // ĐÃ SỬA TỐI ƯU CỰC MẠNH: Dùng chung Cache
    private void loadAndTranslateHistory(long chatId) {
        // Nếu đã từng tải đoạn chat này -> Lấy từ RAM ra ngay lập tức
        if (chatHtmlCache.containsKey(chatId)) {
            StringBuilder builder = chatHtmlCache.get(chatId);
            chatPane.setText(builder.toString() + "</body></html>");
            SwingUtilities.invokeLater(() -> chatPane.setCaretPosition(chatPane.getDocument().getLength()));
            return;
        }

        // Nếu là lần đầu tiên bấm vào -> Khởi tạo bộ nhớ và tải
        StringBuilder newBuilder = new StringBuilder();
        newBuilder.append("<html><body style='font-family: sans-serif; font-size: 14px; margin: 10px;'>");
        chatHtmlCache.put(chatId, newBuilder);

        if (chatId == currentChatId) {
            chatPane.setText(newBuilder.toString() + "<div style='text-align: center; color: gray;'><i>Đang tải và dịch tự động toàn bộ tin nhắn...</i></div></body></html>");
        }

        telegramService.getRawChatHistory(chatId, messages -> {
            new Thread(() -> {
                // Clear UI nếu không có tin nhắn nào
                if (messages.messages.length == 0) {
                    SwingUtilities.invokeLater(() -> {
                        if (chatId == currentChatId) {
                            chatPane.setText(newBuilder.toString() + "</body></html>");
                        }
                    });
                    return;
                }

                for (int i = 0; i < messages.messages.length; i++) {
                    TdApi.Message message = messages.messages[i];
                    if (message.content instanceof TdApi.MessageText textMessage) {
                        String sender = message.isOutgoing ? "Tôi" : "Khách";
                        String text = textMessage.text.text;

                        // Chỉ dịch tin nhắn của Khách
                        String displayMsg = text;
                        if ("Khách".equals(sender)) {
                            displayMsg = translateService.translateText(text, "auto", "vi");
                        }

                        final String finalMsg = displayMsg;
                        SwingUtilities.invokeLater(() -> {
                            appendBubbleToBuilder(chatId, sender, finalMsg, true); // Đẩy lên trên (Prepend)
                        });
                    }
                }
            }).start();
        });
    }

    // ĐÃ THÊM: Hàm lõi để thêm/chèn tin nhắn trực tiếp vào bộ nhớ đệm
    private void appendBubbleToBuilder(long targetChatId, String sender, String text, boolean isPrepend) {
        StringBuilder builder = chatHtmlCache.get(targetChatId);
        if (builder == null) return;

        String escapedText = text.replace("\n", "<br>").replace("<", "&lt;").replace(">", "&gt;");
        StringBuilder bubble = new StringBuilder();

        if (sender.equals("Tôi")) {
            bubble.append("<div style='text-align: right; margin-bottom: 8px;'>")
                    .append("<span style='background-color: #DCF8C6; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%; text-align: left;'>")
                    .append(escapedText)
                    .append("</span></div>");
        } else if (sender.equals("Hệ thống")) {
            bubble.append("<div style='text-align: center; margin-bottom: 8px; color: gray;'>")
                    .append(escapedText)
                    .append("</div>");
        } else {
            bubble.append("<div style='text-align: left; margin-bottom: 8px;'>")
                    .append("<span style='background-color: #F1F0F0; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%;'>")
                    .append("<b>").append(sender).append("</b><br>")
                    .append(escapedText)
                    .append("</span></div>");
        }

        // Nếu Prepend (Dành cho load lịch sử từ mới nhất -> cũ nhất)
        if (isPrepend) {
            String anchor = "margin: 10px;'>";
            int insertIndex = builder.indexOf(anchor);
            if (insertIndex != -1) {
                builder.insert(insertIndex + anchor.length(), bubble.toString());
            } else {
                builder.append(bubble.toString());
            }
        }
        // Nếu Append (Dành cho tin nhắn mới tới trong thời gian thực)
        else {
            builder.append(bubble.toString());
        }

        // Cập nhật ngay lên giao diện nếu đang ở đúng khung Chat đó
        if (targetChatId == currentChatId) {
            chatPane.setText(builder.toString() + "</body></html>");
            SwingUtilities.invokeLater(() -> chatPane.setCaretPosition(chatPane.getDocument().getLength()));
        }
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
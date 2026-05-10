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
    private final JTextField searchLangField = new JTextField(10);
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
        initTelegram();
        loadLanguages();
        loadCustomers();
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

        // Thanh tìm kiếm và nút tải lại
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        searchUserField.setFont(unicodeFont);
        searchUserField.setToolTipText("Tìm kiếm người dùng...");
        topPanel.add(new JLabel("Tìm user: "), BorderLayout.WEST);
        topPanel.add(searchUserField, BorderLayout.CENTER);
        topPanel.add(refreshCustomerButton, BorderLayout.SOUTH);

        // Danh sách người dùng
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

        // 1. TOP PANEL: Chức năng cấu hình & Đăng nhập
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchLangField.setFont(unicodeFont);
        searchLangField.setToolTipText("Tìm NN");
        languageBox.setFont(unicodeFont);

        topPanel.add(new JLabel("Tìm ngôn ngữ:"));
        topPanel.add(searchLangField);
        topPanel.add(languageBox);
        topPanel.add(translateAllButton);
        topPanel.add(reloadChatButton);

        // Căn nút Login sang góc phải bằng cách lót một RigidArea hoặc dùng Box
        JPanel wrapperTop = new JPanel(new BorderLayout());
        wrapperTop.add(topPanel, BorderLayout.CENTER);
        JPanel rightLoginPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightLoginPanel.add(showLoginButton);
        wrapperTop.add(rightLoginPanel, BorderLayout.EAST);

        // 2. CENTER PANEL: Vùng hiển thị Chat (Dùng HTML để hiển thị dạng bong bóng)
        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.setFont(unicodeFont);
        JScrollPane chatScroll = new JScrollPane(chatPane);
        resetChatHtml();

        // 3. BOTTOM PANEL: Nhập liệu và Gửi tin
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

        // Hàng 1: Ô nhập tin nhắn gốc + Các nút
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0.5;
        bottomPanel.add(new JScrollPane(messageField), gbc);

        JPanel btnPanel1 = new JPanel(new GridLayout(2, 1, 0, 5));
        btnPanel1.add(translateButton);
        btnPanel1.add(sendOriginalButton);
        gbc.gridx = 1; gbc.weightx = 0;
        bottomPanel.add(btnPanel1, gbc);

        // Hàng 2: Ô hiển thị tin nhắn dịch + Nút gửi
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
        // Nút Đăng nhập mở Dialog
        showLoginButton.addActionListener(e -> {
            LoginDialog dialog = new LoginDialog(this, telegramService, unicodeFont);
            dialog.setVisible(true);
        });

        // Tải lại danh sách
        refreshCustomerButton.addActionListener(e -> loadCustomers());

        // Tìm kiếm User (Lọc trực tiếp)
        searchUserField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterUsers(); }
            public void removeUpdate(DocumentEvent e) { filterUsers(); }
            public void changedUpdate(DocumentEvent e) { filterUsers(); }
        });

        // Tìm kiếm ngôn ngữ (Lọc combobox)
        searchLangField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterLanguages(); }
            public void removeUpdate(DocumentEvent e) { filterLanguages(); }
            public void changedUpdate(DocumentEvent e) { filterLanguages(); }
        });

        // Click chọn User -> Load Chat
        customerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedChat();
            }
        });

        reloadChatButton.addActionListener(e -> loadSelectedChat());

        // Dịch toàn bộ sang Tiếng Việt
        translateAllButton.addActionListener(e -> translateAllChatToVietnamese());

        // Nhóm gửi/dịch tin nhắn
        translateButton.addActionListener(e -> translateMessage());
        sendOriginalButton.addActionListener(e -> sendOriginal());
        sendTranslatedButton.addActionListener(e -> sendTranslated());

        // Phím tắt Ctrl + Enter để dịch
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
        resetChatHtml(); // Xoá màn hình chat hiện tại
        telegramService.loadChatHistory(currentChatId);
    }

    private void translateMessage() {
        try {
            String text = messageField.getText();
            if (text == null || text.isBlank()) return;

            Object selected = languageBox.getSelectedItem();
            if (selected == null) return;

            String langCode = languageMap.get(selected.toString());
            // Dịch từ Tiếng Việt (vi) sang ngôn ngữ được chọn
            String translated = translateService.translateText(text, "vi", langCode);
            translatedField.setText(translated);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi dịch thuật");
            e.printStackTrace();
        }
    }

    private void translateAllChatToVietnamese() {
        // Chạy trên luồng phụ để tránh đơ giao diện
        new Thread(() -> {
            try {
                // Lấy toàn bộ nội dung text hiện tại. Trong thực tế nếu cần dịch toàn bộ
                // cần bóc tách từng message. Ở mức độ đơn giản ta lấy text hiển thị.
                // Do chatPane dùng HTML, ta cần viết hàm parsing hoặc yêu cầu load lại lịch sử.
                // Cách an toàn: Xoá khung chat hiện tại, gọi lại API chat nhưng qua màng lọc dịch.

                SwingUtilities.invokeLater(() -> {
                    resetChatHtml();
                    appendChatMessage("Hệ thống", "<i>Đang tải và dịch toàn bộ tin nhắn...</i>");
                });

                telegramService.setHistoryListener((chatId, sender, message) -> {
                    if (chatId != currentChatId) return;
                    // Dịch tự động ngôn ngữ khác sang tiếng việt ("auto" -> "vi")
                    String translatedMsg = translateService.translateText(message, "auto", "vi");
                    SwingUtilities.invokeLater(() -> appendChatMessage(sender, translatedMsg));
                });

                telegramService.loadChatHistory(currentChatId);

                // Sau khi load xong, trả lại listener cũ để nhận tin nhắn realtime bình thường
                Thread.sleep(3000); // Đợi load xong lịch sử
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
        appendChatMessage("Tôi", text);
        messageField.setText("");
    }

    private void sendTranslated() {
        String text = translatedField.getText();
        if (text == null || text.isBlank() || currentChatId == 0) return;

        telegramService.sendMessage(currentChatId, text);
        appendChatMessage("Tôi", text);
        translatedField.setText("");
        messageField.setText("");
    }

    // ======================================
    // HIỂN THỊ CHAT BẰNG HTML (BONG BÓNG)
    // ======================================
    private void resetChatHtml() {
        chatHtmlBuilder = new StringBuilder();
        chatHtmlBuilder.append("<html><body style='font-family: sans-serif; font-size: 13px; margin: 10px;'>");
        chatPane.setText(chatHtmlBuilder.toString() + "</body></html>");
    }

    private void appendChatMessage(String sender, String text) {
        String escapedText = text.replace("\n", "<br>").replace("<", "&lt;").replace(">", "&gt;");

        if (sender.equals("Tôi")) {
            // Tin nhắn gửi (Bên phải, màu xanh)
            chatHtmlBuilder.append("<div style='text-align: right; margin-bottom: 8px;'>")
                    .append("<span style='background-color: #DCF8C6; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%; text-align: left;'>")
                    .append(escapedText)
                    .append("</span></div>");
        } else if (sender.equals("Hệ thống")) {
            // Thông báo hệ thống
            chatHtmlBuilder.append("<div style='text-align: center; margin-bottom: 8px; color: gray;'>")
                    .append(escapedText)
                    .append("</div>");
        } else {
            // Tin nhắn nhận (Bên trái, màu xám nhạt)
            chatHtmlBuilder.append("<div style='text-align: left; margin-bottom: 8px;'>")
                    .append("<span style='background-color: #F1F0F0; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%;'>")
                    .append("<b>").append(sender).append("</b><br>")
                    .append(escapedText)
                    .append("</span></div>");
        }

        chatPane.setText(chatHtmlBuilder.toString() + "</body></html>");
        // Cuộn xuống cuối
        SwingUtilities.invokeLater(() -> chatPane.setCaretPosition(chatPane.getDocument().getLength()));
    }

    // ======================================
    // LỚP DIALOG ĐĂNG NHẬP (INNER CLASS)
    // ======================================
    class LoginDialog extends JDialog {
        private JTextField phoneField = new JTextField(15);
        private JTextField otpField = new JTextField(15);
        private JTextField passwordField = new JTextField(15);

        public LoginDialog(JFrame parent, TelegramService telegramService, Font font) {
            super(parent, "Đăng nhập Telegram", true);
            setLayout(new GridBagLayout());
            setSize(350, 250);
            setLocationRelativeTo(parent);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            phoneField.setFont(font);
            otpField.setFont(font);
            passwordField.setFont(font);

            JButton loginBtn = new JButton("Gửi SĐT");
            JButton otpBtn = new JButton("Gửi OTP");
            JButton passBtn = new JButton("Gửi 2FA Pass");

            // Row 0: Phone
            gbc.gridx = 0; gbc.gridy = 0;
            add(new JLabel("SĐT:"), gbc);
            gbc.gridx = 1; add(phoneField, gbc);
            gbc.gridx = 2; add(loginBtn, gbc);

            // Row 1: OTP
            gbc.gridx = 0; gbc.gridy = 1;
            add(new JLabel("OTP:"), gbc);
            gbc.gridx = 1; add(otpField, gbc);
            gbc.gridx = 2; add(otpBtn, gbc);

            // Row 2: 2FA
            gbc.gridx = 0; gbc.gridy = 2;
            add(new JLabel("2FA:"), gbc);
            gbc.gridx = 1; add(passwordField, gbc);
            gbc.gridx = 2; add(passBtn, gbc);

            // Hành động
            loginBtn.addActionListener(e ->
                    telegramService.resetSessionAndLogin(phoneField.getText().replaceAll("^0", "+84"))
            );

            otpBtn.addActionListener(e ->
                    telegramService.checkCode(otpField.getText())
            );

            passBtn.addActionListener(e -> {
                telegramService.checkPassword(passwordField.getText());
                dispose(); // Tắt popup sau khi nhập pass
            });
        }
    }
}
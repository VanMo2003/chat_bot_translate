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
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class MainFrame extends JFrame {

    private final Font unicodeFont = FontUtils.getFont(15f);
    private final Font inputFont = new Font("Segoe UI", Font.PLAIN, 15);
    private final String HTML_FONT_CSS = "font-family: 'Noto Sans', 'Noto Sans Thai', 'Segoe UI', Tahoma, sans-serif;";

    private final TelegramService telegramService = new TelegramService();
    private final LibreTranslateService translateService = new LibreTranslateService();

    private final Map<String, String> languageMap = new LinkedHashMap<>();
    private final List<Customer> allCustomers = new ArrayList<>();
    private final Map<Long, Integer> unreadCounts = new LinkedHashMap<>();

    private final Map<Long, StringBuilder> chatHtmlCache = new HashMap<>();
    private final Map<Long, String> originalMessages = new ConcurrentHashMap<>();
    private final Map<Long, String> messageSenders = new ConcurrentHashMap<>();
    private long currentChatId = 0;

    private final DefaultListModel<Customer> customerListModel = new DefaultListModel<>();
    private final JList<Customer> customerList = new JList<>(customerListModel);
    private final JTextField searchUserField = new JTextField();
    private final JButton refreshCustomerButton = new JButton("🔄 Tải lại User");

    private final JTextField searchLangField = new JTextField();
    private final JComboBox<String> languageBox = new JComboBox<>();
    private final JButton reloadChatButton = new JButton("Tải lại Chat");
    private final JButton showLoginButton = new JButton("Đăng nhập");

    private final JTextPane chatPane = new JTextPane();

    private final JTextArea messageField = new JTextArea(3, 20);
    private final JTextArea translatedField = new JTextArea(3, 20);
    private final JButton translateButton = new JButton("Dịch (Ctrl+Enter)");
    private final JButton sendOriginalButton = new JButton("Gửi gốc");
    private final JButton sendTranslatedButton = new JButton("Gửi bản dịch");

    public MainFrame() throws Client.ExecutionException {
        System.out.println("[MainFrame] Khởi tạo Ứng dụng...");
        setTitle("Telegram Chat Bot Translate");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();

        telegramService.setOnAuthReady(() -> {
            System.out.println("[MainFrame] Nhận tín hiệu AuthReady, bắt đầu tải Customers...");
            SwingUtilities.invokeLater(this::loadCustomers);
        });

        initTelegram();
        loadLanguages();
        System.out.println("[MainFrame] Hoàn tất khởi tạo cơ bản.");
    }

    private void initUI() {
        System.out.println("[MainFrame] Đang vẽ UI...");
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
        searchLangField.setPreferredSize(new Dimension(120, 30));

        languageBox.setFont(unicodeFont);
        // ĐÃ SỬA: Tăng kích thước hộp chọn ngôn ngữ để hiển thị đủ chữ
        languageBox.setPreferredSize(new Dimension(250, 30));

        topPanel.add(new JLabel("Tìm kiếm:"));
        topPanel.add(searchLangField);
        topPanel.add(languageBox);
        topPanel.add(reloadChatButton);

        JPanel wrapperTop = new JPanel(new BorderLayout());
        wrapperTop.add(topPanel, BorderLayout.CENTER);
        JPanel rightLoginPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightLoginPanel.add(showLoginButton);
        wrapperTop.add(rightLoginPanel, BorderLayout.EAST);

        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        chatPane.setText("<html><body style=\"" + HTML_FONT_CSS + " font-size: 14px; margin: 10px; color: gray;\"><i>Chọn người dùng để bắt đầu...</i></body></html>");

        chatPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                if (desc != null && desc.startsWith("trans:")) {
                    String[] parts = desc.split(":");
                    long chatId = Long.parseLong(parts[1]);
                    long msgId = Long.parseLong(parts[2]);
                    System.out.println("[MainFrame] Click Nút Dịch cho MsgID: " + msgId + " ở ChatID: " + chatId);
                    translateSingleMessage(chatId, msgId);
                }
            }
        });

        JScrollPane chatScroll = new JScrollPane(chatPane);

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;

        messageField.setFont(inputFont);
        messageField.setLineWrap(true);
        messageField.setWrapStyleWord(true);

        translatedField.setFont(inputFont);
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
            System.out.println("[MainFrame] Mở Form Đăng Nhập...");
            LoginDialog dialog = new LoginDialog(this, unicodeFont);
            dialog.setVisible(true);
        });

        refreshCustomerButton.addActionListener(e -> {
            System.out.println("[MainFrame] Click Button Tải Lại Danh Sách User.");
            loadCustomers();
        });

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
                Customer selected = customerList.getSelectedValue();
                if (selected != null && selected.getChatId() != currentChatId) {
                    System.out.println("[MainFrame] Chọn User trên List...");
                    loadSelectedChat();
                }
            }
        });

        reloadChatButton.addActionListener(e -> {
            System.out.println("[MainFrame] Click Button Xóa Cache & Reload Chat.");
            if (currentChatId != 0) {
                chatHtmlCache.remove(currentChatId);
                loadHistory(currentChatId);
            }
        });

        translateButton.addActionListener(e -> {
            System.out.println("[MainFrame] Click Button Dịch Ô Text.");
            translateMessage();
        });

        sendOriginalButton.addActionListener(e -> {
            System.out.println("[MainFrame] Click Button Gửi Bản Gốc.");
            sendOriginal();
        });

        sendTranslatedButton.addActionListener(e -> {
            System.out.println("[MainFrame] Click Button Gửi Bản Dịch.");
            sendTranslated();
        });

        messageField.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "translate");
        messageField.getActionMap().put("translate", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.out.println("[MainFrame] Phím tắt Ctrl+Enter: Dịch.");
                translateMessage();
            }
        });
    }

    private void initTelegram() throws Client.ExecutionException {
        telegramService.setMessageListener((chatId, sender, messageText) -> {
            long msgId = System.nanoTime();
            System.out.println("[MainFrame] Xử lý hiển thị tin nhắn mới. ID: " + msgId + " từ ChatID: " + chatId);

            SwingUtilities.invokeLater(() -> {
                if (chatHtmlCache.containsKey(chatId)) {
                    appendBubbleToBuilder(chatId, msgId, sender, messageText, false);
                    if ("Khách".equals(sender)) {
                        System.out.println("[MainFrame] Kích hoạt auto dịch cho Khách gửi, MsgID: " + msgId);
                        translateSingleMessage(chatId, msgId);
                    }
                }

                moveToTopOrAddNewUser(chatId, sender);
            });
        });

        telegramService.init();
    }

    private void moveToTopOrAddNewUser(long chatId, String sender) {
        Customer found = null;
        for (Customer c : allCustomers) {
            if (c.getChatId() == chatId) {
                found = c;
                break;
            }
        }

        if (found != null) {
            System.out.println("[MainFrame] Đẩy User có ChatID " + chatId + " lên đầu danh sách.");
            allCustomers.remove(found);
            allCustomers.add(0, found);
            updateUnreadAndRefreshList(chatId, sender);
        } else {
            System.out.println("[MainFrame] Nhận được tin nhắn từ người lạ ChatID " + chatId + ". Đang gọi API lấy tên...");
            telegramService.getChatTitle(chatId, title -> {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("[MainFrame] Đã lấy được tên: " + title + ". Thêm vào đầu danh sách.");
                    Customer newCustomer = new Customer(chatId, title, "en");
                    allCustomers.add(0, newCustomer);
                    updateUnreadAndRefreshList(chatId, sender);
                });
            });
        }
    }

    private void updateUnreadAndRefreshList(long chatId, String sender) {
        if (chatId != currentChatId && "Khách".equals(sender)) {
            unreadCounts.put(chatId, unreadCounts.getOrDefault(chatId, 0) + 1);
        }

        Customer selected = customerList.getSelectedValue();
        filterUsers();

        if (selected != null) {
            customerList.setSelectedValue(selected, true);
        }
    }

    private void loadLanguages() {
        System.out.println("[MainFrame] Đang nạp danh sách ngôn ngữ vào Giao Diện...");
        languageMap.clear();
        languageMap.putAll(translateService.getSupportedLanguages());
        filterLanguages();
    }

    private void filterLanguages() {
        String query = removeAccents(searchLangField.getText().toLowerCase());
        languageBox.removeAllItems();
        for (String name : languageMap.keySet()) {
            String nameNormalized = removeAccents(name.toLowerCase());
            if (nameNormalized.contains(query)) {
                languageBox.addItem(name);
            }
        }
        if (languageBox.getItemCount() > 0) languageBox.setSelectedIndex(0);
    }

    private void loadCustomers() {
        System.out.println("[MainFrame] Bắt đầu lấy thông tin Khách hàng từ Telegram...");
        refreshCustomerButton.setEnabled(false);
        refreshCustomerButton.setText("Đang tải...");

        telegramService.getCustomers(customers -> SwingUtilities.invokeLater(() -> {
            allCustomers.clear();
            allCustomers.addAll(customers);
            filterUsers();
            refreshCustomerButton.setEnabled(true);
            refreshCustomerButton.setText("🔄 Tải lại User");
            System.out.println("[MainFrame] Đã làm mới UI Danh sách người dùng (" + customers.size() + ").");
        }));
    }

    private String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private void filterUsers() {
        String query = removeAccents(searchUserField.getText().toLowerCase());
        customerListModel.clear();
        for (Customer c : allCustomers) {
            String nameNormalized = removeAccents(c.getName().toLowerCase());
            if (nameNormalized.contains(query)) {
                customerListModel.addElement(c);
            }
        }
    }

    private void loadSelectedChat() {
        Customer customer = customerList.getSelectedValue();
        if (customer == null) return;

        currentChatId = customer.getChatId();
        System.out.println("[MainFrame] Người dùng đã chọn ChatID: " + currentChatId + " - " + customer.getName());

        unreadCounts.put(currentChatId, 0);
        customerList.repaint();

        loadHistory(currentChatId);
    }

    private void loadHistory(long chatId) {
        if (chatHtmlCache.containsKey(chatId)) {
            System.out.println("[MainFrame] Lấy HTML từ CACHE cho ChatID: " + chatId);
            StringBuilder builder = chatHtmlCache.get(chatId);
            chatPane.setText(builder.toString() + "</body></html>");
            SwingUtilities.invokeLater(() -> chatPane.setCaretPosition(chatPane.getDocument().getLength()));
            return;
        }

        System.out.println("[MainFrame] Đang yêu cầu API History cho ChatID: " + chatId);
        StringBuilder newBuilder = new StringBuilder();
        newBuilder.append("<html><body style=\"").append(HTML_FONT_CSS).append(" font-size: 14px; margin: 10px;\">");
        chatHtmlCache.put(chatId, newBuilder);

        if (chatId == currentChatId) {
            chatPane.setText(newBuilder.toString() + "<div style='text-align: center; color: gray;'><i>Đang tải lịch sử...</i></div></body></html>");
        }

        telegramService.getRawChatHistory(chatId, messages -> {
            SwingUtilities.invokeLater(() -> {
                System.out.println("[MainFrame] Bắt đầu render " + messages.messages.length + " tin nhắn lên HTML...");
                if (messages.messages.length == 0) {
                    if (chatId == currentChatId) {
                        chatPane.setText(newBuilder.toString() + "</body></html>");
                    }
                    return;
                }

                for (int i = 0; i < messages.messages.length; i++) {
                    TdApi.Message message = messages.messages[i];
                    if (message.content instanceof TdApi.MessageText textMessage) {
                        String sender = message.isOutgoing ? "Tôi" : "Khách";
                        String text = textMessage.text.text;
                        appendBubbleToBuilder(chatId, message.id, sender, text, true);
                    }
                }
                System.out.println("[MainFrame] Render lịch sử hoàn tất.");
            });
        });
    }

    private void appendBubbleToBuilder(long targetChatId, long messageId, String sender, String text, boolean isPrepend) {
        StringBuilder builder = chatHtmlCache.get(targetChatId);
        if (builder == null) return;

        String escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        StringBuilder bubble = new StringBuilder();

        if (sender.equals("Hệ thống")) {
            bubble.append("<div style='text-align: center; margin-bottom: 8px; color: gray;'>")
                    .append(escapedText)
                    .append("</div>");
        } else {
            originalMessages.put(messageId, text);
            messageSenders.put(messageId, sender);

            String align = sender.equals("Tôi") ? "right" : "left";
            String linkHtml = "<div id='link_" + messageId + "' style='margin-top: 5px; text-align: " + align + ";'><a href='trans:" + targetChatId + ":" + messageId + "' style='color: #0066cc; text-decoration: none; font-size: 12px;'>[Dịch]</a></div>";

            if (sender.equals("Tôi")) {
                bubble.append("<div style='text-align: right; margin-bottom: 8px;'>")
                        .append("<span style='background-color: #DCF8C6; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%; text-align: left;'>")
                        .append(escapedText)
                        .append(linkHtml)
                        .append("</span></div>");
            } else {
                bubble.append("<div style='text-align: left; margin-bottom: 8px;'>")
                        .append("<span style='background-color: #F1F0F0; padding: 8px 12px; border-radius: 15px; display: inline-block; max-width: 70%; text-align: left;'>")
                        .append("<b>").append(sender).append("</b><br>")
                        .append(escapedText)
                        .append(linkHtml)
                        .append("</span></div>");
            }
        }

        if (isPrepend) {
            String anchor = "margin: 10px;\">";
            int insertIndex = builder.indexOf(anchor);
            if (insertIndex != -1) {
                builder.insert(insertIndex + anchor.length(), bubble.toString());
            } else {
                builder.append(bubble.toString());
            }
        } else {
            builder.append(bubble.toString());
        }

        if (targetChatId == currentChatId) {
            chatPane.setText(builder.toString() + "</body></html>");
            if (!isPrepend) {
                SwingUtilities.invokeLater(() -> chatPane.setCaretPosition(chatPane.getDocument().getLength()));
            }
        }
    }

    private void translateSingleMessage(long chatId, long msgId) {
        System.out.println("[MainFrame] Bắt đầu dịch độc lập cho MsgID: " + msgId);
        String originalText = originalMessages.get(msgId);
        if (originalText == null) {
            System.err.println("[MainFrame] Lỗi: Không tìm thấy Text gốc cho MsgID: " + msgId);
            return;
        }

        StringBuilder builder = chatHtmlCache.get(chatId);
        if (builder == null) return;

        String sender = messageSenders.getOrDefault(msgId, "Khách");
        String align = sender.equals("Tôi") ? "right" : "left";

        String linkHtml = "<div id='link_" + msgId + "' style='margin-top: 5px; text-align: " + align + ";'><a href='trans:" + chatId + ":" + msgId + "' style='color: #0066cc; text-decoration: none; font-size: 12px;'>[Dịch]</a></div>";
        String loadingHtml = "<div id='link_" + msgId + "' style='margin-top: 5px; text-align: " + align + ";'><i style='color: gray; font-size: 12px;'>Đang dịch...</i></div>";

        int idx = builder.indexOf(linkHtml);
        if (idx != -1) {
            builder.replace(idx, idx + linkHtml.length(), loadingHtml);
            if (chatId == currentChatId) {
                int currentCaret = chatPane.getCaretPosition();
                chatPane.setText(builder.toString() + "</body></html>");
                chatPane.setCaretPosition(Math.min(currentCaret, chatPane.getDocument().getLength()));
            }
        }

        new Thread(() -> {
            String translated = translateService.translateText(originalText, "auto", "vi");
            String escapedTranslated = translated.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");

            String resultHtml = "<div style='margin-top: 5px; border-top: 1px dashed #ccc; padding-top: 5px; color: #b30000; text-align: " + align + ";'>" + escapedTranslated + "</div>";

            SwingUtilities.invokeLater(() -> {
                StringBuilder b = chatHtmlCache.get(chatId);
                if (b != null) {
                    int i = b.indexOf(loadingHtml);
                    if (i != -1) {
                        b.replace(i, i + loadingHtml.length(), resultHtml);
                        System.out.println("[MainFrame] Đã đính bản dịch vào UI cho MsgID: " + msgId);
                        if (chatId == currentChatId) {
                            int caret = chatPane.getCaretPosition();
                            chatPane.setText(b.toString() + "</body></html>");
                            chatPane.setCaretPosition(Math.min(caret, chatPane.getDocument().getLength()));
                        }
                    }
                }
            });
        }).start();
    }

    private void translateMessage() {
        System.out.println("[MainFrame] Đang gọi API dịch văn bản chuẩn bị gửi...");
        try {
            String text = messageField.getText();
            if (text == null || text.isBlank()) return;

            Object selected = languageBox.getSelectedItem();
            if (selected == null) return;

            String langCode = languageMap.get(selected.toString());
            String translated = translateService.translateText(text, "vi", langCode);
            translatedField.setText(translated);

        } catch (Exception e) {
            System.err.println("[MainFrame] Lỗi tại translateMessage():");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi dịch thuật");
        }
    }

    private void sendOriginal() {
        System.out.println("[MainFrame] Gọi Service gửi văn bản Gốc...");
        String text = messageField.getText();
        if (text == null || text.isBlank() || currentChatId == 0) return;

        telegramService.sendMessage(currentChatId, text);
        messageField.setText("");
    }

    private void sendTranslated() {
        System.out.println("[MainFrame] Gọi Service gửi văn bản Dịch...");
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

        public LoginDialog(JFrame parent, Font font) {
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

            loginBtn.addActionListener(e -> {
                System.out.println("[MainFrame] Dialog: Submit Phone");
                telegramService.resetSessionAndLogin(phoneField.getText().replaceAll("^0", "+84"));
            });

            otpBtn.addActionListener(e -> {
                System.out.println("[MainFrame] Dialog: Submit OTP");
                telegramService.checkCode(otpField.getText());
            });

            passBtn.addActionListener(e -> {
                System.out.println("[MainFrame] Dialog: Submit 2FA");
                telegramService.checkPassword(passwordField.getText());
                dispose();
            });
        }
    }
}
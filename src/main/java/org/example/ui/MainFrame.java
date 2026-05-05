package org.example.ui;

import org.drinkless.tdlib.Client;
import org.example.telegram.TelegramService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final TelegramService telegramService =
            new TelegramService();

    private final JTextArea logArea =
            new JTextArea();

    private final JTextArea chatArea =
            new JTextArea();

    private final JTextField phoneField =
            new JTextField();

    private final JTextField otpField =
            new JTextField();

    private final JTextField passwordField =
            new JTextField();

    private final JTextField chatIdField =
            new JTextField();

    private final JTextField messageField =
            new JTextField();

    private final JButton loginButton =
            new JButton("Login");

    private final JButton otpButton =
            new JButton("Send OTP");

    private final JButton passwordButton =
            new JButton("2FA");

    private final JButton loadChatButton =
            new JButton("Load Chat");

    private final JButton sendButton =
            new JButton("Send Message");

    private long currentChatId = 0;

    public MainFrame() throws Client.ExecutionException {

        setTitle("Telegram CSKH");

        setSize(1000, 700);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        initUI();

        initTelegram();
    }

    private void initUI() {

        setLayout(new BorderLayout());

        JPanel topPanel =
                new JPanel(new GridLayout(5, 3, 10, 10));

        // PHONE
        topPanel.add(
                new JLabel("Phone")
        );

        topPanel.add(phoneField);

        topPanel.add(loginButton);

        // OTP
        topPanel.add(
                new JLabel("OTP")
        );

        topPanel.add(otpField);

        topPanel.add(otpButton);

        // 2FA
        topPanel.add(
                new JLabel("2FA Password")
        );

        topPanel.add(passwordField);

        topPanel.add(passwordButton);

        // CHAT ID
        topPanel.add(
                new JLabel("Chat ID")
        );

        topPanel.add(chatIdField);

        topPanel.add(loadChatButton);

        // MESSAGE
        topPanel.add(
                new JLabel("Message")
        );

        topPanel.add(messageField);

        topPanel.add(sendButton);

        add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane =
                new JSplitPane(
                        JSplitPane.VERTICAL_SPLIT
                );

        logArea.setEditable(false);

        chatArea.setEditable(false);

        splitPane.setTopComponent(
                new JScrollPane(logArea)
        );

        splitPane.setBottomComponent(
                new JScrollPane(chatArea)
        );

        splitPane.setDividerLocation(250);

        add(splitPane, BorderLayout.CENTER);

        // LOGIN BUTTON
        loginButton.addActionListener(e -> {

            telegramService.setPhoneNumber(
                    phoneField.getText()
            );
        });

        // OTP BUTTON
        otpButton.addActionListener(e -> {

            telegramService.checkCode(
                    otpField.getText()
            );
        });

        // PASSWORD BUTTON
        passwordButton.addActionListener(e -> {

            telegramService.checkPassword(
                    passwordField.getText()
            );
        });

        // LOAD CHAT BUTTON
        loadChatButton.addActionListener(e -> {

            loadChat();
        });

        // SEND BUTTON
        sendButton.addActionListener(e -> {

            sendMessage();
        });

        // ENTER TO SEND
        messageField.addActionListener(e -> {

            sendMessage();
        });
    }

    private void initTelegram()
            throws Client.ExecutionException {

        // LOG
        telegramService.setLogListener(log -> {

            SwingUtilities.invokeLater(() -> {

                logArea.append(
                        log + "\n"
                );
            });
        });

        // REALTIME MESSAGE
        telegramService.setMessageListener(

                (chatId, sender, message) -> {

                    if (chatId != currentChatId) {
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {

                        chatArea.append(
                                sender
                                        + ": "
                                        + message
                                        + "\n"
                        );
                    });
                }
        );

        // HISTORY MESSAGE
        telegramService.setHistoryListener(

                (chatId, sender, message) -> {

                    if (chatId != currentChatId) {
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {

                        chatArea.append(
                                sender
                                        + ": "
                                        + message
                                        + "\n"
                        );
                    });
                }
        );

        telegramService.init();
    }

    private void loadChat() {

        try {

            currentChatId =
                    Long.parseLong(
                            chatIdField.getText()
                    );

            chatArea.setText("");

            telegramService.loadChatHistory(
                    currentChatId
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Chat ID không hợp lệ"
            );
        }
    }

    private void sendMessage() {

        try {

            long chatId =
                    Long.parseLong(
                            chatIdField.getText()
                    );

            String text =
                    messageField.getText();

            if (text.isBlank()) {
                return;
            }

            telegramService.sendMessage(
                    chatId,
                    text
            );

            chatArea.append(
                    "Tôi: "
                            + text
                            + "\n"
            );

            messageField.setText("");

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Chat ID không hợp lệ"
            );
        }
    }
}

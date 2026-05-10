package org.example.telegram;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.example.listener.HistoryListener;
import org.example.listener.MessageListener;
import org.example.model.Customer;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class TelegramService {

    private static final int API_ID = 35344214;
    private static final String API_HASH = "65658305fb64c93acbf87215f2acfb7c";

    private Client client;
    private Consumer<String> logListener;
    private MessageListener messageListener;
    private HistoryListener historyListener;
    private TdApi.AuthorizationState authorizationState;
    private Runnable onAuthReady;

    static {
        System.out.println("[Telegram] Đang load thư viện tdjni...");
        System.loadLibrary("tdjni");
        System.out.println("[Telegram] Load tdjni thành công.");
    }

    public void setLogListener(Consumer<String> logListener) {
        this.logListener = logListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setHistoryListener(HistoryListener historyListener) {
        this.historyListener = historyListener;
    }

    public void setOnAuthReady(Runnable onAuthReady) {
        this.onAuthReady = onAuthReady;
    }

    private void log(String text) {
        System.out.println("[Telegram] " + text);
        if (logListener != null) {
            logListener.accept(text);
        }
    }

    public void init() throws Client.ExecutionException {
        log("Khởi tạo Client TDLib...");
        Client.execute(new TdApi.SetLogVerbosityLevel(0));
        client = Client.create(this::onUpdate, this::onError, this::onError);
    }

    private void onUpdate(TdApi.Object object) {
        if (object instanceof TdApi.UpdateAuthorizationState update) {
            handleAuth(update.authorizationState);
        } else if (object instanceof TdApi.UpdateNewMessage update) {
            handleNewMessage(update);
        }
    }

    private void handleAuth(TdApi.AuthorizationState state) {
        authorizationState = state;
        log("AUTH STATE CHUYỂN SANG => " + state.getClass().getSimpleName());

        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            log("Đang gửi TdlibParameters...");
            TdApi.SetTdlibParameters params = new TdApi.SetTdlibParameters();
            params.databaseDirectory = "tdlib";
            params.useMessageDatabase = true;
            params.useSecretChats = true;
            params.useFileDatabase = true;
            params.useChatInfoDatabase = true;
            params.apiId = API_ID;
            params.apiHash = API_HASH;
            params.systemLanguageCode = "en";
            params.deviceModel = "Desktop";
            params.applicationVersion = "1.0";
            client.send(params, this::onResult);
        } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            log("YÊU CẦU: Nhập số điện thoại");
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            log("YÊU CẦU: Nhập mã OTP");
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            log("YÊU CẦU: Nhập mật khẩu 2FA");
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            log("ĐĂNG NHẬP THÀNH CÔNG (READY)");
            if (onAuthReady != null) {
                log("Kích hoạt callback onAuthReady...");
                onAuthReady.run();
            }
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            log("TDLIB ĐÃ ĐÓNG");
        }
    }

    public void getCustomers(Consumer<List<Customer>> callback) {
        log("Đang yêu cầu lấy danh sách Chat (Limit: 100)...");
        client.send(new TdApi.GetChats(null, 100), object -> {
            List<Customer> customers = new ArrayList<>();
            if (!(object instanceof TdApi.Chats chats)) {
                log("Lỗi: Không nhận được danh sách Chats. Trả về: " + object);
                callback.accept(customers);
                return;
            }
            long[] chatIds = chats.chatIds;
            log("Nhận được " + chatIds.length + " chatIds. Đang tải chi tiết từng Chat...");
            if (chatIds.length == 0) {
                callback.accept(customers);
                return;
            }

            final int[] loadedCount = {0};
            for (long chatId : chatIds) {
                client.send(new TdApi.GetChat(chatId), chatObject -> {
                    try {
                        if (chatObject instanceof TdApi.Chat chat) {
                            if (!(chat.type instanceof TdApi.ChatTypePrivate)) {
                                return;
                            }
                            customers.add(new Customer(chat.id, chat.title, "en"));
                        }
                    } finally {
                        loadedCount[0]++;
                        if (loadedCount[0] >= chatIds.length) {
                            log("Đã tải xong chi tiết toàn bộ người dùng. Tổng cộng: " + customers.size() + " Private Chats.");
                            callback.accept(customers);
                        }
                    }
                });
            }
        });
    }

    private void handleNewMessage(TdApi.UpdateNewMessage update) {
        TdApi.Message message = update.message;
        long chatId = message.chatId;

        if (message.content instanceof TdApi.MessageText textMessage) {
            String text = textMessage.text.text;
            String sender = message.isOutgoing ? "Tôi" : "Khách";
            log("Nhận tin nhắn mới: [ChatId: " + chatId + "] " + sender + ": " + text);

            if (messageListener != null) {
                messageListener.onMessage(chatId, sender, text);
            }
        } else {
            log("Nhận tin nhắn mới nhưng không phải dạng Text (Video/Photo/Sticker...). Bỏ qua.");
        }
    }

    public void setPhoneNumber(String phone) {
        log("Gửi SĐT lên Telegram: " + phone);
        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitPhoneNumber)) {
            log("Lỗi: Telegram chưa sẵn sàng nhận SĐT.");
            return;
        }
        client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), this::onResult);
    }

    public void resetSessionAndLogin(String phone) {
        log("Bắt đầu quy trình Reset Session và Đăng nhập mới...");
        try {
            if (client != null) {
                log("Đang đóng Client cũ...");
                CountDownLatch latch = new CountDownLatch(1);
                client.send(new TdApi.Close(), object -> latch.countDown());
                latch.await();
            }
        } catch (Exception e) {
            System.err.println("[Telegram] Lỗi khi đóng Client:");
            e.printStackTrace();
        }

        deleteDirectory(new File("tdlib"));
        log("Đã xóa hoàn toàn thư mục Session cũ (tdlib).");

        try {
            init();
            new Thread(() -> {
                log("Luồng chờ nhập SĐT bắt đầu...");
                while (!(authorizationState instanceof TdApi.AuthorizationStateWaitPhoneNumber)) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                setPhoneNumber(phone);
            }).start();
        } catch (Exception e) {
            System.err.println("[Telegram] Lỗi khi khởi tạo lại Session:");
            e.printStackTrace();
        }
    }

    public void checkCode(String code) {
        log("Gửi mã OTP lên Telegram: " + code);
        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitCode)) return;
        client.send(new TdApi.CheckAuthenticationCode(code), this::onResult);
    }

    public void checkPassword(String password) {
        log("Gửi mật khẩu 2FA lên Telegram...");
        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitPassword)) return;
        client.send(new TdApi.CheckAuthenticationPassword(password), this::onResult);
    }

    public void sendMessage(long chatId, String text) {
        log("Gửi tin nhắn đi: [ChatId: " + chatId + "] Text: " + text);
        TdApi.InputMessageContent content = new TdApi.InputMessageText(
                new TdApi.FormattedText(text, null), null, false
        );
        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = chatId;
        request.inputMessageContent = content;
        client.send(request, object -> {
            log("Kết quả gửi tin nhắn: " + object.getClass().getSimpleName());
            this.onResult(object);
        });
    }

    public void loadChatHistory(long chatId, Runnable onBeforeDispatch) {
        log("Yêu cầu LoadChatHistory (cũ): ChatId: " + chatId);
        client.send(new TdApi.OpenChat(chatId), ignored -> {
            client.send(new TdApi.GetChatHistory(chatId, 0, 0, 50, false), object -> {
                if (object instanceof TdApi.Messages messages) {
                    log("Đã load " + messages.messages.length + " tin nhắn từ Cache cục bộ.");
                    if (messages.messages.length <= 1) {
                        log("Cache thiếu tin nhắn, đợi 600ms tải từ Server...");
                        new Thread(() -> {
                            try { Thread.sleep(600); } catch (InterruptedException e) { }
                            client.send(new TdApi.GetChatHistory(chatId, 0, 0, 50, false), obj2 -> {
                                if (obj2 instanceof TdApi.Messages msgs2) {
                                    log("Đã tải xong " + msgs2.messages.length + " tin nhắn từ Server.");
                                    if (onBeforeDispatch != null) SwingUtilities.invokeLater(onBeforeDispatch);
                                    dispatchHistory(chatId, msgs2);
                                }
                            });
                        }).start();
                    } else {
                        if (onBeforeDispatch != null) SwingUtilities.invokeLater(onBeforeDispatch);
                        dispatchHistory(chatId, messages);
                    }
                } else {
                    log("Lỗi LoadChatHistory: " + object);
                }
            });
        });
    }

    public void getRawChatHistory(long chatId, Consumer<TdApi.Messages> callback) {
        log("Yêu cầu RawChatHistory: ChatId: " + chatId);
        client.send(new TdApi.OpenChat(chatId), ignored -> {
            client.send(new TdApi.GetChatHistory(chatId, 0, 0, 50, false), object -> {
                if (object instanceof TdApi.Messages messages) {
                    log("Raw: Đã load " + messages.messages.length + " tin nhắn từ Cache.");
                    if (messages.messages.length <= 1) {
                        log("Raw: Cache thiếu, đợi 600ms tải từ Server...");
                        new Thread(() -> {
                            try { Thread.sleep(600); } catch (InterruptedException e) { }
                            client.send(new TdApi.GetChatHistory(chatId, 0, 0, 50, false), obj2 -> {
                                if (obj2 instanceof TdApi.Messages msgs2) {
                                    log("Raw: Đã tải xong " + msgs2.messages.length + " tin nhắn từ Server.");
                                    callback.accept(msgs2);
                                }
                            });
                        }).start();
                    } else {
                        callback.accept(messages);
                    }
                } else {
                    log("Lỗi RawChatHistory: " + object);
                }
            });
        });
    }

    private void dispatchHistory(long chatId, TdApi.Messages messages) {
        log("Bắt đầu Dispatch History cho " + messages.messages.length + " tin nhắn...");
        for (int i = messages.messages.length - 1; i >= 0; i--) {
            TdApi.Message message = messages.messages[i];
            if (message.content instanceof TdApi.MessageText textMessage) {
                String sender = message.isOutgoing ? "Tôi" : "Khách";
                String text = textMessage.text.text;
                if (historyListener != null) {
                    historyListener.onHistoryMessage(chatId, sender, text);
                }
            }
        }
        log("Kết thúc Dispatch History.");
    }

    private void deleteDirectory(File file) {
        if (file == null || !file.exists()) return;
        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) deleteDirectory(child);
        }
        file.delete();
    }

    private void onResult(TdApi.Object object) {
        // Log nội bộ của TDLib
    }

    private void onError(Throwable throwable) {
        System.err.println("[Telegram] LỖI NGHIÊM TRỌNG:");
        throwable.printStackTrace();
    }
}
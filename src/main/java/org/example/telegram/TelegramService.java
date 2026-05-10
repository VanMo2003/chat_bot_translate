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
    private Runnable onAuthReady; // Tín hiệu báo đăng nhập thành công

    static {
        System.loadLibrary("tdjni");
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

    // Set callback khi trạng thái TDLib chuyển sang Ready
    public void setOnAuthReady(Runnable onAuthReady) {
        this.onAuthReady = onAuthReady;
    }

    private void log(String text) {
        System.out.println(text);
        if (logListener != null) {
            logListener.accept(text);
        }
    }

    public void init() throws Client.ExecutionException {
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
        log("AUTH STATE => " + state.getClass().getSimpleName());

        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
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
            log("ENTER PHONE NUMBER");
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            log("ENTER OTP CODE");
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            log("ENTER 2FA PASSWORD");
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            log("LOGIN SUCCESS");
            // Kích hoạt load tự động khi sẵn sàng
            if (onAuthReady != null) {
                onAuthReady.run();
            }
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            log("TDLIB CLOSED");
        }
    }

    public void getCustomers(Consumer<List<Customer>> callback) {
        client.send(new TdApi.GetChats(null, 100), object -> {
            List<Customer> customers = new ArrayList<>();
            if (!(object instanceof TdApi.Chats chats)) {
                callback.accept(customers);
                return;
            }
            long[] chatIds = chats.chatIds;
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

            if (messageListener != null) {
                messageListener.onMessage(chatId, sender, text);
            }
        }
    }

    public void setPhoneNumber(String phone) {
        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitPhoneNumber)) {
            log("Telegram chưa sẵn sàng nhập số điện thoại");
            return;
        }
        client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), this::onResult);
    }

    public void resetSessionAndLogin(String phone) {
        try {
            if (client != null) {
                CountDownLatch latch = new CountDownLatch(1);
                client.send(new TdApi.Close(), object -> latch.countDown());
                latch.await();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        deleteDirectory(new File("tdlib"));
        log("ĐÃ XÓA SESSION CŨ");

        try {
            init();
            new Thread(() -> {
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
            e.printStackTrace();
        }
    }

    public void checkCode(String code) {
        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitCode)) return;
        client.send(new TdApi.CheckAuthenticationCode(code), this::onResult);
    }

    public void checkPassword(String password) {
        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitPassword)) return;
        client.send(new TdApi.CheckAuthenticationPassword(password), this::onResult);
    }

    public void sendMessage(long chatId, String text) {
        TdApi.InputMessageContent content = new TdApi.InputMessageText(
                new TdApi.FormattedText(text, null), null, false
        );
        TdApi.SendMessage request = new TdApi.SendMessage();
        request.chatId = chatId;
        request.inputMessageContent = content;
        client.send(request, this::onResult);
    }

    // Đã thay đổi: Thêm OpenChat và cơ chế thử lại nếu Cache cục bộ thiếu tin nhắn
    public void loadChatHistory(long chatId, Runnable onBeforeDispatch) {
        client.send(new TdApi.OpenChat(chatId), ignored -> {
            client.send(new TdApi.GetChatHistory(chatId, 0, 0, 50, false), object -> {
                if (object instanceof TdApi.Messages messages) {

                    // Nếu TDLib chỉ trả về 1 tin (từ cache), đợi một nhịp để tải từ Server
                    if (messages.messages.length <= 1) {
                        new Thread(() -> {
                            try { Thread.sleep(600); } catch (InterruptedException e) { }
                            client.send(new TdApi.GetChatHistory(chatId, 0, 0, 50, false), obj2 -> {
                                if (obj2 instanceof TdApi.Messages msgs2) {
                                    if (onBeforeDispatch != null) SwingUtilities.invokeLater(onBeforeDispatch);
                                    dispatchHistory(chatId, msgs2);
                                }
                            });
                        }).start();
                    } else {
                        if (onBeforeDispatch != null) SwingUtilities.invokeLater(onBeforeDispatch);
                        dispatchHistory(chatId, messages);
                    }
                }
            });
        });
    }

    // Hàm phụ trợ gỡ các tin nhắn và đẩy lên giao diện
    private void dispatchHistory(long chatId, TdApi.Messages messages) {
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
        log("RESULT => " + object);
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }
}
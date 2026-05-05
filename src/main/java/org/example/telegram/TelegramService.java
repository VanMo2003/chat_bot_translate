package org.example.telegram;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.example.listener.HistoryListener;
import org.example.listener.MessageListener;

import java.util.function.Consumer;

public class TelegramService {

    private static final int API_ID = 35344214;

    private static final String API_HASH =
            "65658305fb64c93acbf87215f2acfb7c";

    private Client client;

    private Consumer<String> logListener;

    private MessageListener messageListener;

    private HistoryListener historyListener;

    private TdApi.AuthorizationState authorizationState;

    static {

        System.loadLibrary("tdjni");
    }

    // =========================
    // SET LISTENER
    // =========================

    public void setLogListener(
            Consumer<String> logListener
    ) {

        this.logListener = logListener;
    }

    public void setMessageListener(
            MessageListener messageListener
    ) {

        this.messageListener = messageListener;
    }

    public void setHistoryListener(
            HistoryListener historyListener
    ) {

        this.historyListener = historyListener;
    }

    // =========================
    // LOG
    // =========================

    private void log(
            String text
    ) {

        System.out.println(text);

        if (logListener != null) {

            logListener.accept(text);
        }
    }

    // =========================
    // INIT
    // =========================

    public void init()
            throws Client.ExecutionException {

        Client.execute(
                new TdApi.SetLogVerbosityLevel(0)
        );

        client = Client.create(

                this::onUpdate,

                this::onError,

                this::onError
        );
    }

    // =========================
    // UPDATE
    // =========================

    private void onUpdate(
            TdApi.Object object
    ) {

        if (object instanceof TdApi.UpdateAuthorizationState update) {

            handleAuth(
                    update.authorizationState
            );
        }

        else if (object instanceof TdApi.UpdateNewMessage update) {

            handleNewMessage(update);
        }
    }

    // =========================
    // AUTH
    // =========================

    private void handleAuth(
            TdApi.AuthorizationState state
    ) {

        authorizationState = state;

        log(
                "AUTH STATE => "
                        + state.getClass().getSimpleName()
        );

        // STEP 1
        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {

            TdApi.SetTdlibParameters params =
                    new TdApi.SetTdlibParameters();

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

            client.send(
                    params,
                    this::onResult
            );
        }

        // STEP 2
        else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {

            log("ENTER PHONE NUMBER");
        }

        // STEP 3
        else if (state instanceof TdApi.AuthorizationStateWaitCode) {

            log("ENTER OTP CODE");
        }

        // STEP 4
        else if (state instanceof TdApi.AuthorizationStateWaitPassword) {

            log("ENTER 2FA PASSWORD");
        }

        // SUCCESS
        else if (state instanceof TdApi.AuthorizationStateReady) {

            log("LOGIN SUCCESS");
        }

        // CLOSED
        else if (state instanceof TdApi.AuthorizationStateClosed) {

            log("TDLIB CLOSED");
        }
    }

    // =========================
    // NEW MESSAGE
    // =========================

    private void handleNewMessage(
            TdApi.UpdateNewMessage update
    ) {

        TdApi.Message message =
                update.message;

        long chatId =
                message.chatId;

        if (message.content
                instanceof TdApi.MessageText textMessage) {

            String text =
                    textMessage.text.text;

            String sender =
                    message.isOutgoing
                            ? "Tôi"
                            : "Khách";

            if (messageListener != null) {

                messageListener.onMessage(
                        chatId,
                        sender,
                        text
                );
            }
        }
    }

    // =========================
    // PHONE
    // =========================

    public void setPhoneNumber(
            String phone
    ) {

        if (!(authorizationState
                instanceof TdApi.AuthorizationStateWaitPhoneNumber)) {

            log(
                    "Telegram chưa sẵn sàng nhập số điện thoại"
            );

            return;
        }

        client.send(

                new TdApi.SetAuthenticationPhoneNumber(
                        phone,
                        null
                ),

                this::onResult
        );
    }

    // =========================
    // OTP
    // =========================

    public void checkCode(
            String code
    ) {

        if (!(authorizationState
                instanceof TdApi.AuthorizationStateWaitCode)) {

            log(
                    "Telegram chưa yêu cầu OTP"
            );

            return;
        }

        client.send(

                new TdApi.CheckAuthenticationCode(
                        code
                ),

                this::onResult
        );
    }

    // =========================
    // PASSWORD
    // =========================

    public void checkPassword(
            String password
    ) {

        if (!(authorizationState
                instanceof TdApi.AuthorizationStateWaitPassword)) {

            log(
                    "Telegram chưa yêu cầu mật khẩu 2FA"
            );

            return;
        }

        client.send(

                new TdApi.CheckAuthenticationPassword(
                        password
                ),

                this::onResult
        );
    }

    // =========================
    // SEND MESSAGE
    // =========================

    public void sendMessage(
            long chatId,
            String text
    ) {

        TdApi.InputMessageContent content =
                new TdApi.InputMessageText(

                        new TdApi.FormattedText(
                                text,
                                null
                        ),

                        null,

                        false
                );

        TdApi.SendMessage request =
                new TdApi.SendMessage();

        request.chatId = chatId;

        request.inputMessageContent =
                content;

        client.send(
                request,
                this::onResult
        );
    }

    // =========================
    // LOAD HISTORY
    // =========================

    public void loadChatHistory(
            long chatId
    ) {

        client.send(

                new TdApi.GetChatHistory(
                        chatId,
                        0,
                        0,
                        50,
                        false
                ),

                object -> {
                    System.out.println(object);
                    if (object instanceof TdApi.Messages messages) {

                        for (TdApi.Message message
                                : messages.messages) {

                            if (message.content
                                    instanceof TdApi.MessageText textMessage) {

                                String sender =
                                        message.isOutgoing
                                                ? "Tôi"
                                                : "Khách";

                                String text =
                                        textMessage.text.text;

                                if (historyListener != null) {

                                    historyListener.onHistoryMessage(
                                            chatId,
                                            sender,
                                            text
                                    );
                                }
                            }
                        }
                    }
                }
        );
    }

    // =========================
    // RESULT
    // =========================

    private void onResult(
            TdApi.Object object
    ) {

        log(
                "RESULT => "
                        + object
        );
    }

    // =========================
    // ERROR
    // =========================

    private void onError(
            Throwable throwable
    ) {

        throwable.printStackTrace();
    }
}

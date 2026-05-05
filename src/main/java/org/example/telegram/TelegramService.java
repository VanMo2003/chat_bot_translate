package org.example.telegram;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.Scanner;


public class TelegramService {
    private final Scanner scanner = new Scanner(System.in);
    private int YOUR_API_ID = 35344214;
    private String YOUR_API_HASH = "65658305fb64c93acbf87215f2acfb7c";
    private Client client;

    static {
        System.loadLibrary("tdjni");
    }

    public void init() throws Client.ExecutionException {

        Client.execute(new TdApi.SetLogVerbosityLevel(0));

        client = Client.create(
                this::onUpdate,
                this::onError,
                this::onError
        );
    }

    private void onUpdate(TdApi.Object object) {

        if (object instanceof TdApi.UpdateAuthorizationState update) {
            onAuthorizationStateUpdated(
                    update.authorizationState
            );
        }
    }

    private void onAuthorizationStateUpdated(
            TdApi.AuthorizationState state
    ) {

        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {

            TdApi.SetTdlibParameters params =
                    new TdApi.SetTdlibParameters();

            params.databaseDirectory = "tdlib";
            params.useMessageDatabase = true;
            params.useSecretChats = true;

            params.apiId = YOUR_API_ID;
            params.apiHash = YOUR_API_HASH;

            params.systemLanguageCode = "en";
            params.deviceModel = "Desktop";
            params.applicationVersion = "1.0";

            client.send(
                    params,
                    this::onResult
            );
        }

        else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {

            client.send(
                    new TdApi.SetAuthenticationPhoneNumber(
                            "+84385618713",
                            null
                    ),
                    this::onResult
            );
        }

        else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            System.out.println("ENTER OTP");

            String code = scanner.nextLine();

            client.send(
                    new TdApi.CheckAuthenticationCode(code),
                    this::onResult
            );
        }

        else if (state instanceof TdApi.AuthorizationStateWaitPassword) {

            System.out.print("ENTER PASSWORD: ");

            String password = scanner.nextLine();

            client.send(
                    new TdApi.CheckAuthenticationPassword(password),
                    this::onResult
            );
        }

        else if (state instanceof TdApi.AuthorizationStateReady) {

            System.out.println("LOGIN SUCCESS");
        }
    }

    private void onResult(TdApi.Object object) {

        System.out.println("RESULT: " + object);
    }

    private void onError(Throwable throwable) {

        throwable.printStackTrace();
    }
}

package ru.java.basic.chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private volatile boolean isRunning;

    public Client() throws IOException {
        Scanner scanner = new Scanner(System.in);
        socket = new Socket("localhost", 8189);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        isRunning = true;
        new Thread(() -> {
            try {
                while (isRunning) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exitok")) {
                            break;
                        }
                        if (message.startsWith("/kicked")) {
                            System.out.println("Вас кикнул администратор");
                            isRunning = false;
                            break;
                        }
                        if (message.startsWith("/authok ")) {
                            System.out.println("Аутентификация прошла успешно с именем пользователя: " +
                                    message.split(" ")[1]);
                        }
                        if (message.startsWith("/regok ")) {
                            System.out.println("Регистрация прошла успешно с именем пользователя: " +
                                    message.split(" ")[1]);
                        }
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();

        while (isRunning) {
            try {
                String message = scanner.nextLine();
                if (isRunning) {
                    out.writeUTF(message);
                }
                if (message.startsWith("/exit")) {
                    isRunning = false;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
            }
        }
    }

    public void disconnect() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
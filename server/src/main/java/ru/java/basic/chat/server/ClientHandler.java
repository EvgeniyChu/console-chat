package ru.java.basic.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private String role;

    private volatile boolean isRunning = true; // Флаг для контроля состояния работы клиента

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                System.out.println("Клиент подключился ");
                // Цикл аутентификации
                while (isRunning) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.startsWith("/exit")) {
                            sendMessage("/exitok");
                            isRunning = false; // Устанавливаем флаг для выхода
                            break;
                        }
                        // /auth login password
                        if (message.startsWith("/auth ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 3) {
                                sendMessage("Неверный формат команды /auth ");
                                continue;
                            }
                            if (server.getAuthenticatedProvider().authenticate(this, elements[1], elements[2])) {
                                break; // Успешная аутентификация
                            }
                            continue;
                        }
                        // /reg login password username
                        if (message.startsWith("/reg ")) {
                            String[] elements = message.split(" ");
                            if (elements.length != 5) {
                                sendMessage("Неверный формат команды /reg ");
                                continue;
                            }
                            if (server.getAuthenticatedProvider().registration(this, elements[1], elements[2], elements[3], elements[4])) {
                                break; // Успешная регистрация
                            }
                            continue;
                        }
                    }
                    sendMessage("Перед работой необходимо пройти аутентификацию командой " +
                            "/auth login password или регистрацию командой /reg login password username");
                }

                if (username != null) {
                    System.out.println("Клиент " + username + " успешно прошел аутентификацию");
                }

                // Цикл работы
                while (isRunning) {
                    try {
                        String message = in.readUTF();
                        if (this.getRole().equals("admin")) {
                            if (message.startsWith("/kick")) {
                                String[] elements = message.split(" ");
                                if (elements.length != 2) {
                                    sendMessage("Неверный формат команды /kick ");
                                    continue;
                                }
                                server.kickUser(elements[1]);
                                server.broadcastMessage("Пользователь " + elements[1] + " удален");
                            }
                        }
                        if (message.startsWith("/")) {
                            if (message.startsWith("/exit")) {
                                sendMessage("/exitok");
                                isRunning = false; // Устанавливаем флаг для выхода
                                break;
                            }
                        } else {
                            server.broadcastMessage(username + " : " + message);
                        }
                    } catch (EOFException e) {
                        System.out.println("Клиент отключился: " + username);
                        isRunning = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            if (isRunning) {
                out.writeUTF(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        isRunning = false;
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
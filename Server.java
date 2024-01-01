package ru.fsv67.Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class Server {
    private static Map<String, ServerSocketWrapper> clients = new HashMap<>();
    Socket client;
    ServerSocketWrapper socketWrapper;

    /**
     * Метод создания подключения сервера к порту
     *
     * @return socket подключение сервера
     */
    public ServerSocket createServer() {
        final int PORT = 55555;
        try {
            ServerSocket server = new ServerSocket(PORT);
            System.out.println("Сервер подключен к порту: " + PORT);
            return server;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод подключения клиента к серверу
     *
     * @param server socket подключения сервера
     */
    public void connectClient(ServerSocket server) {
        while (true) {
            try {
                client = server.accept();
                String userName = new Scanner(client.getInputStream()).nextLine();
                socketWrapper = new ServerSocketWrapper(userName, client);
                clients.put(userName, socketWrapper);
                System.out.println("Подключился новый клиент: " + socketWrapper.getUserName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            runThreadClient();
        }
    }

    /**
     * Метод обработки сообщений клиента в потоке
     */
    private void runThreadClient() {
        new Thread(() -> {
            try (Scanner input = socketWrapper.getInput();
                 PrintWriter output = socketWrapper.getOutput()) {

                String userName = socketWrapper.getUserName();

                output.println("Успешное подключение к серверу, " + userName + "! Список клиентов " + clients);

                while (true) {

                    String clientMessage = input.nextLine();

                    String destinationUser = getUserName(clientMessage);
                    ServerSocketWrapper destination = null;

                    if (clientMessage.charAt(0) == '@') {
                        destination = clients.get(destinationUser.toLowerCase());
                    }

                    if (Objects.equals("q", clientMessage)) {
                        sendMessageToEveryone("Клиент " + userName + " отключился", userName);
                        output.println("Отключение от сервера...");
                        System.out.println("Клиент " + userName + " отключился");
                        clients.remove(userName);
                        break;
                    }

                    if (userName.equalsIgnoreCase("admin")) {
                        if (Objects.equals("exit", clientMessage)) {
                            sendMessageToEveryone("Сервер отключен", userName);
                            output.println("Сервер отключен");
                            System.out.println("Сервер отключен");
                            System.exit(0);
                        }
                        if (Objects.equals("kick", clientMessage.split(" ")[0])) {
                            destination = clients.get(clientMessage.split(" ")[1]);
                            destination.getOutput().println("Соединение прервано администратором");
                            destination.getInput().close();
                            destination.getOutput().close();
                            destination.getSocket().close();
                        }
                    }

                    if (destination != null) {
                        destination.getOutput().println(clientMessage);
                    } else {
                        sendMessageToEveryone(clientMessage, userName);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    /**
     * Метод отправки сообщения всем кроме отправляемого пользователя
     *
     * @param message  отправляемое сообщение
     * @param userName имя пользователя который отправляет сообщение
     */
    private void sendMessageToEveryone(String message, String userName) {
        for (String name : clients.keySet()) {
            if (!clients.get(name).getUserName().equalsIgnoreCase(userName))
                clients.get(name).getOutput().println(message);
        }
    }

    /**
     * Метод закрытия соединения сервера к порту
     *
     * @param server socket подключения сервера
     */
    public void serverClosing(ServerSocket server) {
        try {
            server.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод получения имени пользователя к которому обращаются
     *
     * @param message пришедшее сообщение к сервису
     * @return имя пользователя
     */
    private String getUserName(String message) {
        String destinationUserName = "";
        if (message.charAt(0) == '@') {
            destinationUserName = message.split(" ")[0].substring(1);
        } else {
            destinationUserName = message.split(" ")[0];
        }
        return destinationUserName;
    }
}

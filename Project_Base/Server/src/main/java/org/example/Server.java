package org.example;

import org.apache.commons.cli.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void startServer() {
        System.out.println("Run example: java -jar server.jar -port 6452");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port: " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ServerThread(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ServerThread extends Thread {
        private Socket socket;

        public ServerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream ois = new DataInputStream(socket.getInputStream());
                 DataOutputStream oos = new DataOutputStream(socket.getOutputStream())) {
                int resp = 1;

                System.out.println("Server started listening messages ");
                while (true) {
                    try {
                        System.out.print("\r Server received message. Number of received messages:  "+ resp/2);
                        String request = ois.readUTF();
                        resp++;
                        oos.writeUTF(request);
                        oos.flush();

                    } catch (SocketException se) {
                        System.out.println();
                        System.out.println(" Server received all messages");
                        break;
                    } catch (EOFException e){
                        System.out.println("Client dropped connection");
                        break;
//
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            } catch (EOFException e){
                System.out.println("Server lost connectiom");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("port", true, "Port number");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            int port = Integer.parseInt(cmd.getOptionValue("port", "6452"));
            Server server = new Server(port);
            server.startServer();
        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
        }
    }
}

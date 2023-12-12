import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private ServerSocket serverSocket;
    private AtomicBoolean isIndexReady;
    private final InvertedIndex index;

    public Server() {
        this.index = new InvertedIndex();
        this.isIndexReady = new AtomicBoolean(false);
    }

    public void start(int port) {
        try {
            (new Thread(new IndexFiller(index, null, isIndexReady))).start();
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                (new Thread(new ClientHandler(clientSocket))).start();
                System.out.println("New client " + clientSocket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                String inputLine;
                String[] words;
                boolean disconnected = false;
                String options = """
                        Server options:
                        1. Provide words to find
                        2. Check index status
                        3. See options
                        4. Disconnect""";
                dos.writeUTF(options);
                while (!disconnected) {
                    inputLine = dis.readUTF();
                    switch (inputLine) {
                        case "1" -> {
                            dos.writeUTF("Enter separated by space words to find:");
                            words = dis.readUTF().split(" ");
                            dos.writeUTF("Result: " + String.join(" ", words));
                        }
                        case "2" -> {
                            dos.writeUTF(
                                    isIndexReady.get() ? "Index is populated and ready for use." : "Index is empty.");
                        }
                        case "3" -> {
                            dos.writeUTF(options);
                        }
                        case "4" -> {
                            disconnected = true;
                            dos.writeUTF("Disconnected.");
                        }
                        default -> dos.writeUTF("Unknown option");
                    }
                }
                dis.close();
                dos.close();
                clientSocket.close();
                System.out.println("Client finished " + clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

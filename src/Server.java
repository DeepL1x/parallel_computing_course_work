import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private static final int DEF_THREADS_AMOUNT = 4;
    private ServerSocket serverSocket;
    private AtomicBoolean isIndexReady;
    private InvertedIndex index;
    private final int fillerThreadsAmount;
    private final boolean loadIndex;
    private final boolean saveIndex;

    public Server() {
        this(DEF_THREADS_AMOUNT, false, false);
    }

    public Server(int fillerThreadsAmount) {
        this(fillerThreadsAmount, false, false);
    }

    public Server(boolean loadIndex) {
        this(DEF_THREADS_AMOUNT, loadIndex, false);
    }

    public Server(int fillerThreadsAmount, boolean saveIndex) {
        this(fillerThreadsAmount, false, saveIndex);
    }

    /**
     * Universal constructor that resolves conflicts between loadIndex and saveIndex
     * params
     * 
     * @param fillerThreadsAmount - amount of threads that will fill the index
     * @param loadIndex           - if true, the index will be loaded from file
     * @param saveIndex           - if true the index will be saved to file
     */
    public Server(int fillerThreadsAmount, boolean loadIndex, boolean saveIndex) {
        this.index = new InvertedIndex();
        this.isIndexReady = new AtomicBoolean(false);
        this.fillerThreadsAmount = fillerThreadsAmount;
        if (loadIndex) {
            this.loadIndex = true;
            this.saveIndex = false;
        } else if (saveIndex) {
            this.loadIndex = false;
            this.saveIndex = true;
        } else {
            this.loadIndex = false;
            this.saveIndex = false;
        }
    }

    public void start(int port) {
        try {
            if (loadIndex) {
                (new Thread(() -> FileUtils.loadIndex(index, "savedIndex/index.ser", isIndexReady))).start();
            } else {
                (new Thread(new IndexFiller(index,
                        "files", isIndexReady,
                        fillerThreadsAmount,
                        saveIndex,
                        "savedIndex/index.ser"))).start();
            }
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
                            words = dis.readUTF().split("\\W");
                            if (!isIndexReady.get()) {
                                dos.writeUTF("Index is not ready yet.");
                                break;
                            }
                            double time;
                            long start, end;
                            start = System.nanoTime();
                            dos.writeUTF("Result: " + index.get(words).keySet().toString());
                            end = System.nanoTime();
                            time = (end - start) / 1e6;
                            System.out.println("Result found in " + time + "ms");
                            break;
                        }
                        case "2" -> {
                            dos.writeUTF(
                                    isIndexReady.get() ? "Index is populated and ready for use." : "Index is empty.");
                            break;
                        }
                        case "3" -> {
                            dos.writeUTF(options);
                            break;
                        }
                        case "4" -> {
                            disconnected = true;
                            dos.writeUTF("Disconnected.");
                            break;
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

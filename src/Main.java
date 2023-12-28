public class Main {
    public static void main(String[] args) {
        int threadsAmount = 1;
        boolean loadIndex = false;
        boolean saveIndex = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-threads":
                    if (i + 1 < args.length) {
                        threadsAmount = Integer.parseInt(args[i + 1]);
                        i++;
                    } else {
                        System.err.println("Missing value for -threads");
                        System.exit(1);
                    }
                    break;
                case "-loadIndex":
                    loadIndex = true;
                    break;
                case "-saveIndex":
                    saveIndex = true;
                    break;
                default:
                    System.err.println("Unknown parameter: " + args[i]);
                    System.exit(1);
            }
        }
        (new Server(threadsAmount, loadIndex, saveIndex)).start(6969);
    }
}
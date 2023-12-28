import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileUtils {
    public static List<File> getFiles(String filename) {
        List<File> files = new ArrayList<>();
        getFile(new File(filename), files);
        return files;
    }

    public static void getFile(File file, List<File> files) {
        if (file.isDirectory()) {
            File[] filesInDir = file.listFiles();
            for (File fileInDir : filesInDir) {
                getFile(fileInDir, files);
            }
        } else {
            files.add(file);
        }
    }

    public static void saveObject(Object index, String path) {
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(index);
                System.out.println("Object saved successfully to " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadIndex(InvertedIndex index, String filePath, AtomicBoolean isIndexReady) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            InvertedIndex loadedIndex = (InvertedIndex) ois.readObject();
            index.setBuckets(loadedIndex.getBuckets());
            index.setSize(loadedIndex.getSize());
            isIndexReady.set(true);
            System.out.println("Index is successfully loaded");

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

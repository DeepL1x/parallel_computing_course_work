import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
}

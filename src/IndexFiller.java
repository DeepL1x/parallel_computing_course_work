import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IndexFiller implements Runnable {
    private InvertedIndex index;
    private List<File> files;
    private static final Integer DEF_THREADS_AMOUNT = 4;
    private Integer threadsAmount;
    private AtomicBoolean isIndexReady;

    public IndexFiller(InvertedIndex index, String filename, AtomicBoolean isIndexReady) {
        this.index = index;
        this.threadsAmount = DEF_THREADS_AMOUNT;
        this.isIndexReady = isIndexReady;
        this.files = FileUtils.getFiles("files");
        fillIndex();
    }

    public IndexFiller(InvertedIndex index, String filename, AtomicBoolean isIndexReady, Integer threadsAmount) {
        this.index = index;
        this.threadsAmount = threadsAmount;
        this.isIndexReady = isIndexReady;
        this.files = FileUtils.getFiles("files");
        fillIndex();
    }

    @Override
    public void run() {
        fillIndex();
    }

    private void fillIndex() {
        ExecutorService executor = Executors.newFixedThreadPool(threadsAmount);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        int step = files.size() / threadsAmount;
        if (step == 0) {
            step = 1;
        }
        int tasksSubmitted = 0;
        for (int i = 0; i < threadsAmount; i++) {
            int start = i * step;
            int end = (i + 1) * step;
            if (end <= files.size()) {
                completionService.submit(() -> fillIndexTask(files.subList(start, end)), null);
                tasksSubmitted++;
            }
        }
        for (int i = 0; i < tasksSubmitted; i++) {
            try {
                Future<Void> future = completionService.poll(1, TimeUnit.MINUTES);
                if (future != null) {
                    future.get();
                    System.out.println("get");
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        isIndexReady.set(true);
        executor.shutdown();
    }

    private void fillIndexTask(List<File> files) {
        for (File file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                int wordPosition = 0;
                while ((line = br.readLine()) != null) {
                    String[] words = line.split("\\W");
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            System.out.println(word + " " + file.getName() + " " + wordPosition);
                            ;
                            index.put(word, file.getName(), wordPosition);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

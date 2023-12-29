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
    private final InvertedIndex index;
    private final List<File> files;
    private static final Integer DEF_THREADS_AMOUNT = 4;
    private final Integer threadsAmount;
    private final AtomicBoolean isIndexReady;
    private final Boolean saveIndex;
    private final String indexPath;

    public IndexFiller(InvertedIndex index, String filename, AtomicBoolean isIndexReady) {
        this(index, filename, isIndexReady, DEF_THREADS_AMOUNT, false, "");
    }

    public IndexFiller(InvertedIndex index, String filename, AtomicBoolean isIndexReady, Integer threadsAmount) {
        this(index, filename, isIndexReady, threadsAmount, false, "");
    }

    public IndexFiller(InvertedIndex index, String filename, AtomicBoolean isIndexReady, Integer threadsAmount,
            Boolean saveIndex, String indexPath) {
        this.index = index;
        this.threadsAmount = threadsAmount;
        this.isIndexReady = isIndexReady;
        this.files = FileUtils.getFiles("files");
        this.saveIndex = saveIndex;
        this.indexPath = indexPath;
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
        double time, start, end;
        start = System.nanoTime();
        if (threadsAmount <= 1) {
            fillIndexTask(files);
            end = System.nanoTime();
        } else {
            for (int i = 0; i < threadsAmount; i++) {
                int filesFrom = i * step;
                int filesTo = (i + 1) * step;
                if (filesFrom >= files.size()) {
                    break;
                } else if (i == threadsAmount - 1) {
                    if (filesTo <= files.size()) {
                        completionService.submit(() -> fillIndexTask(files.subList(filesFrom, files.size())), null);
                        tasksSubmitted++;
                        break;
                    }
                }
                completionService.submit(() -> fillIndexTask(files.subList(filesFrom, filesTo)), null);
                tasksSubmitted++;
            }
            for (int i = 0; i < tasksSubmitted; i++) {
                try {
                    Future<Void> future = completionService.poll(1, TimeUnit.MINUTES);
                    if (future != null) {
                        future.get();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            end = System.nanoTime();
        }
        time = (end - start) / 1e6;
        System.out.println("Index filled in " + time + " ms");
        isIndexReady.set(true);
        executor.shutdown();

        if (saveIndex) {
            FileUtils.saveObject(index, indexPath);
        }
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
                            index.put(word, file.getName(), wordPosition);
                            wordPosition++;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

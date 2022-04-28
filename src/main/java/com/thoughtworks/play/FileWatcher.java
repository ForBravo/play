package com.thoughtworks.play;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.concurrent.Executor;

@Component
public class FileWatcher {

    private final Executor executor;
    private final ContextRefresher contextRefresher;

    public FileWatcher(Executor executor, ContextRefresher contextRefresher) {
        this.executor = executor;
        this.contextRefresher = contextRefresher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterPropertiesSet() {
        executor.execute(() -> {
            WatchService watchService;
            try {
                watchService = FileSystems.getDefault()
                        .newWatchService();
                var path = Paths.get("/vault/secrets/");
                var register = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    register.pollEvents()
                            .forEach(event -> {
                                System.out.println(
                                        "Event kind:" + event.kind()
                                                + ". File affected: " + event.context() + ".");
                                contextRefresher.refresh();
                            });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

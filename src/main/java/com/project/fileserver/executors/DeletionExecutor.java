package com.project.fileserver.executors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class DeletionExecutor {

	public DeletionExecutor(@Value("${executors.enabled:false}") boolean enabled,
			@Value("${executors.archival.path:/fileserver/archives}") String archivalpath,
			@Value("${executors.deletion.days:15}") int days) {
		if (enabled) {
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.scheduleAtFixedRate(new DeletionProcess(archivalpath, days), 1, 1, TimeUnit.DAYS);
		}
	}

	private class DeletionProcess implements Runnable {

		private String archivalpath;
		private int days;

		public DeletionProcess(String archivalpath, int days) {
			this.archivalpath = archivalpath;
			this.days = days;
		}

		@Override
		public void run() {
			try (Stream<Path> buckets = Files.list(Paths.get(archivalpath)).filter(path -> Files.isDirectory(path))) {
				buckets.forEach(bucket -> {
					try (Stream<Path> folderids = Files.list(bucket)
							.filter(path -> !Files.isDirectory(path)
									&& (Instant.now().toEpochMilli() - path.toFile().lastModified())
											/ (1000 * 60 * 60 * 24) > days)) {
						folderids.forEach(folderid -> {
							try {
								Files.deleteIfExists(folderid);
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						});
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				});
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}

	}

}

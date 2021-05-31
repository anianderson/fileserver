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

import com.project.fileserver.utils.CommonServiceUtils;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class CleanUpExecutor {

	public CleanUpExecutor(CommonServiceUtils commonService, @Value("${temppath:/fileserver/temp}") String temppath) {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(new CleanUpProcess(commonService, temppath), 1, 1, TimeUnit.DAYS);
	}

	private class CleanUpProcess implements Runnable {

		private String temppath;
		private CommonServiceUtils commonService;

		public CleanUpProcess(CommonServiceUtils commonService, String temppath) {
			this.temppath = temppath;
			this.commonService = commonService;
		}

		@Override
		public void run() {
			try (Stream<Path> folders = Files.list(Paths.get(temppath)).filter(path -> Files.isDirectory(path)
					&& (Instant.now().toEpochMilli() - path.toFile().lastModified()) / (1000 * 60 * 60 * 24) > 1)) {
				folders.forEach(folder -> {
					try {
						commonService.deleteDirectory(folder);
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

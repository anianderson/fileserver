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
public class ArchivalExecutor {

	public ArchivalExecutor(CommonServiceUtils commonService, @Value("${executors.enabled:false}") boolean enabled,
			@Value("${localpath:/fileserver/files}") String localpath,
			@Value("${executors.archival.path:/fileserver/archives}") String archivalpath,
			@Value("${executors.archival.days:30}") int days) {
		if (enabled) {
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.scheduleAtFixedRate(new ArchivalProcess(commonService, localpath, archivalpath, days), 1, 1,
					TimeUnit.DAYS);
		}
	}

	private class ArchivalProcess implements Runnable {

		private CommonServiceUtils commonService;
		private String localpath;
		private String archivalpath;
		private int days;

		public ArchivalProcess(CommonServiceUtils commonService, String localpath, String archivalpath, int days) {
			this.commonService = commonService;
			this.localpath = localpath;
			this.archivalpath = archivalpath;
			this.days = days;
		}

		@Override
		public void run() {
			try (Stream<Path> buckets = Files.list(Paths.get(this.localpath)).filter(path -> Files.isDirectory(path))) {
				buckets.forEach(bucket -> {
					try (Stream<Path> folderids = Files.list(bucket)
							.filter(path -> Files.isDirectory(path)
									&& (Instant.now().toEpochMilli() - path.toFile().lastModified())
											/ (1000 * 60 * 60 * 24) > days)) {
						folderids.forEach(folderid -> {
							try {
								if (!commonService.isEmpty(folderid)) {
									Path target = Paths.get(archivalpath, bucket.getFileName().toString(),
											String.format("%s.zip", folderid.getFileName().toString()));
									Files.createDirectories(target.getParent());
									commonService.zipDirectory(folderid, target);
								}
								commonService.deleteDirectory(folderid);
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

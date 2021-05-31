package com.project.fileserver.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonArray;
import com.project.fileserver.model.RequiredObject;
import com.project.fileserver.service.FileserverService;
import com.project.fileserver.utils.CommonServiceUtils;

import lombok.extern.log4j.Log4j2;

@Service("local")
@Log4j2
public class LocalFileserverService implements FileserverService {

	private String archivalpath;
	private String localpath;
	private CommonServiceUtils commonService;

	public LocalFileserverService(@Value("${localpath:/fileserver/files}") String localpath,
			@Value("${executors.archival.path:/fileserver/archives}") String archivalpath,
			CommonServiceUtils commonService) {
		this.archivalpath = archivalpath;
		this.localpath = localpath;
		this.commonService = commonService;
	}

	@Override
	public String generateFolderUniqueId(RequiredObject requiredObject, String prefix) throws Exception {
		String id = commonService.generateUniqueId(prefix);
		Path path = Paths.get(localpath, requiredObject.getBucket(), id);
		Files.createDirectories(path);
		return id;
	}

	@Override
	public boolean uploadFiles(RequiredObject requiredObject, MultipartFile[] files, boolean replace) throws Exception {
		for (MultipartFile file : files) {
			Path path = Paths.get(localpath, requiredObject.getBucket(), requiredObject.getFolderid(),
					file.getOriginalFilename());
			if (replace) {
				Files.deleteIfExists(path);
			}
			file.transferTo(path);
		}
		return true;
	}

	@Override
	public File downloadFiles(RequiredObject requiredObject, String filenames) throws Exception {
		JsonArray array = commonService.getFilenameList(filenames);
		List<File> files = getFileList(requiredObject, array);
		updateLastModifiedDate(files);
		File file = commonService.generateFiles(files);
		return file;
	}

	@Override
	public boolean deleteFiles(RequiredObject requiredObject, String filenames) throws Exception {
		JsonArray array = commonService.getFilenameList(filenames);
		List<File> files = getFileList(requiredObject, array);
		return commonService.deleteFiles(files);
	}

	private List<File> getFileList(RequiredObject requiredObject, JsonArray array) {
		Path checkpath = Paths.get(localpath, requiredObject.getBucket(), requiredObject.getFolderid());
		if (!Files.exists(checkpath)) {
			try {
				unarchive(requiredObject);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		List<File> files = new LinkedList<>();
		array.forEach(element -> {
			Path path = Paths.get(localpath, requiredObject.getBucket(), requiredObject.getFolderid(),
					element.getAsString());
			files.add(path.toFile());
		});
		return files;
	}

	private void unarchive(RequiredObject requiredObject) throws IOException {
		Path source = Paths.get(archivalpath, requiredObject.getBucket(),
				String.format("%s.zip", requiredObject.getFolderid()));
		Path target = Paths.get(localpath, requiredObject.getBucket());
		commonService.unzip(source, target);
		Files.deleteIfExists(source);
	}

	private void updateLastModifiedDate(List<File> files) {
		files.stream().map(file -> file.toPath()).forEach(path -> {
			try {
				Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
				Files.setLastModifiedTime(path.getParent(), FileTime.from(Instant.now()));
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		});
	}

}

package com.project.fileserver.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

@Component
public class CommonServiceUtils {

	@Value("${temppath:/fileserver/temp}")
	private String temppath;

	public String refactorBucketName(String bucket) {
		bucket = retainLowerCaseOnly(bucket.toLowerCase());
		return bucket.substring(0, bucket.length() >= 50 ? 50 : bucket.length());
	}

	public String generateUniqueId(String prefix) {
		prefix = retainLowerCaseOnly(prefix.toLowerCase());
		String uuid = retainAplhaNumericOnly(UUID.randomUUID().toString().toLowerCase());
		return String.format("%s%s", prefix, uuid);
	}

	public String retainLowerCaseOnly(String name) {
		return name.replaceAll("[^a-z]", "");
	}

	public String retainAplhaNumericOnly(String name) {
		return name.replaceAll("[^a-z0-9]", "");
	}

	public JsonArray getFilenameList(String filenames) {
		return new Gson().fromJson(filenames, JsonArray.class);
	}

	public File generateFiles(List<File> files) throws IOException {
		if (files.isEmpty()) {
			return null;
		} else {
			if (files.size() > 1) {
				Path targetdir = Paths.get(temppath, retainAplhaNumericOnly(UUID.randomUUID().toString()), "files.zip");
				Files.createDirectories(targetdir.getParent());
				File result = targetdir.toFile();
				zipFiles(files, result);
				return result;
			} else {
				return files.get(0);
			}
		}
	}

	public boolean deleteFiles(List<File> files) {
		boolean result = true;
		for (int index = 0, limit = files.size(); index < limit; index++) {
			File file = files.get(index);
			result = file.delete() && result;
		}
		return result;
	}

	public void deleteDirectory(Path path) throws IOException {
		Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}

	public void zipFiles(List<File> files, File targetfile) throws FileNotFoundException, IOException {
		try (FileOutputStream fos = new FileOutputStream(targetfile, false)) {
			try (ZipOutputStream zipOut = new ZipOutputStream(fos)) {
				for (File fileToZip : files) {
					try (FileInputStream fis = new FileInputStream(fileToZip)) {
						ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
						zipOut.putNextEntry(zipEntry);
						byte[] bytes = new byte[1024];
						int length;
						while ((length = fis.read(bytes)) >= 0) {
							zipOut.write(bytes, 0, length);
						}
					}
				}
			}
		}
	}

	public void zipDirectory(Path source, Path target) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
			try (ZipOutputStream zipOut = new ZipOutputStream(fos)) {
				File fileToZip = source.toFile();
				zipFile(fileToZip, fileToZip.getName(), zipOut);
			}
		}
	}

	public void unzip(Path source, Path destination) throws IOException {
		File destDir = destination.toFile();
		byte[] buffer = new byte[1024];
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File newFile = newFile(destDir, zipEntry);
				if (zipEntry.isDirectory()) {
					if (!newFile.isDirectory() && !newFile.mkdirs()) {
						throw new IOException("Failed to create directory " + newFile);
					}
				} else {
					// fix for Windows-created archives
					File parent = newFile.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("Failed to create directory " + parent);
					}
					// write file content
					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
	}

	public boolean isEmpty(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			try (Stream<Path> entries = Files.list(path)) {
				return !entries.findFirst().isPresent();
			}
		}
		return false;
	}

	private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			if (fileName.endsWith("/")) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + "/"));
				zipOut.closeEntry();
			}
			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		try (FileInputStream fis = new FileInputStream(fileToZip)) {
			ZipEntry zipEntry = new ZipEntry(fileName);
			zipOut.putNextEntry(zipEntry);
			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zipOut.write(bytes, 0, length);
			}
		}
	}

	private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());
		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();
		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}
		return destFile;
	}

}

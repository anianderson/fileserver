package com.project.fileserver.controller;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.fileserver.model.RequiredObject;
import com.project.fileserver.service.FileserverService;

import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api")
@Log4j2
public class FileserverController {

	private FileserverService fileserverService;

	public FileserverController(BeanFactory beanFactory, @Value("${spring.profiles.active}") String active) {
		this.fileserverService = beanFactory.getBean(active, FileserverService.class);
	}

	@GetMapping("/generateId")
	public ResponseEntity<String> generateFolderUniqueId(@RequestParam(name = "bucket", required = true) String bucket,
			@RequestParam(name = "prefix", required = false, defaultValue = "default") String prefix) {
		try {
			String result = fileserverService.generateFolderUniqueId(new RequiredObject(bucket, null), prefix);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/upload/files", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Boolean> uploadFile(@RequestParam(name = "bucket", required = true) String bucket,
			@RequestParam(name = "id", required = true) String id,
			@RequestParam(name = "replace", required = false, defaultValue = "false") boolean replace,
			@RequestBody MultipartFile[] files) {
		try {
			boolean result = fileserverService.uploadFiles(new RequiredObject(bucket, id), files, replace);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/download/files")
	public ResponseEntity<Resource> downloadFiles(@RequestParam(name = "bucket", required = true) String bucket,
			@RequestParam(name = "id", required = true) String id, @RequestBody String filenames) {
		try {
			File file = fileserverService.downloadFiles(new RequiredObject(bucket, id), filenames);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
			return new ResponseEntity<>(new InputStreamResource(new FileInputStream(file)), responseHeaders,
					HttpStatus.OK);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/delete/files")
	public ResponseEntity<Boolean> deleteFiles(@RequestParam(name = "bucket", required = true) String bucket,
			@RequestParam(name = "id", required = true) String id, @RequestBody String filenames) {
		try {
			boolean result = fileserverService.deleteFiles(new RequiredObject(bucket, id), filenames);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}

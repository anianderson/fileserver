package com.project.fileserver.service;

import java.io.File;

import org.springframework.web.multipart.MultipartFile;

import com.project.fileserver.model.RequiredObject;

public interface FileserverService {

	public String generateFolderUniqueId(RequiredObject requiredObject, String prefix) throws Exception;

	public boolean uploadFiles(RequiredObject requiredObject, MultipartFile[] files, boolean replace) throws Exception;

	public File downloadFiles(RequiredObject requiredObject, String filenames) throws Exception;

	public boolean deleteFiles(RequiredObject requiredObject, String filenames) throws Exception;

}

package com.example.demo.iface.rest;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.service.IngestionApplicationService;
import com.example.demo.iface.dto.res.DocumentUploadedResource;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class IngestionController {

	private final IngestionApplicationService ingestionService;

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DocumentUploadedResource> uploadDocument(@RequestPart("file") MultipartFile file)
			throws IOException {
		ingestionService.ingest(file);
		return ResponseEntity.ok(new DocumentUploadedResource("201", "文件已成功轉化為向量並存入知識庫！"));
	}
}
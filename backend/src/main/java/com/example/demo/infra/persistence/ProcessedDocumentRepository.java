package com.example.demo.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.infra.idempotence.ProcessedDocument;

public interface ProcessedDocumentRepository extends JpaRepository<ProcessedDocument, Long> {
	boolean existsByFileHash(String fileHash);
}
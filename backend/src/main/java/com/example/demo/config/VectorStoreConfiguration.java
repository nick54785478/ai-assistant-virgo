package com.example.demo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <b>RAG 向量資料庫配置</b>
 * <p>
 * 負責初始化 PostgreSQL 的 pgvector 擴充功能，並將其橋接為 Spring AI 可識別的 VectorStore。 這是實現「DDD
 * 架構專家」等知識增強模式 (RAG) 的核心組件。
 * </p>
 */
@Configuration
public class VectorStoreConfiguration {

	/**
	 * 向量維度：需與使用的 Embedding 模型 (如 nomic-embed-text) 輸出維度一致
	 */
	@Value("${app.vectorstore.pgvector.dimensions:768}")
	private int dimensions;

	/**
	 * 是否在啟動時自動建立向量資料表與 pgvector extension
	 */
	@Value("${app.vectorstore.pgvector.initialize-schema:true}")
	private boolean initSchema;

	/**
	 * <b>建立 PgVectorStore Bean</b>
	 * <p>
	 * 利用 JdbcTemplate 與底層資料庫溝通，提供相似度搜尋 (Similarity Search) 能力。
	 * </p>
	 * 
	 * @param jdbcTemplate   Spring 內建的 JDBC 操作模板
	 * @param embeddingModel 負責將文字轉換為向量的嵌入模型 (由 AiConfiguration 之外的配置注入)
	 */
	@Bean
	public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
		return PgVectorStore.builder(jdbcTemplate, embeddingModel).dimensions(dimensions).initializeSchema(initSchema)
				.vectorTableName("vector_store").build();
	}
}
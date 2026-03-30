package com.example.demo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfiguration {

	@Value("${app.vectorstore.pgvector.dimensions:768}")
	private int dimensions;

	@Value("${app.vectorstore.pgvector.initialize-schema:true}")
	private boolean initSchema;

	@Bean
	public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
		return PgVectorStore.builder(jdbcTemplate, embeddingModel).dimensions(dimensions).initializeSchema(initSchema)
				.vectorTableName("vector_store").build();
	}
}
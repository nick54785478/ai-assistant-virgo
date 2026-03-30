package com.example.demo.application.shared.dto.view;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteMessageQueriedView {

	private Long id;
	
	private String chatId;
	
	private String question;
	
	private String answer;
	
	private List<String> tags;
	
	private LocalDateTime createdAt;

}

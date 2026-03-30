package com.example.demo.infra.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.demo.application.shared.dto.dto.ChatDataDto;
import com.example.demo.application.shared.dto.dto.ChatMetaDto;
import com.example.demo.application.shared.dto.view.ChatHistoryGottenView;
import com.example.demo.application.shared.dto.view.ChatInteractionParsedView;

@Mapper(componentModel = "spring")
public interface ChatHistoryMapper {

	// 主入口：告訴 MapStruct，data 和 meta 都要從 view 轉換過來
	@Mapping(target = "data", source = "view")
	@Mapping(target = "meta", source = "view")
	ChatHistoryGottenView toDto(ChatInteractionParsedView view);

	// 子轉換 1：專門處理 ChatDataDto (這裡變數叫 view，expression 就不會報錯了)
	@Mapping(target = "sender", source = "sender")
	@Mapping(target = "thinkingText", source = "thinkingText")
	@Mapping(target = "outputText", source = "outputText")
	@Mapping(target = "rawText", expression = "java(view.thinkingText() + view.outputText())")
	ChatDataDto toDataDto(ChatInteractionParsedView view);

	// 子轉換 2：專門處理 ChatMetaDto
	@Mapping(target = "isThinking", constant = "false")
	@Mapping(target = "isFavorited", constant = "false")
	@Mapping(target = "favoriteId", ignore = true) // 預留欄位先忽略，避免編譯警告
	ChatMetaDto toMetaDto(ChatInteractionParsedView view);
}
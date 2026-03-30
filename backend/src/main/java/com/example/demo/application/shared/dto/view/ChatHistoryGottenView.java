package com.example.demo.application.shared.dto.view;

import com.example.demo.application.shared.dto.dto.ChatDataDto;
import com.example.demo.application.shared.dto.dto.ChatMetaDto;

public record ChatHistoryGottenView(
	    ChatDataDto data,
	    ChatMetaDto meta
	) {}
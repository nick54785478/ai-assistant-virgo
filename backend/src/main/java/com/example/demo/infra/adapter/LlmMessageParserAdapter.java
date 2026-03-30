package com.example.demo.infra.adapter;

import org.springframework.stereotype.Component;

import com.example.demo.application.port.LlmMessageParserPort;
import com.example.demo.application.shared.dto.view.ChatInteractionParsedView;
import com.example.demo.application.shared.dto.view.RawChatMessageGottenView;

@Component
class LlmMessageParserAdapter implements LlmMessageParserPort {

	@Override
	public ChatInteractionParsedView parse(RawChatMessageGottenView rawMessage) {
		if (rawMessage == null)
			return null;

		String msgType = rawMessage.messageType();
		String content = rawMessage.content();

		if ("user".equalsIgnoreCase(msgType)) {
			return new ChatInteractionParsedView("user", "", content);
		} else if ("assistant".equalsIgnoreCase(msgType)) {
			return parseAssistantMessage(content);
		} else {
			return new ChatInteractionParsedView("system", "", content);
		}
	}

	private ChatInteractionParsedView parseAssistantMessage(String rawText) {
		int thinkStart = rawText.indexOf("<think>");
		int thinkEnd = rawText.indexOf("</think>");

		if (thinkStart != -1 && thinkEnd != -1) {
			String think = rawText.substring(thinkStart + 7, thinkEnd).trim();
			String output = rawText.substring(thinkEnd + 8).trim();
			return new ChatInteractionParsedView("bot", think, output);
		}
		return new ChatInteractionParsedView("bot", "", rawText.trim());
	}
}
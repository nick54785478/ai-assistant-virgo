package com.example.demo.infra.adapter;

import org.springframework.stereotype.Component;

import com.example.demo.application.port.LlmMessageParserPort;
import com.example.demo.application.shared.dto.view.ChatInteractionParsedView;
import com.example.demo.application.shared.dto.view.RawChatMessageGottenView;

/**
 * <b>[Infrastructure Adapter] LLM 訊息解析轉譯器 (防腐層)</b>
 * <p>
 * 本類別為 {@link LlmMessageParserPort} 的具體實作，位於六角架構的基礎設施層 (Infrastructure Layer)。
 * 其核心職責是作為文本解析的防腐層 (Anti-Corruption Layer)，專門處理特定 AI 模型（如 DeepSeek-R1）
 * 所產生的特定標記語言（例如：{@code <think>} 思維鏈標籤）。
 * 透過此轉譯器，可將充滿底層模型特徵的原始字串，淨化為應用層與展示層皆通用的結構化領域視圖
 * {@link ChatInteractionParsedView}。
 * </p>
 */
@Component
class LlmMessageParserAdapter implements LlmMessageParserPort {
	/**
	 * <b>解析包含特定供應商標籤的原始訊息</b>
	 * <p>
	 * 根據原始訊息的角色 (Role/Type) 進行分流處理。若為 AI 助手的回覆，則進一步觸發
	 * 深度解析邏輯，分離出隱藏的思考過程與最終交付給使用者的淨輸出文字。
	 * </p>
	 * 
	 * @param rawMessage 來自底層記憶體或 AI API 的原始訊息紀錄 (DTO)
	 * @return 解析後的結構化領域視圖，包含發送者身分、思考過程 (CoT) 與淨輸出；若輸入為 null 則回傳 null
	 */
	@Override
	public ChatInteractionParsedView parse(RawChatMessageGottenView rawMessage) {
		if (rawMessage == null) {
			return null;
		}

		String msgType = rawMessage.messageType();
		String content = rawMessage.content();

		if ("user".equalsIgnoreCase(msgType)) {
			return new ChatInteractionParsedView("user", "", content);
		} else if ("assistant".equalsIgnoreCase(msgType)) {
			return parseAssistantMessage(content);
		} else {
			// 處理 System Prompt 或其他未知的角色類型
			return new ChatInteractionParsedView("system", "", content);
		}
	}

	/**
	 * <b>深度解析 AI 助手的文本內容</b>
	 * <p>
	 * 針對具備深度思考能力的模型（如 DeepSeek-R1），擷取並剝離 {@code <think>} 與 {@code </think>}
	 * 標籤內的文字。若文本中未包含完整的思考標籤，則將全段文字視為最終輸出。
	 * </p>
	 * 
	 * @param rawText 助手回傳的原始完整文本
	 * @return 封裝好的解析視圖 (包含分離後的 think 與 output)
	 */
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
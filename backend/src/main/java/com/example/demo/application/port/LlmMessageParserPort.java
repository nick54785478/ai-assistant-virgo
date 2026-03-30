package com.example.demo.application.port;

import com.example.demo.application.shared.dto.view.ChatInteractionParsedView;
import com.example.demo.application.shared.dto.view.RawChatMessageGottenView;

/**
 * <b>[Application Port] LLM 訊息解析器接口</b>
 * <p>
 * 充當基礎設施層與應用層之間的防腐層 (Anti-Corruption Layer)。 職責是將特定 LLM 供應商 (如 DeepSeek)
 * 的原始文本格式， 解析為應用層可直接使用的結構化領域視圖 (Parsed View)。
 * </p>
 */
public interface LlmMessageParserPort {

	/**
	 * 解析包含特定供應商標籤的原始訊息
	 * <p>
	 * 例如：從內容中分離出 &lt;think&gt; 標籤內的思維鏈與最終輸出文字。
	 * </p>
	 * 
	 * @param rawMessage 來自底層存儲或 API 的原始訊息紀錄
	 * @return 解析後的結構化領域視圖，包含發送者身分、思考過程與淨輸出
	 */
	ChatInteractionParsedView parse(RawChatMessageGottenView rawMessage);
}
import { Injectable } from '@angular/core';
import { FavoriteSavedResource } from '../models/favorite-message.saved.model';
import { Observable } from 'rxjs/internal/Observable';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { FavoriteDeletedResource } from '../models/favorite-message.deleted.model';
import { PagedFavoriteMessagesQueriedResource } from '../models/favorite-message.querid.model';

/**
 * <b>[Frontend API Client] 收藏訊息服務</b>
 * <p>負責與後端的 FavoriteController 進行 REST API 溝通，管理個人的 QA 知識庫。</p>
 */
@Injectable({
  providedIn: 'root',
})
export class FavoriteMessageService {
  private readonly baseUrl = `${environment.apiEndpoint}/favorite`;

  constructor(private http: HttpClient) {}

  /**
   * <b>新增收藏 (POST)</b>
   * <p>將有價值的問答對存入後端關聯式資料庫中。</p>
   * @param question 使用者提問
   * @param answer AI 回答
   * @param tags 自定義標籤陣列
   */
  saveFavorite(
    question: string,
    answer: string,
    tags: string[] = [],
  ): Observable<FavoriteSavedResource> {
    // Payload 現在對齊後端的 SaveFavoriteResource
    const payload = {
      question,
      answer,
      tags,
      chatId: 'default-session', // 未來可根據需求替換為真實會話 ID
    };

    return this.http.post<FavoriteSavedResource>(this.baseUrl, payload);
  }

  /**
   * <b>取消/移除收藏 (DELETE)</b>
   * @param id 資料庫主鍵 ID
   */
  removeFavorite(id: number): Observable<FavoriteDeletedResource> {
    return this.http.delete<FavoriteDeletedResource>(`${this.baseUrl}/${id}`);
  }

  /**
   * <b>獲取收藏清單 (GET)</b>
   * <p>支援分頁與關鍵字模糊搜尋，適用於表格或虛擬滾動列表展示。</p>
   * @param keyword 搜尋關鍵字 (選填)
   * @param chatId 特定會話過濾 (選填)
   * @param page 頁碼 (0-based)
   * @param size 每頁筆數
   */
  getFavorites(
    keyword: string,
    chatId: string,
    page: number,
    size: number,
  ): Observable<PagedFavoriteMessagesQueriedResource> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    if (chatId) {
      params = params.set('chatId', chatId);
    }

    return this.http.get<PagedFavoriteMessagesQueriedResource>(this.baseUrl, {
      params,
    });
  }

  /**
   * <b>更新標籤 (PATCH)</b>
   * <p>覆蓋指定收藏紀錄的 Tag 清單。</p>
   * @param id 資料庫主鍵 ID
   * @param tags 新的標籤字串陣列
   */
  updateTags(id: number, tags: string[]): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/tags`, tags);
  }
}

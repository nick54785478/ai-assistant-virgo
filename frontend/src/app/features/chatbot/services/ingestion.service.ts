import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '../../../../environments/environment';
/**
 * <b>[Frontend API Client] 知識進食 (RAG Ingestion) 服務</b>
 * <p>負責處理上傳文件至後端向量資料庫 (Vector Store) 的相關作業。</p>
 */
@Injectable({
  providedIn: 'root',
})
export class IngestionService {
  //  統一管理 API 路徑
  private readonly baseUrl = `${environment.apiEndpoint}/ingestion`;

  constructor(private http: HttpClient) {}

  /**
   * <b>獲取標準上傳端點 URL</b>
   * <p>供 PrimeNG 的 p-fileUpload 元件的 url 屬性直接綁定使用。</p>
   */
  get uploadUrl(): string {
    return `${this.baseUrl}/upload`;
  }

  /**
   * <b>手動上傳檔案</b>
   * <p>若不依賴 UI 套件的內建 XHR，可透過此方法以 FormData 形式發送 POST 請求。</p>
   * @param file 瀏覽器選取的 File 物件
   */
  upload(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(this.uploadUrl, formData);
  }

  /**
   * <b>查詢知識庫處理狀態</b>
   * <p>未來擴充用：可輪詢此端點了解大檔案是否已完成 Embedding。</p>
   */
  getIngestionStatus(): Observable<any> {
    return this.http.get(`${this.baseUrl}/status`);
  }
}

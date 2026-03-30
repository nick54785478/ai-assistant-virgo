import { PagedQueriedView } from '../../../shared/models/paged-queried.model';
import { FavoriteItem } from './favorite-item.model';

// 🚀 對齊後端的 Response Envelope
export interface PagedFavoriteMessagesQueriedResource {
  code: string;
  message: string;
  data: PagedQueriedView<FavoriteItem>;
}

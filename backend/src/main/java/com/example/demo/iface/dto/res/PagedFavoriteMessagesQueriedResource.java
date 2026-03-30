package com.example.demo.iface.dto.res;

import com.example.demo.application.shared.dto.view.FavoriteMessageQueriedView;
import com.example.demo.application.shared.page.PagedQueriedView;

public record PagedFavoriteMessagesQueriedResource(String code, String message,
		PagedQueriedView<FavoriteMessageQueriedView> data) {

}

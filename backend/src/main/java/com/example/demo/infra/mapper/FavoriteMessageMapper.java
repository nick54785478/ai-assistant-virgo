package com.example.demo.infra.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.example.demo.application.domain.favorite.aggregate.FavoriteMessage;
import com.example.demo.application.shared.dto.view.FavoriteMessageQueriedView;

@Mapper(componentModel = "spring")
public interface FavoriteMessageMapper {

	List<FavoriteMessageQueriedView>  transformAggregate(List<FavoriteMessage> favoriteMessage);
}

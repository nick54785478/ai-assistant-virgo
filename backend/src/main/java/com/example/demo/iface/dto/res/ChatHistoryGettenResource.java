package com.example.demo.iface.dto.res;

import java.util.List;

import com.example.demo.application.shared.dto.view.ChatHistoryGottenView;

public record ChatHistoryGettenResource(String code, String message, List<ChatHistoryGottenView> data) {

}

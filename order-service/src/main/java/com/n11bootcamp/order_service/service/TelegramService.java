package com.n11bootcamp.order_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TelegramService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

    private final RestTemplate restTemplate;

    @Value("${telegram.bot.token:YOUR_BOT_TOKEN}")
    private String botToken;

    @Value("${telegram.chat.id:YOUR_CHAT_ID}")
    private String chatId;

    public TelegramService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendMessage(String message) {
        if ("YOUR_BOT_TOKEN".equals(botToken) || "YOUR_CHAT_ID".equals(chatId)) {
            LOGGER.warn("Telegram credentials are not set. Skipping notification.");
            return;
        }

        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    botToken, chatId, message);
            restTemplate.getForObject(url, String.class);
            LOGGER.info("Telegram notification sent successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to send telegram notification: {}", e.getMessage());
        }
    }
}

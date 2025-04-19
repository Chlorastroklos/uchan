package com.chloro.uchan;

import com.chloro.uchan.services.DiscordEventListener;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class UchanConfig {

    private final String token;
    private final String adminId;

    public UchanConfig(@Value("${discord.token}") String token,
                       @Value("${discord.admin.id}") String adminId) {
        this.token = token;
        this.adminId = adminId;
    }

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<DiscordEventListener<T>> listeners) {
        GatewayDiscordClient client = DiscordClientBuilder.create(token).build().login().block();

        for (DiscordEventListener<T> listener : listeners) {
            client.on(listener.getDiscordEventType())
                    .flatMap(listener::listenAndRespond)
                    .onErrorResume(listener::handleError)
                    .subscribe();
        }

        return client;
    }

    public String getAdminId() {
        return adminId;
    }
}

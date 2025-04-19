package com.chloro.uchan.services;

import discord4j.core.event.domain.Event;
import reactor.core.publisher.Mono;

public abstract class DiscordEventListener<T extends Event> {

    public abstract Class<T> getDiscordEventType();
    public abstract Mono<Void> listenAndRespond(T discordEvent);

    public Mono<Void> handleError(Throwable ex) {
        ex.printStackTrace();
        return Mono.empty();
    }
}

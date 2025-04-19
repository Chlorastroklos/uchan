package com.chloro.uchan.services;

import com.chloro.uchan.UchanConfig;
import com.chloro.uchan.enums.SlotAC6;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@Service
public class MessageCreateListener extends DiscordEventListener<MessageCreateEvent> {

    private final UchanConfig config;
    private final PartService partService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public MessageCreateListener(UchanConfig config, PartService partService) {
        this.config = config;
        this.partService = partService;
    }

    @Override
    public Class<MessageCreateEvent> getDiscordEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> listenAndRespond(MessageCreateEvent discordEvent) {
        return Flux.just(discordEvent.getMessage())
                .filter(message -> message.getData().content().startsWith("!")
                        && message.getAuthor().isPresent()
                        && !message.getAuthor().get().isBot())
                .publishOn(Schedulers.boundedElastic())
                .map(message -> {
                    try {
                        if (message.getData().content().startsWith("!test")) {
                            if (message.getAuthor().orElseThrow().getId().asString().equals(config.getAdminId())) {
                                String distribution = partService.distribution();
                                System.out.println(distribution);
                                message.getChannel().flatMap(channel ->
                                        channel.createMessage(distribution)).subscribe();
                            } else {
                                message.getChannel().flatMap(channel ->
                                        channel.createMessage(String.format(
                                                "You don't have permission to talk to me like that <@%s>!",
                                                message.getAuthor().orElseThrow().getId().asLong()))).subscribe();
                            }
                        } else if (message.getData().content().startsWith("!random")) {
                            message.getChannel().flatMap(channel ->
                                    channel.createMessage(String.format(
                                            "%s <@%s> %s",
                                            dateFormat.format(Date.from(message.getTimestamp())),
                                            message.getAuthor().orElseThrow().getId().asLong(),
                                            partService.randomBuild()))).subscribe();
                        } else {
                            message.getChannel().flatMap(channel ->
                                    channel.createMessage(String.format(
                                            "Didn't recognize command <@%s>!",
                                            message.getAuthor().orElseThrow().getId().asLong()))).subscribe();
                        }
                    } catch (Exception ex) {
                        message.getChannel().flatMap(channel ->
                                        channel.createMessage(String.format(
                                                "Encountered an exception! <@%s> WIN OR DIE CHIMP!",
                                                config.getAdminId()))).subscribe();
                    }
                    return message;
                }).then();
    }
}

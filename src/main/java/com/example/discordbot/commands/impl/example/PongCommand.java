package com.example.discordbot.commands.impl.example;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.example.discordbot.commands.CommandHandler;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

@Command(name = "pong", description = "Replies with Pong!")
public class PongCommand implements CommandHandler {
    @Override
    public Mono<Void> execute(ChatInputInteractionEvent event) {
        return event.reply("Pong!");
    }
}

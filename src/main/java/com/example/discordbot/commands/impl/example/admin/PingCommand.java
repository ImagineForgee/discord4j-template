package com.example.discordbot.commands.impl.example.admin;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.clawsoftstudios.purrfectlib.annotations.CommandOption;
import com.clawsoftstudios.purrfectlib.annotations.OptionChoice;
import com.clawsoftstudios.purrfectlib.enums.OptionType;
import com.example.discordbot.commands.CommandHandler;
import com.example.discordbot.utils.OptionUtils;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

@Command(
        name = "ping",
        description = "Ping a domain or IP address.",
        options = {
                @CommandOption(
                        name = "id",
                        description = "Domain or IP to ping",
                        type = OptionType.STRING,
                        required = true,
                        choices = {
                                @OptionChoice(name = "Google", value = "google.com"),
                                @OptionChoice(name = "Cloudflare", value = "1.1.1.1"),
                                @OptionChoice(name = "OpenDNS", value = "208.67.222.222"),
                                @OptionChoice(name = "Localhost", value = "127.0.0.1")
                        }
                )
        }
)
public class PingCommand implements CommandHandler {
    @Override
    public Mono<Void> execute(ChatInputInteractionEvent event) {
        Optional<String> idOpt = OptionUtils.getAsString(event, "id");

        if (idOpt.isEmpty()) {
            return event.reply("Missing domain or IP address!").withEphemeral(true);
        }

        String id = idOpt.get();

        return Mono.fromCallable(() -> {
            try {
                InetAddress inet = InetAddress.getByName(id);
                boolean reachable = inet.isReachable(3000);
                return reachable ? "✅ " + id + " is reachable!" : "❌ " + id + " is NOT reachable.";
            } catch (IOException e) {
                return "⚠️ Failed to ping " + id + ": " + e.getMessage();
            }
        }).flatMap(result -> event.reply(result));
    }
}

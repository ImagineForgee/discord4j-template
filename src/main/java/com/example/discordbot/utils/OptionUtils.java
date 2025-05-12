package com.example.discordbot.utils;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;

import java.util.Optional;
import java.util.function.Function;

public class OptionUtils {

    public static Optional<String> getAsString(ChatInputInteractionEvent event, String optionName) {
        return findOptionValue(event, optionName, ApplicationCommandInteractionOptionValue::asString);
    }

    public static Optional<Double> getAsDouble(ChatInputInteractionEvent event, String optionName) {
        return findOptionValue(event, optionName, ApplicationCommandInteractionOptionValue::asDouble);
    }

    public static Optional<Boolean> getAsBoolean(ChatInputInteractionEvent event, String optionName) {
        return findOptionValue(event, optionName, ApplicationCommandInteractionOptionValue::asBoolean);
    }

    public static Optional<Snowflake> getAsSnowflake(ChatInputInteractionEvent event, String optionName) {
        return findOptionValue(event, optionName, ApplicationCommandInteractionOptionValue::asSnowflake);
    }

    private static <T> Optional<T> findOptionValue(ChatInputInteractionEvent event, String optionName, Function<ApplicationCommandInteractionOptionValue, T> mapper) {
        return event.getOptions().stream()
                .map(option -> findOptionRecursive(option, optionName, mapper))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private static <T> Optional<T> findOptionRecursive(ApplicationCommandInteractionOption option, String targetName, Function<ApplicationCommandInteractionOptionValue, T> mapper) {
        if (option.getName().equals(targetName)) {
            return option.getValue().map(mapper);
        }

        return option.getOptions().stream()
                .map(sub -> findOptionRecursive(sub, targetName, mapper))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}



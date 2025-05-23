package com.example.discordbot.managers;

import com.clawsoftstudios.purrfectlib.annotations.Command;
import com.clawsoftstudios.purrfectlib.enums.OptionType;
import com.clawsoftstudios.purrfectlib.manager.AbstractCommandManager;
import com.clawsoftstudios.purrfectlib.scanner.CommandInfo;
import com.clawsoftstudios.purrfectlib.scanner.CommandScanner;
import com.example.discordbot.Constants;
import com.example.discordbot.commands.CommandGroup;
import com.example.discordbot.commands.CommandHandler;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.*;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.util.*;

public class CommandManager extends AbstractCommandManager {

    private final Map<String, CommandHandler> standalone = new HashMap<>();
    private final Map<String, List<CommandHandler>> groupSubs = new HashMap<>();
    private final Map<String, Map<String, List<CommandHandler>>> subgroupSubs = new HashMap<>();
    private static final Map<String, CommandGroup> groupPermissions = new HashMap<>();
    private static final EnumMap<OptionType, ApplicationCommandOption.Type> OPTION_TYPE_MAP = new EnumMap<>(OptionType.class);

    public CommandManager(GatewayDiscordClient client) {
        long start = System.currentTimeMillis();
        registerCommands(client);
        Constants.LOG.info("Commands loaded in {}ms", System.currentTimeMillis() - start);
    }

    static {
        OPTION_TYPE_MAP.put(OptionType.STRING, ApplicationCommandOption.Type.STRING);
        OPTION_TYPE_MAP.put(OptionType.INTEGER, ApplicationCommandOption.Type.INTEGER);
        OPTION_TYPE_MAP.put(OptionType.BOOLEAN, ApplicationCommandOption.Type.BOOLEAN);
        OPTION_TYPE_MAP.put(OptionType.USER, ApplicationCommandOption.Type.USER);
        OPTION_TYPE_MAP.put(OptionType.CHANNEL, ApplicationCommandOption.Type.CHANNEL);
        OPTION_TYPE_MAP.put(OptionType.ROLE, ApplicationCommandOption.Type.ROLE);
        OPTION_TYPE_MAP.put(OptionType.MENTIONABLE, ApplicationCommandOption.Type.MENTIONABLE);
        OPTION_TYPE_MAP.put(OptionType.DOUBLE, ApplicationCommandOption.Type.NUMBER);
        OPTION_TYPE_MAP.put(OptionType.ATTACHMENT, ApplicationCommandOption.Type.ATTACHMENT);
    }

    private void registerCommands(GatewayDiscordClient client) {
        CommandScanner scanner = new CommandScanner("com.example.discordbot.commands.impl");
        List<CommandInfo> infos = scanner.scanCommands();

        for (CommandInfo info : infos) {
            try {
                CommandHandler handler = (CommandHandler) info.getCommandClass()
                        .getDeclaredConstructor().newInstance();
                List<String> groups = info.getGroups();
                String name = info.getName();

                if (groups.isEmpty()) {
                    standalone.put(name, handler);

                } else if (groups.size() == 1) {
                    String grp = groups.get(0);
                    groupSubs.computeIfAbsent(grp, k -> new ArrayList<>()).add(handler);

                } else {
                    String grp = groups.get(0), sub = groups.get(1);
                    subgroupSubs
                            .computeIfAbsent(grp, k -> new HashMap<>())
                            .computeIfAbsent(sub, k -> new ArrayList<>())
                            .add(handler);
                }

            } catch (Exception e) {
                Constants.LOG.error("Failed to register command {}", info.getCommandClass().getName(), e);
            }
        }

        Constants.LOG.info("Standalone: {}", standalone.keySet());
        groupSubs.forEach((g, list) ->
                Constants.LOG.info("Group '{}': {}", g,
                        list.stream().map(h -> h.getClass().getAnnotation(Command.class).name()).toList())
        );
        subgroupSubs.forEach((g, map) ->
                map.forEach((sub, list) ->
                        Constants.LOG.info("Subgroup '{} {}': {}", g, sub,
                                list.stream().map(h -> h.getClass().getAnnotation(Command.class).name()).toList())
                )
        );

        client.on(ReadyEvent.class, ev ->
                client.getRestClient().getApplicationId()
                        .flatMapMany(appId ->
                                client.getRestClient()
                                        .getApplicationService()
                                        .bulkOverwriteGuildApplicationCommand(appId, Long.parseLong("1234554264370544660"), createCommandRequests())
                        )
                        .then()
        ).subscribe();
    }

    private List<ApplicationCommandRequest> createCommandRequests() {
        List<ApplicationCommandRequest> reqs = new ArrayList<>();

        standalone.forEach((name, handler) -> {
            Command ann = handler.getClass().getAnnotation(Command.class);

            ImmutableApplicationCommandRequest.Builder builder = ApplicationCommandRequest.builder()
                    .name(name)
                    .description(ann.description());

            for (var opt : ann.options()) {
                ApplicationCommandOption.Type optionType = (ApplicationCommandOption.Type) map(opt.type());
                ImmutableApplicationCommandOptionData.Builder optionBuilder = ApplicationCommandOptionData.builder()
                        .name(opt.name())
                        .description(opt.description())
                        .type(optionType.getValue())
                        .required(opt.required());

                for (var choice : opt.choices()) {
                    ImmutableApplicationCommandOptionChoiceData.Builder choiceBuilder = ApplicationCommandOptionChoiceData.builder()
                            .name(choice.name())
                            .value(choice.value());
                    optionBuilder.addChoice(choiceBuilder.build());
                }

                builder.addOption(optionBuilder.build());
            }

            reqs.add(builder.build());
        });

        Set<String> allGroups = new HashSet<>();
        allGroups.addAll(groupSubs.keySet());
        allGroups.addAll(subgroupSubs.keySet());

        for (String grp : allGroups) {
            ImmutableApplicationCommandRequest.Builder b = ApplicationCommandRequest.builder()
                    .name(grp)
                    .description("Group: " + grp);

            List<CommandHandler> direct = groupSubs.getOrDefault(grp, List.of());
            for (CommandHandler h : direct) {
                Command ann = h.getClass().getAnnotation(Command.class);
                ImmutableApplicationCommandOptionData.Builder subCmdBuilder = ApplicationCommandOptionData.builder()
                        .name(ann.name())
                        .description(ann.description())
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue());

                for (var opt : ann.options()) {
                    ApplicationCommandOption.Type optionType = (ApplicationCommandOption.Type) map(opt.type());
                    ImmutableApplicationCommandOptionData.Builder subOptionBuilder = ApplicationCommandOptionData.builder()
                            .name(opt.name())
                            .description(opt.description())
                            .type(optionType.getValue())
                            .required(opt.required());

                    for (var choice : opt.choices()) {
                        ImmutableApplicationCommandOptionChoiceData.Builder choiceBuilder = ApplicationCommandOptionChoiceData.builder()
                                .name(choice.name())
                                .value(choice.value());
                        subOptionBuilder.addChoice(choiceBuilder.build());
                    }

                    subCmdBuilder.addOption(subOptionBuilder.build());
                }

                b.addOption(subCmdBuilder.build());
            }

            Map<String, List<CommandHandler>> subs = subgroupSubs.getOrDefault(grp, Map.of());
            for (var entry : subs.entrySet()) {
                String subGrp = entry.getKey();
                ImmutableApplicationCommandOptionData.Builder sg = ApplicationCommandOptionData.builder()
                        .name(subGrp)
                        .description("Subgroup: " + subGrp)
                        .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue());

                for (CommandHandler h : entry.getValue()) {
                    Command ann = h.getClass().getAnnotation(Command.class);
                    ImmutableApplicationCommandOptionData.Builder subCmdBuilder = ApplicationCommandOptionData.builder()
                            .name(ann.name())
                            .description(ann.description())
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue());

                    for (var opt : ann.options()) {
                        ApplicationCommandOption.Type optionType = (ApplicationCommandOption.Type) map(opt.type());
                        ImmutableApplicationCommandOptionData.Builder subOptionBuilder = ApplicationCommandOptionData.builder()
                                .name(opt.name())
                                .description(opt.description())
                                .type(optionType.getValue())
                                .required(opt.required());

                        for (var choice : opt.choices()) {
                            ImmutableApplicationCommandOptionChoiceData.Builder choiceBuilder = ApplicationCommandOptionChoiceData.builder()
                                    .name(choice.name())
                                    .value(choice.value());
                            subOptionBuilder.addChoice(choiceBuilder.build());
                        }

                        subCmdBuilder.addOption(subOptionBuilder.build());
                    }
                    sg.addOption(subCmdBuilder.build());
                }
                b.addOption(sg.build());
            }
            reqs.add(b.build());
        }

        return reqs;
    }

    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        String root = event.getCommandName();
        var options = event.getOptions();

        if (options.isEmpty()) {
            CommandHandler h = standalone.get(root);
            return h != null ? h.execute(event) : Mono.empty();
        }

        ApplicationCommandInteractionOption first = options.get(0);

        if (first.getType() == ApplicationCommandOption.Type.SUB_COMMAND_GROUP) {
            String subGroup = first.getName();
            List<ApplicationCommandInteractionOption> inner = first.getOptions();
            if (inner.isEmpty()) return Mono.empty();

            String cmd = inner.get(0).getName();
            CommandHandler handler = subgroupSubs.getOrDefault(root, Map.of())
                    .getOrDefault(subGroup, List.of())
                    .stream()
                    .filter(h -> h.getClass().getAnnotation(Command.class).name().equals(cmd))
                    .findFirst()
                    .orElse(null);

            if (handler == null) return Mono.empty();

            CommandGroup group = groupPermissions.get(root);
            if (group != null) {
                return userHasPermission(event, group.permissions()).flatMap(has -> {
                    if (!has) {
                        return event.reply("You do not have permission to use this command.")
                                .withEphemeral(true);
                    }
                    return handler.execute(event);
                });
            }

            return handler.execute(event);
        }

        if (first.getType() == ApplicationCommandOption.Type.SUB_COMMAND) {
            String cmd = first.getName();
            CommandHandler handler = groupSubs.getOrDefault(root, List.of())
                    .stream()
                    .filter(h -> h.getClass().getAnnotation(Command.class).name().equals(cmd))
                    .findFirst()
                    .orElse(null);

            if (handler == null) return Mono.empty();

            CommandGroup group = groupPermissions.get(root);
            if (group != null) {
                return userHasPermission(event, group.permissions()).flatMap(has -> {
                    if (!has) {
                        return event.reply("You do not have permission to use this command.")
                                .withEphemeral(true);
                    }
                    return handler.execute(event);
                });
            }

            return handler.execute(event);
        }

        return Mono.empty();
    }

    private Mono<Boolean> userHasPermission(ChatInputInteractionEvent event, List<Permission> perms) {
        return Mono.justOrEmpty(event.getInteraction().getMember())
                .flatMap(m -> m.getBasePermissions()
                        .map(base -> perms.stream().allMatch(base::contains)))
                .defaultIfEmpty(false);
    }

    public static void registerGroupPermission(String group, List<Permission> perms) {
        groupPermissions.put(group, new CommandGroup(group, perms));
    }

    @Override
    protected Object map(OptionType type) {
        return OPTION_TYPE_MAP.get(type);
    }
}


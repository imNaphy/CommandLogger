package xyz.naphy.commandlogger;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Plugin(
        id = "commandlogger",
        name = "Command Logger",
        version = "1.0",
        authors = "naphy"
)
public class CommandLogger {

    private final Logger logger;
    private final ProxyServer server;
    private List<String> blockedCommands = new ArrayList<>();
    private String staffMessage;
    private String consoleMessage;
    private ConfigurationNode node;
    @Inject
    @DataDirectory
    private final Path dataDirectory;

    @Inject
    public CommandLogger(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;

        logger.info("[CommandLogger] The plugin has been loaded!");
    }

    @Subscribe
    public void onProxyInitialization(final ProxyInitializeEvent event) throws IOException, ObjectMappingException {
        if (Files.notExists(dataDirectory)) {
            Files.createDirectory(dataDirectory);
        }
        final Path config = dataDirectory.resolve("config.yml");
        if (Files.notExists(config)) {
            try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                Files.copy(stream, config);
            }
        }
        final YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder().setPath(config).build();
        this.node = loader.load();
        final List<String> strings = node.getNode("blocked commands").getList(TypeToken.of(String.class));
        for (String x : strings) {
            this.blockedCommands.add(x.toLowerCase());
        }
        this.staffMessage = node.getNode("staff message").getString();
        this.consoleMessage = node.getNode("console message").getString();
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (event.getCommandSource().toString().split(" ")[0].equals("[connected")) {
            String player = event.getCommandSource().toString().split(" ")[2];
            if (this.blockedCommands.contains(event.getCommand().split(" ")[0].toLowerCase())) {
                final Component component = MiniMessage.miniMessage().deserialize(this.staffMessage.replace("%command%", event.getCommand().split(" ")[0].toLowerCase()).replace("%player%", player));
                for (Player x : server.getAllPlayers()) {
                    if (x.hasPermission("commandlogger.notify")) {
                        x.sendMessage(component);
                    }
                }
                System.out.println(this.consoleMessage.replace("%command%", event.getCommand().split(" ")[0].toLowerCase()).replace("%player%", player));
            }
        }
    }

    @Subscribe
    public void reloadCommand(CommandExecuteEvent event) throws IOException, ObjectMappingException {
        if (event.getCommand().equals("commandlogger reload")) {
            if (!event.getCommandSource().toString().split(" ")[0].equals("[connected")) {
                if (Files.notExists(dataDirectory)) {
                    Files.createDirectory(dataDirectory);
                }
                final Path config = dataDirectory.resolve("config.yml");
                if (Files.notExists(config)) {
                    try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("config.yml")) {
                        Files.copy(stream, config);
                    }
                }
                final YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder().setPath(config).build();
                this.node = loader.load();
                final List<String> strings = node.getNode("blocked commands").getList(TypeToken.of(String.class));
                this.blockedCommands = new ArrayList<>();
                for (String x : strings) {
                    this.blockedCommands.add(x.toLowerCase());
                }
                this.staffMessage = node.getNode("staff message").getString();
                this.consoleMessage = node.getNode("console message").getString();
                System.out.println("[CommandLogger] The plugin has been reloaded!");
            }
        }
    }
}

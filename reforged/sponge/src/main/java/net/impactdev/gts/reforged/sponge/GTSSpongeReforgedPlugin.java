package net.impactdev.gts.reforged.sponge;

import com.google.common.collect.Lists;
import net.impactdev.gts.reforged.sponge.entry.ReforgedEntry;
import net.impactdev.gts.reforged.sponge.price.ReforgedPrice;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.configuration.Config;
import net.impactdev.impactor.api.dependencies.Dependency;
import net.impactdev.impactor.api.event.annotations.Subscribe;
import net.impactdev.impactor.api.event.listener.ImpactorEventListener;
import net.impactdev.impactor.api.logging.Logger;
import net.impactdev.impactor.api.plugin.PluginMetadata;
import net.impactdev.impactor.sponge.configuration.SpongeConfig;
import net.impactdev.impactor.sponge.configuration.SpongeConfigAdapter;
import net.impactdev.impactor.sponge.logging.SpongeLogger;
import net.impactdev.gts.api.GTSService;
import net.impactdev.gts.api.events.extension.PlaceholderRegistryEvent;
import net.impactdev.gts.api.extension.Extension;
import net.impactdev.gts.common.config.updated.ConfigKeys;
import net.impactdev.gts.common.plugin.GTSPlugin;
import net.impactdev.gts.reforged.sponge.config.ReforgedConfigKeys;
import net.impactdev.gts.reforged.sponge.config.ReforgedLangConfigKeys;
import net.impactdev.gts.reforged.sponge.legacy.LegacyReforgedPokemonDeserializer;
import net.impactdev.gts.reforged.sponge.manager.ReforgedPokemonDataManager;
import net.impactdev.gts.reforged.sponge.placeholders.ReforgedPlaceholders;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.text.placeholder.PlaceholderParser;

import java.nio.file.Path;
import java.util.List;

/**
 *
 * For entry type displays with discord:
 * https://projectpokemon.org/images/normal-sprite/bulbasaur.gif
 * https://projectpokemon.org/images/shiny-sprite/bulbasaur.gif
 */
public class GTSSpongeReforgedPlugin implements Extension, ImpactorEventListener {

    private static GTSSpongeReforgedPlugin instance;

    private Logger logger;
    private final PluginMetadata metadata = PluginMetadata.builder()
            .id("reforged_extension")
            .name("GTS - Reforged Extension")
            .version("@version@")
            .build();

    private ReforgedPokemonDataManager manager;

    private Config extended;
    private Config lang;

    public GTSSpongeReforgedPlugin() {
        instance = this;
    }

    @Override
    public void load(GTSService service, Path dataDir) {
        this.logger = new SpongeLogger(this, LoggerFactory.getLogger(this.getMetadata().getName()));
        this.logger.debug("Initializing extension...");

        this.extended = new SpongeConfig(new SpongeConfigAdapter(this, dataDir.resolve("reforged").resolve("main.conf").toFile()), new ReforgedConfigKeys());
        this.lang = new SpongeConfig(new SpongeConfigAdapter(this, dataDir.resolve("reforged").resolve(GTSPlugin.getInstance().getConfiguration().get(ConfigKeys.LANGUAGE) + ".conf").toFile()), new ReforgedLangConfigKeys());

        service.getGTSComponentManager().registerLegacyEntryDeserializer("pokemon", new LegacyReforgedPokemonDeserializer());
        service.getGTSComponentManager().registerEntryManager(ReforgedEntry.class, this.manager = new ReforgedPokemonDataManager());
        service.getGTSComponentManager().registerPriceManager(ReforgedPrice.class, new ReforgedPrice.ReforgedPriceManager());

        Impactor.getInstance().getEventBus().subscribe(this);
    }

    @Override
    public void enable(GTSService service) {
        this.logger.debug("Enabling...");
    }

    @Override
    public List<Dependency> getRequiredDependencies() {
        return Lists.newArrayList(Dependency.PIXELMON_BRIDGE_API, Dependency.PIXELMON_BRIDGE_REFORGED);
    }

    @Override
    public void unload() {

    }

    public static GTSSpongeReforgedPlugin getInstance() {
        return instance;
    }

    public ReforgedPokemonDataManager getManager() {
        return this.manager;
    }

    @Override
    public Path getConfigDir() {
        return null;
    }

    @Override
    public Config getConfiguration() {
        return this.extended;
    }

    @Override
    public PluginMetadata getMetadata() {
        return this.metadata;
    }

    @Override
    public Logger getPluginLogger() {
        return this.logger;
    }

    @Override
    public Config getMsgConfig() {
        return this.lang;
    }

    @Subscribe
    public void onPlaceholderRegistrationEvent(PlaceholderRegistryEvent<GameRegistryEvent.Register<PlaceholderParser>> event) {
        ReforgedPlaceholders placeholders = new ReforgedPlaceholders();
        placeholders.register(event.getManager());
    }

}
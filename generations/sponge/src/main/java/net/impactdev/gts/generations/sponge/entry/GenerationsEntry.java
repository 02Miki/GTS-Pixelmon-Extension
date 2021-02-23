package net.impactdev.gts.generations.sponge.entry;

import com.google.common.collect.Lists;
import com.pixelmongenerations.common.battle.BattleRegistry;
import com.pixelmongenerations.common.entity.pixelmon.EntityPixelmon;
import com.pixelmongenerations.common.item.ItemPixelmonSprite;
import com.pixelmongenerations.core.Pixelmon;
import com.pixelmongenerations.core.config.PixelmonEntityList;
import com.pixelmongenerations.core.config.PixelmonItems;
import com.pixelmongenerations.core.enums.EnumSpecies;
import com.pixelmongenerations.core.storage.NbtKeys;
import com.pixelmongenerations.core.storage.PixelmonStorage;
import com.pixelmongenerations.core.storage.PlayerStorage;
import net.impactdev.gts.api.blacklist.Blacklist;
import net.impactdev.gts.api.data.registry.GTSKeyMarker;
import net.impactdev.gts.api.listings.Listing;
import net.impactdev.gts.api.listings.makeup.Display;
import net.impactdev.gts.api.listings.prices.PriceControlled;
import net.impactdev.gts.common.config.MsgConfigKeys;
import net.impactdev.gts.common.plugin.GTSPlugin;
import net.impactdev.gts.generations.sponge.GTSSpongeGenerationsPlugin;
import net.impactdev.gts.generations.sponge.config.GenerationsLangConfigKeys;
import net.impactdev.gts.generations.sponge.converter.JObjectConverter;
import net.impactdev.gts.sponge.listings.makeup.SpongeDisplay;
import net.impactdev.gts.sponge.listings.makeup.SpongeEntry;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.configuration.Config;
import net.impactdev.impactor.api.json.factory.JObject;
import net.impactdev.impactor.api.services.text.MessageService;
import net.impactdev.pixelmonbridge.details.SpecKeys;
import net.impactdev.pixelmonbridge.generations.GenerationsPokemon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@GTSKeyMarker("generations-pokemon")
public class GenerationsEntry extends SpongeEntry<GenerationsPokemon> implements PriceControlled {

    private GenerationsPokemon pokemon;

    public GenerationsEntry(GenerationsPokemon pokemon) {
        this.pokemon = pokemon;
    }

    @Override
    public GenerationsPokemon getOrCreateElement() {
        return this.pokemon;
    }

    @Override
    public TextComponent getName() {
        return Component.text(this.pokemon.getOrCreate().getSpecies().getPokemonName());
    }

    @Override
    public TextComponent getDescription() {
        return this.getName();
    }

    @Override
    public Display<ItemStack> getDisplay(UUID viewer, Listing listing) {
        final MessageService<Text> service = Impactor.getInstance().getRegistry().get(MessageService.class);

        List<Text> lore = Lists.newArrayList();
        lore.addAll(service.parse(GTSSpongeGenerationsPlugin.getInstance().getMsgConfig().get(GenerationsLangConfigKeys.POKEMON_DETAILS), Lists.newArrayList(() -> this.pokemon)));

        ItemStack rep = ItemStack.builder()
                .from(this.getPicture(this.pokemon.getOrCreate()))
                .add(Keys.DISPLAY_NAME, service.parse(GTSSpongeGenerationsPlugin.getInstance().getMsgConfig()
                        .get(GenerationsLangConfigKeys.POKEMON_TITLE), Lists.newArrayList(() -> this.pokemon))
                )
                .add(Keys.ITEM_LORE, lore)
                .build();

        return new SpongeDisplay(rep);
    }

    @Override
    public boolean give(UUID receiver) {
        PixelmonStorage.pokeBallManager.getPlayerStorageFromUUID(receiver).get().addToParty(this.pokemon.getOrCreate());
        return true;
    }

    @Override
    public boolean take(UUID depositor) {
        Optional<Player> user = Sponge.getServer().getPlayer(depositor);
        Config mainLang = GTSPlugin.getInstance().getMsgConfig();
        Config reforgedLang = GTSSpongeGenerationsPlugin.getInstance().getMsgConfig();

        MessageService<Text> parser = Impactor.getInstance().getRegistry().get(MessageService.class);

        PlayerStorage party = PixelmonStorage.pokeBallManager.getPlayerStorageFromUUID(depositor).get();
        if(BattleRegistry.getBattle(party.getPlayer()) != null) {
            user.ifPresent(player -> player.sendMessages(parser.parse(reforgedLang.get(GenerationsLangConfigKeys.ERROR_IN_BATTLE))));
            return false;
        }

        boolean blacklisted = Impactor.getInstance().getRegistry()
                .get(Blacklist.class)
                .isBlacklisted(EnumSpecies.class, this.pokemon.getOrCreate().getSpecies().name);
        if(blacklisted) {
            user.ifPresent(player -> player.sendMessages(parser.parse(mainLang.get(MsgConfigKeys.GENERAL_FEEDBACK_BLACKLISTED))));
            return false;
        }

        // Check party size. Ensure we aren't less than 1 because who knows whether Reforged or another plugin
        // will break something
        if(party.getTeam().size() <= 1) {
            user.ifPresent(player -> player.sendMessages(parser.parse(reforgedLang.get(GenerationsLangConfigKeys.ERROR_LAST_ABLE_MEMBER))));
            return false;
        }

        party.recallAllPokemon();
        party.removeFromPartyPlayer(party.getPosition(this.pokemon.getOrCreate().getPokemonId()));
        return true;
    }

    @Override
    public Optional<String> getThumbnailURL() {
        StringBuilder url = new StringBuilder("https://projectpokemon.org/images/");
        if(this.pokemon.get(SpecKeys.SHINY).orElse(false)) {
            url.append("shiny");
        } else {
            url.append("normal");
        }

        url.append("-sprite/");
        url.append(this.pokemon.getOrCreate().getSpecies().name.toLowerCase());
        url.append(".gif");
        return Optional.of(url.toString());
    }

    @Override
    public List<String> getDetails() {
        MessageService<Text> parser = Impactor.getInstance().getRegistry().get(MessageService.class);
        Config reforgedLang = GTSSpongeGenerationsPlugin.getInstance().getMsgConfig();

        return parser.parse(reforgedLang.get(GenerationsLangConfigKeys.DISCORD_DETAILS), Lists.newArrayList(this::getOrCreateElement))
                .stream()
                .map(Text::toPlain)
                .collect(Collectors.toList());
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public JObject serialize() {
        // Since the custom serializer for the Cross Pixelmon Library uses it's own version of
        // JObject, as to not rely on Impactor, we need to convert between the two objects
        return new JObject()
                .add("pokemon", JObjectConverter.convert(GTSSpongeGenerationsPlugin.getInstance()
                        .getManager()
                        .getInternalManager()
                        .serialize(this.pokemon)
                ))
                .add("version", this.getVersion());
    }

    @Override
    public double getMin() {
        return 0;
    }

    @Override
    public double getMax() {
        return 0;
    }

    private ItemStack getPicture(EntityPixelmon pokemon) {
        Calendar calendar = Calendar.getInstance();

        boolean aprilFools = false;
        if(calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            aprilFools = true;
        }

        if(pokemon.isEgg) {
            net.minecraft.item.ItemStack item = new net.minecraft.item.ItemStack(PixelmonItems.itemPixelmonSprite);
            NBTTagCompound nbt = new NBTTagCompound();
            switch (pokemon.getSpecies()) {
                case Manaphy:
                case Togepi:
                    nbt.setString(NbtKeys.SPRITE_NAME,
                            String.format("pixelmon:sprites/eggs/%s1", pokemon.getSpecies().name.toLowerCase()));
                    break;
                default:
                    nbt.setString(NbtKeys.SPRITE_NAME, "pixelmon:sprites/eggs/egg1");
                    break;
            }
            item.setTagCompound(nbt);
            return (ItemStack) (Object) item;
        } else {
            World world = (World) (Object) Sponge.getServer().getWorlds().iterator().next();
            return (ItemStack) (Object) (aprilFools ? ItemPixelmonSprite.getPhoto((EntityPixelmon) PixelmonEntityList.createEntityByName(EnumSpecies.Bidoof.name, world)) : ItemPixelmonSprite.getPhoto(pokemon));
        }
    }

}

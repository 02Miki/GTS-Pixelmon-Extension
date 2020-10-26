package net.impactdev.gts.reforged.sponge.entry;

import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.configuration.Config;
import net.impactdev.impactor.api.json.factory.JObject;
import net.impactdev.impactor.api.services.text.MessageService;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.config.PixelmonItems;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite;
import com.pixelmonmod.pixelmon.storage.NbtKeys;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import net.impactdev.gts.api.blacklist.Blacklist;
import net.impactdev.gts.api.data.registry.GTSKeyMarker;
import net.impactdev.gts.api.listings.Listing;
import net.impactdev.gts.api.listings.auctions.Auction;
import net.impactdev.gts.api.listings.buyitnow.BuyItNow;
import net.impactdev.gts.api.listings.makeup.Display;
import net.impactdev.gts.common.config.MsgConfigKeys;
import net.impactdev.gts.common.plugin.GTSPlugin;
import net.impactdev.gts.reforged.sponge.GTSSpongeReforgedPlugin;
import net.impactdev.gts.reforged.sponge.config.ReforgedLangConfigKeys;
import net.impactdev.gts.reforged.sponge.converter.JObjectConverter;
import net.impactdev.gts.sponge.listings.makeup.SpongeDisplay;
import net.impactdev.gts.sponge.listings.makeup.SpongeEntry;
import net.impactdev.gts.sponge.utils.Utilities;
import net.impactdev.pixelmonbridge.details.SpecKeys;
import net.impactdev.pixelmonbridge.reforged.ReforgedPokemon;
import net.kyori.text.TextComponent;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@GTSKeyMarker("reforged-pokemon")
public class ReforgedEntry extends SpongeEntry<ReforgedPokemon> {

    public ReforgedPokemon pokemon;

    public ReforgedEntry(ReforgedPokemon pokemon) {
        this.pokemon = pokemon;
    }

    @Override
    public ReforgedPokemon getOrCreateElement() {
        return this.pokemon;
    }

    @Override
    public TextComponent getName() {
        return TextComponent.builder(this.pokemon.getOrCreate().getSpecies().getLocalizedName()).build();
    }

    @Override
    public TextComponent getDescription() {
        return this.getName();
    }

    @Override
    public Display<ItemStack> getDisplay(UUID viewer, Listing listing) {
        final MessageService<Text> service = Impactor.getInstance().getRegistry().get(MessageService.class);

        List<Text> lore = Lists.newArrayList();
        lore.addAll(service.parse(GTSSpongeReforgedPlugin.getInstance().getMsgConfig().get(ReforgedLangConfigKeys.POKEMON_DETAILS), Lists.newArrayList(() -> this.pokemon)));

        ItemStack rep = ItemStack.builder()
                .from(this.getPicture(this.pokemon.getOrCreate()))
                .add(Keys.DISPLAY_NAME, service.parse(GTSSpongeReforgedPlugin.getInstance().getMsgConfig()
                        .get(ReforgedLangConfigKeys.POKEMON_TITLE), Lists.newArrayList(() -> this.pokemon))
                )
                .add(Keys.ITEM_LORE, lore)
                .build();

        return new SpongeDisplay(rep);
    }

    @Override
    public boolean give(UUID receiver) {
        PlayerPartyStorage storage = Pixelmon.storageManager.getParty(receiver);
        if(!storage.hasSpace()) {
            PCStorage pc = Pixelmon.storageManager.getPCForPlayer(receiver);
            if(!pc.hasSpace()) {
                return false;
            }
        }

        return storage.add(this.pokemon.getOrCreate());
    }

    @Override
    public boolean take(UUID depositor) {
        Config mainLang = GTSPlugin.getInstance().getMsgConfig();

        MessageService<Text> parser = Impactor.getInstance().getRegistry().get(MessageService.class);

        PlayerPartyStorage party = Pixelmon.storageManager.getParty(depositor);
        if(BattleRegistry.getBattle(party.getPlayer()) != null) {
            return false;
        }

        // TODO - Flag checks

        boolean blacklisted = Impactor.getInstance().getRegistry()
                .get(Blacklist.class)
                .isBlacklisted(EnumSpecies.class, this.pokemon.getOrCreate().getSpecies().name);
        if(blacklisted) {
            return false;
        }

        party.retrieveAll();
        party.set(party.getPosition(this.pokemon.getOrCreate()), null);
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
        return Lists.newArrayList();
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
                .add("pokemon", JObjectConverter.convert(GTSSpongeReforgedPlugin.getInstance()
                        .getManager()
                        .getInternalManager()
                        .serialize(this.pokemon)
                ))
                .add("version", this.getVersion());
    }

    private ItemStack getPicture(Pokemon pokemon) {
        Calendar calendar = Calendar.getInstance();

        boolean aprilFools = false;
        if(calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            aprilFools = true;
        }

        if(pokemon.isEgg()) {
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
            return (ItemStack) (Object) (aprilFools ? ItemPixelmonSprite.getPhoto(Pixelmon.pokemonFactory.create(EnumSpecies.Bidoof)) : ItemPixelmonSprite.getPhoto(pokemon));
        }
    }
}

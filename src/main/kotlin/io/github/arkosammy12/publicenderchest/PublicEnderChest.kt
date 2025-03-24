package io.github.arkosammy12.publicenderchest

import com.mojang.serialization.Codec
import io.github.arkosammy12.publicenderchest.config.ConfigUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.EnderChestBlock
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.github.arkosammy12.monkeyconfig.base.ConfigManager
import io.github.arkosammy12.monkeyconfig.builders.tomlConfigManager
import io.github.arkosammy12.monkeyconfig.managers.getRawNumberSettingValue
import io.github.arkosammy12.monkeyutils.registrars.DefaultConfigRegistrar
import io.github.arkosammy12.monkeyutils.settings.CommandBooleanSetting
import io.github.arkosammy12.monkeyutils.settings.CommandNumberSetting
import io.github.arkosammy12.publicenderchest.PublicEnderChest.MOD_ID
import io.github.arkosammy12.publicenderchest.inventory.PublicInventoryManager
import io.github.arkosammy12.publicenderchest.logging.InventoryDatabaseManager
import io.github.arkosammy12.publicenderchest.util.Events
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object PublicEnderChest : ModInitializer {

	const val MOD_ID: String = "publicenderchest"
	const val PUBLIC_INVENTORY_NAME = "Public Ender Chest"
	val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
	//val CONFIG_MANAGER: ConfigManager = TomlConfigManager(MOD_ID, SettingGroups.settingGroups, ConfigSettings.settingBuilders)
	val CONFIG_MANAGER: ConfigManager = tomlConfigManager(MOD_ID, FabricLoader.getInstance().configDir.resolve("$MOD_ID.toml")) {
		section("player_blacklist") {
			comment = "Add players to a blacklist to prevent them from being able to use the public ender chest."
			ConfigUtils.ENABLE_PLAYER_BLACKLIST = booleanSetting("enable_player_blacklist", false) {
				comment = "(Default = false) Toggle the use of the player blacklist feature."
				implementation = ::CommandBooleanSetting
			}
			ConfigUtils.PLAYER_BLACKLIST = stringListSetting("player_blacklist", mutableListOf()) {
				comment = "Add players to this blacklist by specifying their username."
			}
		}
		section("dimension_blacklist") {
			comment = "Add dimension identifiers to this blacklist to prevent players from being able to use the public ender chest in those dimensions."
			ConfigUtils.ENABLE_DIMENSION_BLACKLIST = booleanSetting("enable_dimension_blacklist", false) {
				comment = "(Default = false) Toggle the use of the dimension blacklist feature."
				implementation = ::CommandBooleanSetting
			}
			ConfigUtils.DIMENSION_BLACKLIST = stringListSetting("dimension_blacklist", mutableListOf()) {
				comment = "Add dimensions to this blacklist by specifying their full identifier. For example, to add the Overworld dimension, you can add the identifier \"minecraft:overworld\" to this list."
			}
		}
		section("database") {
			comment = "Configure settings related to the mod's database."
			ConfigUtils.PURGE_OLDER_THAN_X_DAYS = numberSetting<Int>("purge_older_than_x_days", 30) {
				comment = "(Default = 30) Purge database entries older than the amount of days specified. The database is purged on every server shutdown and using the `/publicenderchest database purge` command."
				minValue = 0
				implementation = ::CommandNumberSetting
			}
		}
	}
	val INVENTORY_MANAGER: PublicInventoryManager = PublicInventoryManager()
	lateinit var DATABASE_MANAGER: InventoryDatabaseManager
		private set
	val USING_PUBLIC_INVENTORY: AttachmentType<Boolean> = run {
		val builder: AttachmentRegistry.Builder<Boolean> = AttachmentRegistryImpl.builder()
		builder.copyOnDeath().persistent(Codec.BOOL).initializer {true}.buildAndRegister(Identifier.of(MOD_ID, "using_public_inventory"))
	}

	override fun onInitialize() {
		DefaultConfigRegistrar.registerConfigManager(CONFIG_MANAGER)
		ServerLifecycleEvents.SERVER_STARTING.register(::onServerStarting)
		ServerLifecycleEvents.SERVER_STOPPING.register(::onServerStopping)
		Events.registerEvents()
	}

	private fun onServerStarting(server: MinecraftServer) {
		DATABASE_MANAGER = InventoryDatabaseManager(server)
		INVENTORY_MANAGER.onServerStarting(server)
	}

	private fun onServerStopping(server: MinecraftServer) {
		INVENTORY_MANAGER.onServerStopping(server)
		DATABASE_MANAGER.purge(server, CONFIG_MANAGER.getRawNumberSettingValue<Int>(ConfigUtils.PURGE_OLDER_THAN_X_DAYS) ?: 30)
	}

}

fun getModFolderPath(server: MinecraftServer) : Path {
	val path: Path = server.getSavePath(WorldSavePath.ROOT).resolve(MOD_ID)
	try {
		if (!Files.exists(path)) {
			Files.createDirectory(path)
		}
	} catch (e: Exception) {
		PublicEnderChest.LOGGER.error("Error attempting to create mod directory: $e")
	}
	return path
}

fun BlockState.isEnderChest() : Boolean = this.isOf(Blocks.ENDER_CHEST)
fun ItemStack.isEnderChest() : Boolean {
	val item: Item = this.item
	if (item !is BlockItem) {
		return false
	}
	return item.block is EnderChestBlock
}

fun formatInfoTextMetadata(syncId: Int, revision: Int) : String =
	"publicenderchest:CustomText[syncId=$syncId, revision=$revision]"

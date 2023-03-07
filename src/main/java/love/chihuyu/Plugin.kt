package love.chihuyu

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent
import love.chihuyu.utils.ItemUtil
import love.chihuyu.utils.runTaskLater
import love.chihuyu.utils.runTaskTimer
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class Plugin : JavaPlugin(), Listener {

    companion object {
        lateinit var plugin: JavaPlugin
    }

    init {
        plugin = this
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)

        commandAPICommand("giveHookshot") {
            playerExecutor { player, args ->
                player.inventory.addItem(
                    ItemUtil.create(Material.CROSSBOW, customModelData = 1, name = "${ChatColor.GOLD}${ChatColor.BOLD}${ChatColor.ITALIC}Hookshot")
                )
            }
        }
    }

    @EventHandler
    fun onLoad(e: EntityLoadCrossbowEvent) {
        val player = e.entity as? Player ?: return
        val item = e.crossbow
        val density = 20

        if (item?.itemMeta?.hasCustomModelData() == false) return
        if (item?.itemMeta?.customModelData != 1) return

        player.setMetadata("hookshot_isHooked", FixedMetadataValue(plugin, true))
        player.removeMetadata("hookshot_isHookable", this)

        plugin.runTaskTimer(0 ,1) {

            val hookable = player.rayTraceBlocks(80.0, FluidCollisionMode.NEVER)?.hitPosition == null
            if (player.getMetadata("hookshot_isHookable").isNotEmpty()) {
                if (player.getMetadata("hookshot_isHookable")[0].asBoolean() != hookable) {
                    player.playSound(Sound.sound(Key.key("entity.item.pickup"), Sound.Source.AMBIENT, 1f, 1f))
                }
            }
            player.setMetadata("hookshot_isHookable", FixedMetadataValue(plugin, hookable))

            repeat(density) {
                val pos = player.rayTraceBlocks(80.0, FluidCollisionMode.NEVER)?.hitPosition
                val fixedPos = pos ?: player.location.toVector().add(player.location.direction.multiply(80))
                var playerPos = player.location.toVector().subtract(pos ?: fixedPos)
                playerPos = playerPos.subtract(Vector((playerPos.x / density) * it, (playerPos.y / density) * it, (playerPos.z / density) * it))
                player.spawnParticle(
                    Particle.REDSTONE,
                    playerPos.x + fixedPos.x,
                    playerPos.y + fixedPos.y + (1.3 * ((1.0 / density) * (density - it))),
                    playerPos.z + fixedPos.z,
                    1, .0, .0, .0, 1.0, DustOptions(if (pos == null) Color.RED else Color.LIME, .7f)
                )
            }
            if (player.getMetadata("hookshot_isHooked").isEmpty()) cancel()
            if (!player.getMetadata("hookshot_isHooked")[0].asBoolean()) cancel()
        }
    }

    @EventHandler
    fun onLaunch(e: EntityShootBowEvent) {
        if (e.bow?.itemMeta?.hasCustomModelData() == false) return
        if (e.bow?.itemMeta?.customModelData != 1) return

        val player = e.entity as? Player ?: return
        if (player.getMetadata("hookshot_isHooked").isEmpty()) return
        if (!player.getMetadata("hookshot_isHooked")[0].asBoolean()) return

        player.setMetadata("hookshot_isHooked", FixedMetadataValue(this, false))
        e.isCancelled = player.rayTraceBlocks(80.0, FluidCollisionMode.NEVER)?.hitPosition == null

        val targetPos = player.rayTraceBlocks(80.0, FluidCollisionMode.NEVER)?.hitPosition ?: return
        val proj = e.projectile as? Arrow ?: return
        val pos = player.location.toVector()
        val diff = targetPos.subtract(pos)

        proj.velocity = diff.multiply(0.05).add(Vector(.0, .44, .0)).setX(diff.x * .13).setZ(diff.z * .13)
        proj.addPassenger(player)
        proj.setGravity(true)
        proj.isSilent = true
        proj.isCritical = false

        player.velocity = player.velocity.add(diff.setY(diff.y / 2))
        player.playSound(Sound.sound(Key.key("block.anvil.place"), Sound.Source.AMBIENT, 1f, .6f))

        var count = 0
        plugin.runTaskTimer(0, 1) {
            count++
            player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.AMBIENT, 1f, 2f))
            proj.velocity = proj.velocity.multiply(Vector(1.148, 1.0, 1.148))
            if (count == 24) {
                proj.remove()
                plugin.runTaskLater(1) {
                    player.velocity = player.location.direction.multiply(Vector(0.24, 1.0, 0.24)).setY(0.7)
                }
                player.playSound(Sound.sound(Key.key("block.anvil.place"), Sound.Source.AMBIENT, 1f, .2f))
                cancel()
            }
        }
    }
}
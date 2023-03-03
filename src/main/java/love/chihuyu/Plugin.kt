package love.chihuyu

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent
import love.chihuyu.utils.ItemUtil
import love.chihuyu.utils.runTaskLater
import love.chihuyu.utils.runTaskTimer
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.ChatColor
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerMoveEvent
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
        val pos = player.rayTraceBlocks(80.0, FluidCollisionMode.NEVER)?.hitPosition ?: return
        val item = e.crossbow

        if (item?.itemMeta?.customModelData != 1) return

        player.playSound(Sound.sound(Key.key("item.trident.throw"), Sound.Source.AMBIENT, 1f, 1f))
        player.setMetadata("hookshot_isHooked", FixedMetadataValue(plugin, true))
        player.setMetadata("hookshot_hookedPoint_x", FixedMetadataValue(this, pos.x))
        player.setMetadata("hookshot_hookedPoint_y", FixedMetadataValue(this, pos.y))
        player.setMetadata("hookshot_hookedPoint_z", FixedMetadataValue(this, pos.z))

        plugin.runTaskLater(20) {
            player.playSound(Sound.sound(Key.key("item.trident.hit_ground"), Sound.Source.AMBIENT, 1f, 1f))
        }

        plugin.runTaskTimer(0 ,1) {
            repeat(20) {
                var playerPos = player.location.toVector().subtract(pos)
                playerPos = playerPos.subtract(Vector((playerPos.x / 20) * it, (playerPos.y / 20) * it, (playerPos.z / 20) * it))
                plugin.runTaskLater(it * 1L) {
                    player.world.spawnParticle(Particle.CRIT, playerPos.x + pos.x, playerPos.y + pos.y + 1.3 * (1 / 20.0) * (20 - it), playerPos.z + pos.z, 1, .0, .0, .0, .0)
                }
            }
            if (!player.getMetadata("hookshot_isHooked")[0].asBoolean()) cancel()
        }
    }

    @EventHandler
    fun onLaunch(e: EntityShootBowEvent) {
        val player = e.entity as? Player ?: return
        val proj = e.projectile as? Arrow ?: return

        if (!player.getMetadata("hookshot_isHooked")[0].asBoolean()) return
        val pos = player.location.toVector()
        val target = Vector(
            player.getMetadata("hookshot_hookedPoint_x")[0].asDouble(),
            player.getMetadata("hookshot_hookedPoint_y")[0].asDouble(),
            player.getMetadata("hookshot_hookedPoint_z")[0].asDouble()
        )
        val diff = target.subtract(pos)
        proj.velocity = diff.multiply(0.05).add(Vector(.0, .44, .0)).setX(diff.x * .13).setZ(diff.z * .13)
        proj.addPassenger(player)
        proj.setGravity(true)
        proj.isSilent = true
        proj.isCritical = false
        player.setMetadata("hookshot_isHooked", FixedMetadataValue(this, false))
        player.velocity = player.velocity.add(diff.setY(diff.y / 2))

        var count = 0
        plugin.runTaskTimer(0, 1) {
            repeat(20) {
                var playerPos = player.location.toVector().subtract(pos)
                playerPos = playerPos.subtract(Vector((playerPos.x / 20) * it, (playerPos.y / 20) * it, (playerPos.z / 20) * it))
                plugin.runTaskLater(it * 1L) {
                    player.world.spawnParticle(Particle.CRIT, playerPos.x + pos.x, playerPos.y + pos.y + 1.3 * (1 / 20.0) * (20 - it), playerPos.z + pos.z, 1, .0, .0, .0, .0)
                }
            }
            player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.AMBIENT, 1f, 2f))
            proj.velocity = proj.velocity.multiply(Vector(1.148, 1.0, 1.148))
            count++
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

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        val player = e.player
        val pos = player.location.toVector()

        if (!player.getMetadata("hookshot_isHooked")[0].asBoolean()) return
        val target = Vector(
            player.getMetadata("hookshot_hookedPoint_x")[0].asDouble(),
            player.getMetadata("hookshot_hookedPoint_y")[0].asDouble(),
            player.getMetadata("hookshot_hookedPoint_z")[0].asDouble()
        )
        val diff = target.subtract(pos)
        if (pos.distance(target) <= 170) return

        player.velocity = player.velocity.add(Vector(.0021 * diff.x, .001 * diff.y, .0021 * diff.z))
    }
}
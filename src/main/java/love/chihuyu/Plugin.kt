package love.chihuyu

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent
import love.chihuyu.utils.runTaskTimer
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Particle
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
    }

    @EventHandler
    fun onLoad(e: EntityLoadCrossbowEvent) {
        val player = e.entity as? Player ?: return
        val pos = player.rayTraceBlocks(80.0, FluidCollisionMode.NEVER)?.hitPosition ?: return
        player.sendMessage("Hooked!")
        player.setMetadata("hookshot_isHooked", FixedMetadataValue(this, true))
        player.setMetadata("hookshot_hookedPoint_x", FixedMetadataValue(this, pos.x))
        player.setMetadata("hookshot_hookedPoint_y", FixedMetadataValue(this, pos.y))
        player.setMetadata("hookshot_hookedPoint_z", FixedMetadataValue(this, pos.z))

        plugin.runTaskTimer(0 ,1) {
            repeat(20) {
                var playerPos = player.location.toVector().subtract(pos)
                playerPos = playerPos.subtract(Vector((playerPos.x / 20) * it, (playerPos.y / 20) * it, (playerPos.z / 20) * it))
                player.world.spawnParticle(Particle.CRIT, playerPos.x + pos.x, playerPos.y + pos.y + 1.3 * (1 / 20.0) * (20 - it), playerPos.z + pos.z, 1, .0, .0, .0, .0)
            }
            if (!player.getMetadata("hookshot_isHooked")[0].asBoolean()) cancel()
        }
    }

    @EventHandler
    fun onLaunch(e: EntityShootBowEvent) {
        val player = e.entity as? Player ?: return
        val proj = e.projectile

        if (player.getMetadata("hookshot_isHooked")[0].asBoolean()) {
            proj.remove()
            player.setMetadata("hookshot_isHooked", FixedMetadataValue(this, false))
            player.teleport(Location(
                player.world,
                player.getMetadata("hookshot_hookedPoint_x")[0].asDouble(),
                player.getMetadata("hookshot_hookedPoint_y")[0].asDouble(),
                player.getMetadata("hookshot_hookedPoint_z")[0].asDouble()
            ))
            player.sendMessage("Jumped!")
        }
    }
}
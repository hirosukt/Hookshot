package love.chihuyu

import io.papermc.paper.event.entity.EntityLoadCrossbowEvent
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin

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
        val block = player.rayTraceBlocks(40.0, FluidCollisionMode.NEVER)?.hitBlock ?: return
//        if (e.crossbow?.type != Material.ARROW) return
        player.sendMessage("Hooked!")
        player.setMetadata("hookshot_isHooked", FixedMetadataValue(this, true))
        player.setMetadata("hookshot_hookedPoint_x", FixedMetadataValue(this, block.location.x))
        player.setMetadata("hookshot_hookedPoint_y", FixedMetadataValue(this, block.location.y))
        player.setMetadata("hookshot_hookedPoint_z", FixedMetadataValue(this, block.location.z))
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
package moe.pizza.analyser

import moe.pizza.zkapi.zkillboard.Killmail
import moe.pizza.zkapi.WebsocketFeed
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class SuperDetector(url: String) {

  val logger = LoggerFactory.getLogger(classOf[SuperDetector])

  val supers = Map(
    22852L -> "Hel",
    23913L -> "Nyx",
    23919L -> "Aeon",
    23917L -> "Wyvern",
    3514L -> "Revenant",
    23773L -> "Ragnarok",
    671L -> "Erebus",
    11567L -> "Avatar",
    3764L -> "Leviathan"
  )

  val client = WebsocketFeed.createClient(url, { kill: Killmail =>

    if (supers.keySet contains kill.victim.shipTypeID) {
      // the person who died was in a super
      logger.info("victim %s id %d died in a %s in %s".format(kill.victim.characterName, kill.victim.characterID, supers(kill.victim.shipTypeID), kill.solarSystemID))
    }
    kill.attackers.foreach { attacker =>
      if(supers.keySet contains attacker.shipTypeID) {
        // attacker was in a super
        logger.info("%s id %d used a %s in %s".format(attacker.characterName, attacker.characterID, supers(attacker.shipTypeID), kill.solarSystemID))
      }
    }
  })

  Thread.sleep(60.seconds.toMillis)

}

object SuperDetector extends App {
  new SuperDetector("ws://ws.eve-kill.net/kills")
}
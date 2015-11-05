package moe.pizza.analyser

import moe.pizza.zkapi.zkillboard.Killmail
import moe.pizza.zkapi.WebsocketFeed

class SuperDetector(url: String) {

  val supers = Map(
    22852 -> "Hel",
    23913 -> "Nyx",
    23919 -> "Aeon",
    23917 -> "Wyvern",
    3514 -> "Revenant",
    23773 -> "Ragnarok",
    671 -> "Erebus",
    11567 -> "Avatar",
    3764 -> "Leviathan"
  )

  val client = WebsocketFeed.createClient(url, { kill: Killmail =>
    if (supers.keySet contains kill.victim.shipTypeID.toInt) {
      // the person who died was in a super
      println("victim %s id %d died in a %s in %s".format(kill.victim.characterName, kill.victim.characterID.toInt, supers(kill.victim.shipTypeID.toInt), kill.solarSystemID))
    }
    kill.attackers.foreach { attacker =>
      if(supers.keySet contains attacker.shipTypeID.toInt) {
        // attacker was in a super
        println("%s id %d used a %s in %s".format(attacker.characterName, attacker.characterID.toInt, supers(attacker.shipTypeID.toInt), kill.solarSystemID))
      }
    }
  })

}

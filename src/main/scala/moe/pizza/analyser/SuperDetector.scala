package moe.pizza.analyser

import moe.pizza.zkapi.WebsocketFeed
import moe.pizza.zkapi.zkillboard.Killmail
import org.slf4j.LoggerFactory
import moe.pizza.sdeapi._
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.duration._

class SuperDetector(url: String, db: DatabaseOps) {

  val logger = LoggerFactory.getLogger(classOf[SuperDetector])

  val supers: Set[String] = Set("Hel", "Nyx", "Aeon", "Wyvern", "Revenant")
  val titans: Set[String] = Set("Ragnarok", "Erebus", "Avatar", "Leviathan")
  val combined = supers ++ titans

  val targets = combined.flatMap( name =>
    db.getTypeID(name).sync() match {
      case Some(i) => Some((i, name))
      case None => None
    }
  ).toMap

  val client = WebsocketFeed.createClient(url, { kill: Killmail =>

    //logger.info("%s lost a %s in %s".format(kill.victim.characterName, db.getTypeName(kill.victim.shipTypeID).sync(), db.getSystemName(kill.solarSystemID.toInt).sync()))

    if (targets.keySet contains kill.victim.shipTypeID.toInt) {
      // the person who died was in a super
      val character = kill.victim.characterName
      val lostshiptype = db.getTypeName(kill.victim.shipTypeID.toInt).sync()
      val location = db.getSystemName(kill.solarSystemID.toInt).sync()
      logger.info(s"$character lost a $lostshiptype in $location")
    }
    kill.attackers.foreach { attacker =>
      if(targets.keySet contains attacker.shipTypeID.toInt) {
        // attacker was in a super
        val character = attacker.characterName
        val usedship = db.getTypeName(attacker.shipTypeID.toInt).sync()
        val target = db.getTypeName(kill.victim.shipTypeID.toInt).sync()
        val location = db.getSystemName(kill.solarSystemID.toInt).sync()
        logger.info(s"$character used a $usedship to kill a $target in $location")
      }
    }
  })

  Thread.sleep(60.seconds.toMillis)

}

object SuperDetector extends App {
  val db = Database.forURL("jdbc:mysql://localhost:3306/sde", "sde", "sde", driver = "com.mysql.jdbc.Driver")
  new SuperDetector("ws://ws.eve-kill.net/kills", new DatabaseOps(db))
}
package moe.pizza.analyser

import moe.pizza.analyser.WormholeResidency.ResidencyModifier
import moe.pizza.eveapi.EVEAPI
import moe.pizza.zkapi.{ZKBRequest, WebsocketFeed}
import moe.pizza.zkapi.zkillboard.Killmail
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import moe.pizza.sdeapi._

import scala.concurrent.duration._
import scala.collection.mutable

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Andi on 06/11/2015.
 */

object WormholeResidency {
  case class ResidencyModifier(corporationID: Long, modifier: Double)
   def calculateResidencyModifiers(kill: Killmail, value: Int): Seq[ResidencyModifier] = {
    val loss = ResidencyModifier(kill.victim.corporationID, 0-value)
    val corpscores = kill.attackers.groupBy(_.corporationID).mapValues(_.size)
    val totalcorpscores = corpscores.values.sum
    val kills = corpscores.map { kv =>
      val (corp, score) = kv
      ResidencyModifier(corp, 100.toDouble*(score.toDouble/totalcorpscores.toDouble))
    }
    Seq(loss) ++ kills
  }
}

class WormholeResidency(db: DatabaseOps) {

  /*
  POS - 100 points
  POCO - 10 points
  Capital - 5 points
  exponential falloff from 100% at 30d to 0% at 90d
  Kill using pos - 2 points
  kill - 1 point
   */

  val logger = LoggerFactory.getLogger(classOf[SuperDetector])

  // Towers
  val TOWER_GROUP = 478
  val towers = db.getTypesbyMarketGroup(TOWER_GROUP)
  val towerids = towers.sync().map(_.typeid).toSet

  // Pocos
  val POCO_GROUP = 1410
  val pocos = db.getTypesbyMarketGroup(POCO_GROUP)
  val pocoids = pocos.sync().map(_.typeid).toSet



  def analyse(kill: Killmail): Seq[ResidencyModifier] = {
    kill match {
      case k if towerids contains k.victim.shipTypeID.toInt =>
        WormholeResidency.calculateResidencyModifiers(k, 100)
      case k if pocoids contains k.victim.shipTypeID.toInt =>
        WormholeResidency.calculateResidencyModifiers(k, 10)
      case _ => Nil
    }
  }

  def useWebSockets(): Unit = {
    //WebsocketFeed.createClient("ws://ws.eve-kill.net/kills", analyse)
  }

  def backfill(systemID: Long) = {
    val killmails = fetchKms(systemID)
    println("gathered %d killmails".format(killmails.size))
    val scores = killmails.flatMap(analyse).groupBy(_.corporationID).mapValues(_.map(_.modifier).sum).toList.sortBy(0-_._2)
    val affiliation = new EVEAPI().eve.CharacterAffiliation(scores.map(_._1.toString)).sync()
    val namelookup = affiliation.get.result.map(r => (r.characterID.toLong, r.characterName)).toMap
    scores.foreach { score =>
      println("%s scored %f".format(namelookup.getOrElse(score._1, "Unknown"), score._2))
    }
  }

  def fetchKms(systemID: Long, page: Int = 1): List[Killmail] = {
    import scala.concurrent.ExecutionContext.Implicits.global

     val req = new ZKBRequest(useragent = "pizza-wormhole-intel")
      .solarSystemID(systemID)
      .start(DateTime.now().minusDays(90))
      .end(DateTime.now())
    val res = req.page(page).build(global).sync().get
    if (res.size==200) {
      res ++ fetchKms(systemID, page+1)
    } else {
      res
    }
  }


}

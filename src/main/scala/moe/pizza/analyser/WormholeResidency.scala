package moe.pizza.analyser

import moe.pizza.zkapi.{ZKBRequest, WebsocketFeed}
import moe.pizza.zkapi.zkillboard.Killmail
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import moe.pizza.sdeapi._

import scala.concurrent.duration._
import scala.collection.mutable


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

class WormholeResidency(url: String, db: DatabaseOps) {

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



  def analyse(kill: Killmail) {
    kill match {
      case k if towerids contains k.victim.shipTypeID.toInt =>
        WormholeResidency.calculateResidencyModifiers(k, 100)
      case k if pocoids contains k.victim.shipTypeID.toInt =>
        WormholeResidency.calculateResidencyModifiers(k, 10)

    }
  }

  def useWebSockets(): Unit = {
    WebsocketFeed.createClient("ws://ws.eve-kill.net/kills", analyse)
  }

  def backFill(systemID: Long): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    new ZKBRequest(useragent = "pizza-wormhole-intel")
      .solarSystemID(systemID)
      .start(DateTime.now().minusDays(90))
      .end(DateTime.now())
      .build(global)
  }


}

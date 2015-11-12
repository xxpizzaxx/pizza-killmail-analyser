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
class WormholeResidency(url: String, db: DatabaseOps) {

  /*
  POS - 100 points
  POCO - 10 points
  Capital - 5 points
  exponential falloff from 100% at 30d to 0% at 90d
  Kill using pos - 2 points
  kill - 1 point
   */
  case class ResidencyModifier(corporationID: Long, modifier: Int)

  val logger = LoggerFactory.getLogger(classOf[SuperDetector])

  val TOWER_GROUP = 478

  val RESIDENCYOPS = new mutable.Queue[ResidencyModifier]

  val towers = db.getTypesbyMarketGroup(TOWER_GROUP)
  val towerids = towers.sync().map(_.typeid).toSet

  def analyse(kill: Killmail) {
    // if it's a pos that was killed
    if (towerids contains kill.victim.shipTypeID.toInt) {
      RESIDENCYOPS.enqueue(new ResidencyModifier(kill.victim.corporationID, -100))
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

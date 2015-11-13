package moe.pizza.analyser

import moe.pizza.analyser.WormholeResidency.ResidencyModifier
import moe.pizza.eveapi.EVEAPI
import moe.pizza.zkapi.{ZKBRequest, WebsocketFeed}
import moe.pizza.zkapi.zkillboard.Killmail
import org.joda.time.{Days, DateTime}
import org.joda.time.format.DateTimeFormatterBuilder
import org.slf4j.LoggerFactory
import moe.pizza.sdeapi._

import scala.concurrent.duration._
import scala.collection.mutable

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Andi on 06/11/2015.
 */

object WormholeResidency {
  case class ResidencyModifier(corporationID: Long, modifier: Double, relevancy: Double)
  val datetimeformat = new DateTimeFormatterBuilder()
    .appendYear(4,4).appendLiteral("-").appendMonthOfYear(2).appendLiteral("-").appendDayOfMonth(2).appendLiteral(" ")
    .appendHourOfDay(2).appendLiteral(":").appendMinuteOfHour(2).appendLiteral(":").appendSecondOfMinute(2).toFormatter

  def calculateRelevancy(kill: Killmail): Double = {
    val dt = datetimeformat.parseDateTime(kill.killTime)
    val days = Days.daysBetween(dt, DateTime.now()).getDays
    val normaliseddays = if (days < 1) 1 else days
    1.0/normaliseddays.toFloat
  }
  def calculateResidencyModifiers(kill: Killmail, value: Int): Seq[ResidencyModifier] = {
    val relevancy = calculateRelevancy(kill)
    val loss = ResidencyModifier(kill.victim.corporationID, 0-value, relevancy)
    val corpscores = kill.attackers.groupBy(_.corporationID).mapValues(_.size)
    val totalcorpscores = corpscores.values.sum
    val kills = corpscores.map { kv =>
      val (corp, score) = kv
      ResidencyModifier(corp, 100.toDouble*(score.toDouble/totalcorpscores.toDouble), relevancy)
    }
    Seq(loss) ++ kills
  }
}

class WormholeResidency(db: DatabaseOps) {

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
      case k =>
        WormholeResidency.calculateResidencyModifiers(k, 1)
    }
  }

  def useWebSockets(): Unit = {
    //WebsocketFeed.createClient("ws://ws.eve-kill.net/kills", analyse)
  }

  def backfill(systemID: Long) = {
    val killmails = fetchKms(systemID)
    println("gathered %d killmails".format(killmails.size))
    val scores = killmails.flatMap(analyse).groupBy(_.corporationID).mapValues(_.map(rm => rm.modifier * rm.relevancy).sum).toList.sortBy(0-_._2)
    val affiliation = new EVEAPI().eve.CharacterAffiliation(scores.map(_._1.toString)).sync()
    if (scores.nonEmpty) {
      val namelookup = affiliation.get.result.map(r => (r.characterID.toLong, r.characterName)).toMap
      scores.foreach { score =>
        println("%s scored %f".format(namelookup.getOrElse(score._1, "Unknown"), score._2))
      }
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

package moe.pizza.analyser

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import moe.pizza.analyser.WormholeResidency.ResidencyModifier
import moe.pizza.eveapi.EVEAPI
import moe.pizza.zkapi.{ZKBRequest, WebsocketFeed}
import moe.pizza.zkapi.zkillboard.Killmail
import org.joda.time.{Days, DateTime}
import org.joda.time.format.DateTimeFormatterBuilder
import org.slf4j.LoggerFactory
import moe.pizza.sdeapi._
import moe.pizza.analyser.WormholeResidency._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.collection.mutable


/**
 * Created by Andi on 06/11/2015.
 */

object WormholeResidency {
  val requests = new AtomicInteger(0)

  case class ResidencyModifier(corporationID: Long, modifier: Double, relevancy: Double)

  case class ResidencyResult(corporationID: Long, corporationName: String, residencyScore: Double)

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
      ResidencyModifier(corp, value.toDouble*(score.toDouble/totalcorpscores.toDouble), relevancy)
    }
    Seq(loss) ++ kills
  }
}

class WormholeResidency(db: DatabaseOps) {

  val executorService = Executors.newFixedThreadPool(1000)
  implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

  val logger = LoggerFactory.getLogger(classOf[WormholeResidency])

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
      //val namelookup = affiliation.get.result.map(r => (r.characterID.toLong, r.characterName)).toMap
      scores.foreach { score =>
        //println("%s scored %f".format(namelookup.getOrElse(score._1, "Unknown"), score._2))
      }
    }
  }

  def analyseAndGenerateResults(systemID: Long): Seq[ResidencyResult] = {
    val killmails = fetchKms(systemID)
    val scores = killmails.flatMap(analyse).groupBy(_.corporationID).mapValues(_.map(rm => rm.modifier * rm.relevancy).sum).toList.sortBy(0-_._2)
    val affiliation = new EVEAPI().eve.CharacterAffiliation(scores.map(_._1.toString)).sync()
    if (scores.nonEmpty) {
      val namelookup = affiliation.get.result.map(r => (r.characterID.toLong, r.characterName)).toMap
      val res = scores.map {
        score => ResidencyResult(score._1, namelookup.getOrElse(score._1, "Unknown"), score._2)
      }
      res
    }
    else {
      Nil
    }

  }

  def fetchKms(systemID: Long, page: Int = 1): List[Killmail] = {
    import scala.concurrent.ExecutionContext.Implicits.global

     val req = new ZKBRequest(useragent = "pizza-wormhole-intel")
      .solarSystemID(systemID)
      .start(DateTime.now().minusDays(90))
      .end(DateTime.now())
    val res = req.page(page).build().sync(60 seconds).get
    WormholeResidency.requests.incrementAndGet()
    if (res.size==200) {
      res ++ fetchKms(systemID, page+1)
    } else {
      res
    }
  }


}

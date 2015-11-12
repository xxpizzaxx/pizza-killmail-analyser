package moe.pizza.analyser

import moe.pizza.sdeapi._
import slick.driver.MySQLDriver.api._
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcBackend.Database
import spray.caching.{Cache, LruCache}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * Created by Andi on 05/11/2015.
 */
class DatabaseOps(db: JdbcBackend.Database) {


  val typeidcache: Cache[Int] = LruCache()
  def getTypeID(name: String): Future[Int] = typeidcache(name) {
    val r = Tables.Invtypes.filter(_.typename === name).result.run(db)
    val r2 = r.map(_.headOption.map(_.typeid))
    r2.map(_.get)
  }

  val invtypescache: Cache[Tables.Invtypes#TableElementType] = LruCache()
  def getType(id: Int): Future[Tables.Invtypes#TableElementType] = invtypescache(id) {
    val r = Tables.Invtypes.filter(_.typeid === id).result.run(db)
    val r2 = r.map(_.head)
    r2
  }

  val invtypesbymarketgroupcache: Cache[Seq[Tables.Invtypes#TableElementType]] = LruCache()
  def getTypesbyMarketGroup(id: Int): Future[Seq[Tables.Invtypes#TableElementType]] = invtypesbymarketgroupcache(id) {
    Tables.Invtypes.filter(_.marketgroupid === id).result.run(db)
  }

  val typenamecache: Cache[String] = LruCache()
  def getTypeName(id: Long): Future[String] = typenamecache(id) {
    val query = sql"select typeName from invTypes where typeID = $id".as[(String)]
    val res = db.run(query)
    res.map(_.head)
  }

  val systemid2namecache: Cache[String] = LruCache()
  def getSystemName(id: Int): Future[String] = systemid2namecache(id) {
    val query = sql"select solarSystemName from mapSolarSystems where solarsystemid = $id".as[(String)]
    val res = db.run(query)
    res.map(_.head)
  }

}

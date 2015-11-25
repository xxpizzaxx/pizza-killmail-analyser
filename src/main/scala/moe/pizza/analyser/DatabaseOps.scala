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


  val typeidcache: Cache[Option[Int]] = LruCache()
  def getTypeID(name: String): Future[Option[Int]] = typeidcache(name) {
    val r = Tables.Invtypes.filter(_.typename === name).result.run(db)
    r.map(_.headOption.map(_.typeid))
  }

  val invtypescache: Cache[Option[Tables.Invtypes#TableElementType]] = LruCache()
  def getType(id: Int): Future[Option[Tables.Invtypes#TableElementType]] = invtypescache(id) {
    val r = Tables.Invtypes.filter(_.typeid === id).result.run(db)
    r.map(_.headOption)
  }

  val invtypesbymarketgroupcache: Cache[Seq[Tables.Invtypes#TableElementType]] = LruCache()
  def getTypesbyMarketGroup(id: Int): Future[Seq[Tables.Invtypes#TableElementType]] = invtypesbymarketgroupcache(id) {
    Tables.Invtypes.filter(_.marketgroupid === id.toLong).result.run(db)
  }

  val typenamecache: Cache[Option[String]] = LruCache()
  def getTypeName(id: Long): Future[Option[String]] = typenamecache(id) {
    val query = sql"select typeName from invTypes where typeID = $id".as[(String)]
    val res = db.run(query)
    res.map(_.headOption)
  }

  val systemid2namecache: Cache[Option[String]] = LruCache()
  def getSystemName(id: Int): Future[Option[String]] = systemid2namecache(id) {
    val query = sql"select solarSystemName from mapSolarSystems where solarsystemid = $id".as[(String)]
    val res = db.run(query)
    res.map(_.headOption)
  }

  val systemname2idcache: Cache[Option[Int]] = LruCache()
  def getSystemID(name: String): Future[Option[Int]] = systemname2idcache(name) {
    val query = sql"select solarSystemID from mapSolarSystems where solarSystemName = $name".as[(Int)]
    val res = db.run(query)
    res.map(_.headOption)
  }

}

package moe.pizza.analyser

import slick.jdbc.JdbcBackend._

/**
 * Created by Andi on 12/11/2015.
 */
object WormholeResidencyTester extends App {

  val wr = new WormholeResidency(new DatabaseOps(Database.forURL("jdbc:mysql://localhost:3306/sde", "sde", "sde", driver = "com.mysql.jdbc.Driver")))
  wr.backfill(31002487)

}

package fr.acinq.alarmbot

import com.typesafe.config.{ConfigException, ConfigFactory, Config => TypesafeConfig}
import net.ceedubs.ficus.Ficus._

import java.io.File
import scala.jdk.CollectionConverters.MapHasAsScala
import java.lang.Float

class AlarmBotConfig(datadir: File) {
  val resourcesDir: File = new File(datadir, "/plugin-resources/alarmbot/")

  val config: TypesafeConfig = ConfigFactory parseFile new File(resourcesDir, "alarmbot.conf")

  val botApiKey: String = config.as[String]("config.botApiKey")

  val chatId: String = config.as[String]("config.chatId")

  val hedgeServices: TypesafeConfig = config.getObject("config.hedgeServices").toConfig
  val hedgeNotify: Boolean = config.as[Boolean]("config.hedgeNotify")
  val hedgeServicesMap: scala.collection.mutable.Map[String,String]  = collection.mutable.Map()

  config.getObject("config.hedgeServices").asScala.foreach({ case (k, _) => hedgeServicesMap += (k -> hedgeServices.getString(k)) })
}

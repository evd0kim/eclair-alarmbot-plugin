package fr.acinq.alarmbot

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import net.ceedubs.ficus.Ficus._

import java.io.File

class AlarmBotConfig(datadir: File) {
  val resourcesDir: File = new File(datadir, "/plugin-resources/alarmbot/")

  val config: TypesafeConfig = ConfigFactory parseFile new File(resourcesDir, "alarmbot.conf")

  val botApiKey: String = config.as[String]("config.botApiKey")

  val chatId: String = config.as[String]("config.chatId")
  val hedgeService: String = config.as[String]("config.hedgeServiceUri")
}

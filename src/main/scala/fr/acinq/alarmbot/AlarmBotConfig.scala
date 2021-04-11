package fr.acinq.alarmbot

import net.ceedubs.ficus.Ficus._
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import java.io.File

object AlarmBotConfig {
  val resourcesDir: String = s"${System getProperty "user.dir"}/plugin-resources/alarmbot"

  val config: TypesafeConfig = ConfigFactory parseFile new File(resourcesDir, "alarmbot.conf")

  val bot: String = config.as[String]("config.botApiKey")

  val chat: String = config.as[String]("config.chatId")
}

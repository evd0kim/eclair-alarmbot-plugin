package fr.acinq.alarmbot

import fr.acinq.eclair.wire.Color
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import java.io.File

object Config {
  implicit val colorReader: ValueReader[Color] = ValueReader.relative { source =>
    Color(source.getInt("r").toByte, source.getInt("g").toByte, source.getInt("b").toByte)
  }

  val resourcesDir: String = s"${System getProperty "user.dir"}/plugin-resources/alarmbot"

  val config: TypesafeConfig = ConfigFactory parseFile new File(resourcesDir, "alarmbot.conf")

  val bot: String = config.as[String]("config.botApiKey")
  val chat: String = config.as[String]("config.chatId")
}

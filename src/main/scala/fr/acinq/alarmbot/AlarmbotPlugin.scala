package fr.acinq.alarmbot

import akka.actor.Props
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import fr.acinq.eclair.{Kit, Plugin, PluginParams, Setup}
import grizzled.slf4j.Logging
import net.ceedubs.ficus.Ficus._

import java.io.File


class AlarmbotPlugin extends Plugin with Logging {

  private var setupRef: Setup = _

  override def onSetup(setup: Setup): Unit = setupRef = setup

  override def onKit(kit: Kit): Unit = {
    val resourcesDir: File = new File(setupRef.datadir, "/plugin-resources/alarmbot/")
    val config: TypesafeConfig = ConfigFactory parseFile new File(resourcesDir, "alarmbot.conf")

    val botApiKey: String = config.as[String]("config.botApiKey")
    val chatId: String = config.as[String]("config.chatId")

    kit.system actorOf Props(classOf[WatchdogSync], setupRef, botApiKey, chatId)
  }

  override def params: PluginParams = new PluginParams {
    override def name: String = "AlarmBot"
  }
}
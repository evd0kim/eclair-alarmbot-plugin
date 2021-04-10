package fr.acinq.alarmbot

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.Config
import fr.acinq.eclair.{Kit, Plugin, PluginParams, Setup}
import grizzled.slf4j.Logging

import scala.concurrent.ExecutionContextExecutor

class AlarmbotPlugin extends Plugin with Logging {

  var conf: Config = null
  var kit: Kit = null

  override def onSetup(setup: Setup): Unit = {
    logger.info(s"plugin for sending messages to telegram bot")
  }

  override def onKit(kit: Kit): Unit = {
    val syncRef = kit.system actorOf Props(classOf[WatchdogSync], kit)

    implicit val executionContext: ExecutionContextExecutor = kit.system.dispatcher
    implicit val coreActorSystem: ActorSystem = kit.system
  }


  override def params: PluginParams = new PluginParams {
    override def name: String = "AlarmBot"
  }
}
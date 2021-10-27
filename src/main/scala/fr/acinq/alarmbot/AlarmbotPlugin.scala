package fr.acinq.alarmbot

import fr.acinq.eclair.{Kit, Plugin, PluginParams, Setup}
import grizzled.slf4j.Logging
import akka.actor.Props


class AlarmbotPlugin extends Plugin with Logging {
  var pluginConfig: AlarmBotConfig = _
  private var setupRef: Setup = _

  override def onSetup(setup: Setup): Unit = {
    pluginConfig = new AlarmBotConfig(datadir = setup.datadir)
    setupRef = setup
  }

  override def onKit(kit: Kit): Unit = kit.system actorOf Props(classOf[WatchdogSync], kit, setupRef, pluginConfig)

  override def params: PluginParams = new PluginParams {
    override def name: String = "AlarmBot"
  }
}
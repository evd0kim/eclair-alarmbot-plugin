package fr.acinq.alarmbot

import fr.acinq.eclair.{Kit, Plugin, PluginParams, Setup}
import grizzled.slf4j.Logging
import akka.actor.Props


class AlarmbotPlugin extends Plugin with Logging {

  private var setupRef: Setup = _

  override def onSetup(setup: Setup): Unit = setupRef = setup

  override def onKit(kit: Kit): Unit = kit.system actorOf Props(classOf[WatchdogSync], kit, setupRef)

  override def params: PluginParams = new PluginParams {
    override def name: String = "AlarmBot"
  }
}
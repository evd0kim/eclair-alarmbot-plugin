package fr.acinq.alarmbot

import fr.acinq.eclair.MilliSatoshi

/**
 * Should be extended by other plugins to get their messages sent to TG through this plugin
 * (1) Include Alarmbot plugin (this one) in your own plugin dependencies
 * (2) Extend this trait, implement methods and broadcast when needed
 * (3) Alarmbot will catch your custom message and send it to TG
 */
trait ExternalHedgeMessage {
  def senderEntity: String
  def amount: MilliSatoshi
  def rate: MilliSatoshi
}

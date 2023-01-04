package fr.acinq.alarmbot

import akka.event.DiagnosticLoggingAdapter
import fr.acinq.eclair.randomBytes
import fr.acinq.eclair.tor.Socks5ProxyParams
import sttp.client3.okhttp.OkHttpFutureBackend
import sttp.client3.{SttpBackend, SttpBackendOptions}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object SttpUtil {
  def createSttpBackend(socksProxy_opt: Option[Socks5ProxyParams], useProxy: Boolean, log: DiagnosticLoggingAdapter): SttpBackend[Future, _] = {
    val options = SttpBackendOptions(connectionTimeout = 30.seconds, proxy = None)
    val sttpBackendOptions: SttpBackendOptions = socksProxy_opt match {
      case Some(proxy) if useProxy =>
        val proxyOptions = options.connectionTimeout(120.seconds)
        val host = proxy.address.getHostString
        val port = proxy.address.getPort
        log.info(s"PLGN AlarmBot, connecting via Socks5 proxy $host:$port")
        if (proxy.randomizeCredentials)
          proxyOptions.socksProxy(host, port, username = randomBytes(16).toHex, password = randomBytes(16).toHex)
        else
          proxyOptions.socksProxy(host, port)
      case _ => options
    }
    OkHttpFutureBackend(sttpBackendOptions)
  }
}

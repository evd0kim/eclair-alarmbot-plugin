package fr.acinq.alarmbot

import com.softwaremill.sttp._
import com.softwaremill.sttp.json4s._
import fr.acinq.eclair.MilliSatoshi

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class KolliderClient(pluginConfig: AlarmBotConfig) {
  val botApiKey = pluginConfig.botApiKey
  val chatId = pluginConfig.chatId
  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  val serviceUri: Uri = uri"http://192.168.2.10:8081"

  val readTimeout: FiniteDuration = 10.seconds

  def checkAvailability()(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri = serviceUri.path("/state")
    sttp.readTimeout(readTimeout).get(parametrizedUri).send.map(identity)
  }

  //case class HedgeRequest(sats: Long, rate: Long)
  //HedgeRequest(amount.toLong, rate.toLong)
  case class HedgeResponse()

  def addPosition(amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[HedgeResponse]] = {
    val htlcApiUri = serviceUri.path("/hedge/htlc")
    implicit val serialization = org.json4s.native.Serialization
    implicit val formats = org.json4s.DefaultFormats
    sttp.readTimeout(readTimeout)
      .contentType("application/json")
      .post(htlcApiUri)
      .body(Map("sats"->amount, "rate"->rate))
      .response(asJson[HedgeResponse])
      .send()
      .map(identity)
  }

  def sendMessage(amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> s"hedging ${amount} ${rate}", "parse_mode" -> "HTML")
    sttp.readTimeout(readTimeout).get(parametrizedUri).send.map(identity)
  }
}
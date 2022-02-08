package fr.acinq.alarmbot

import com.softwaremill.sttp._
import com.softwaremill.sttp.json4s.asJson
import fr.acinq.eclair.MilliSatoshi
import fr.acinq.eclair.api.serde.JsonSupport.serialization

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

  case class HedgeRequest(sats: Long, rate: Long)

  implicit val hedgeRequestSerializer: BodySerializer[HedgeRequest] = { r: HedgeRequest =>
    val serialized = s"${r.sats},${r.rate}"
    StringBody(serialized, "UTF-8")
  }
  //HedgeRequest(amount.toLong, rate.toLong)
  //Map("sats"->amount, "rate"->rate)
  def addPosition(amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[String]] = {
    val htlcApiUri = serviceUri.path("/hedge/htlc")
    sttp.readTimeout(readTimeout).contentType("application/json").post(htlcApiUri).body(HedgeRequest(amount.toLong, rate.toLong)).send.map(identity)
  }

  def sendMessage(amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> s"hedging ${amount} ${rate}", "parse_mode" -> "HTML")
    sttp.readTimeout(readTimeout).get(parametrizedUri).send.map(identity)
  }
}
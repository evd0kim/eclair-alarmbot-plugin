package fr.acinq.alarmbot

import com.softwaremill.sttp._
import com.softwaremill.sttp.json4s._
import fr.acinq.bitcoin.Satoshi
import fr.acinq.eclair.MilliSatoshi

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

//import org.json4s._
//import org.json4s.jackson.JsonMethods._
//import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.write

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

  case class HedgeResponse()
  case class HedgeRequest(channel_id: String, sats: Long, rate: Long)

  def addPosition(amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[HedgeResponse]] = {
    implicit val serialization = org.json4s.native.Serialization
    implicit val formats = org.json4s.DefaultFormats

    val htlcApiUri = serviceUri.path("/hedge/htlc")

    val hedgeRequest = HedgeRequest(
      "test",
      amount.truncateToSatoshi.toLong,
      rate.truncateToSatoshi.toLong)

    println(write(hedgeRequest))

    sttp.readTimeout(readTimeout)
      .contentType("application/json")
      .post(htlcApiUri)
      .body(write(hedgeRequest))
      .response(asJson[HedgeResponse])
      .send()
      .map(identity)
  }

  def sendMessage(amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> s"hedging ${amount} ${rate}", "parse_mode" -> "HTML")
    sttp.readTimeout(readTimeout).get(parametrizedUri).send.map(identity)
  }
}
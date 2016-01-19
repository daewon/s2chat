package actors

import actors.UserSocket.ClientMessage.messageReads
import actors.UserSocket.{ClientMessage, ServerMessage}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.event.LoggingReceive
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import services.Graph

import scala.xml.Utility

object UserSocket {
  def props(topic: String, s2: Graph)(out: ActorRef) = Props(classOf[UserSocket], topic, s2, out)

  case class ClientMessage(user: String, roomId: String, text: String) {
    def escape = copy(text = Utility.escape(text))
  }

  case class ServerMessage(user: String, roomId: String, text: String) {
    def escape = copy(text = Utility.escape(text))
  }

  object ClientMessage {
    implicit val messageReads = Json.reads[ClientMessage]
  }

  object ServerMessage {
    implicit val chatMessageWrites = new Writes[ServerMessage] {
      def writes(message: ServerMessage): JsValue = Json.obj(
        "user" -> message.user,
        "roomId" -> message.roomId,
        "text" -> message.text
      )
    }
  }

}

class UserSocket(userId: String, s2: Graph, out: ActorRef) extends Actor with ActorLogging {
  val mediator = DistributedPubSub(context.system).mediator
  val rooms = s2.rooms(userId).toSeq.flatten
  var roomCount = rooms.length - 1
  def isReady = roomCount == 0

  rooms.foreach { room => mediator ! Subscribe(room, self) }

  def receive = LoggingReceive {
    case a@SubscribeAck(Subscribe(_, None, `self`)) =>
      Logger.error(a.toString)

      if (isReady) {
        context become ready
        out ! Json.toJson("ready")
      } else roomCount -= 1
  }

  def ready = LoggingReceive {
    case js: JsValue =>
      val msg = js.validate[ClientMessage](messageReads)
      if (msg.isError) Logger.error(s"Parsing JS error: ${js.toString}")

      msg.map(_.escape).foreach { msg =>
        mediator ! Publish(msg.roomId, ServerMessage(msg.user, msg.roomId, msg.text))
      }

    case c: ServerMessage => out ! Json.toJson(c)
  }
}

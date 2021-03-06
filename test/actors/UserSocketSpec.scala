package actors

import actors.UserSocket.{ServerMessage$, ClientMessage$}
import akka.actor._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.testkit.TestProbe
import org.specs2.mutable._
import play.api.libs.json._

import scala.language.postfixOps

class UserSocketSpec extends Specification {
  val UserId = "user1"

  "A user socket" should {
    val topic = "chat"

    "send chat message to all subscribers" in new AkkaTestkitSpecs2Support {
      implicit val messageWrites = Json.writes[ClientMessage]

      val mediator = DistributedPubSub(system).mediator
      val browser = TestProbe()
      val chatMember1 = TestProbe()
      val chatMember2 = TestProbe()
      mediator ! Subscribe(topic, chatMember1.ref)
      mediator ! Subscribe(topic, chatMember2.ref)
      val socket = system.actorOf(UserSocket.props("user1")(browser.ref), "userSocket")
      val message = "hello"

      socket ! Json.toJson(ClientMessage(message))

      chatMember1.ignoreMsg({ case SubscribeAck => true })
      chatMember1.expectMsg(ServerMessage(UserId, message))
      chatMember2.ignoreMsg({ case SubscribeAck => true })
      chatMember2.expectMsg(ServerMessage(UserId, message))
    }

    "forward chat message to browser" in new AkkaTestkitSpecs2Support {
      val browser = TestProbe()
      val socket = system.actorOf(UserSocket.props(UserId)(browser.ref), "userSocket")
      val chatMessage = ServerMessage(UserId, "There is important thing to do!")

      socket ! chatMessage

      browser.expectMsg(Json.toJson(chatMessage))
    }
  }
}

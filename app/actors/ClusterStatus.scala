package actors

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.event.LoggingReceive

class ClusterStatus extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  var leader: Option[Address] = None

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember], classOf[LeaderChanged])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = LoggingReceive {
    case MemberUp(member) =>
      log.info(s"Member is Up: ${member.address}")

      leader.filter(_ == member.address) foreach { address =>
        log.info(s"Leader is now up: $address")
      }

    case MemberRemoved(member, previousStatus) =>
      log.info(s"Member is Removed: ${member.address} after $previousStatus")

    case LeaderChanged(address) =>
      log.info(s"Leader changed: $address")
      leader = address

    case UnreachableMember(member) =>
      log.info(s"Member detected as unreachable: $member")

    case any: MemberEvent => // ignore
      log.info(s"Ignore unknown event: $any")
  }
}

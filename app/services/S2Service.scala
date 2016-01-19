package services

import javax.inject._

object Model {
  type Members = Seq[String]
  type Rooms = Seq[String]
}

// mock
@Singleton
class Graph() {

  import Model._

  val mockRooms = Map(
    "daewon" -> Seq("1", "2"),
    "ws" -> Seq("1")
  )

  val mockMembers = Map(
    "1" -> Seq("daewon", "ws", "ok", "hi"),
    "2" -> Seq("ws", "ok", "hi")
  )

  def rooms(uid: String): Option[Rooms] = mockRooms.get(uid)

  def members(roomNumber: String): Option[Members] = mockMembers.get(roomNumber)
}


package controllers

import javax.inject._

import actors.{ClusterStatus, UserSocket}
import akka.actor._
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Action, Controller, WebSocket}
import services.Graph

import scala.concurrent.Future


@Singleton
class Application @Inject()(val messagesApi: MessagesApi, system: ActorSystem, s2: Graph) extends Controller with I18nSupport {
  val SessionName = "login-session"
  val clusterStatus = system.actorOf(Props[ClusterStatus], "clusterStatus")

  val nameForm = Form(single("name" -> nonEmptyText))

  def index = Action { implicit request =>
    request.session.get(SessionName).map { user =>
      Redirect(routes.Application.chat()).flashing("success" -> s"hello $user :)")
    }.getOrElse(Ok(views.html.index(nameForm)).flashing(request.flash))
  }

  def signin = Action { implicit request =>
    nameForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.index(formWithErrors)),
      name => Redirect(routes.Application.chat()).withSession(request.session + (SessionName -> name))
    )
  }

  def signout = Action { implicit request =>
    Redirect(routes.Application.index()).withNewSession.flashing("success" -> "Exit")
  }

  def chat = Action { implicit request =>
    val session = request.session.get(SessionName)
    val userRoomOpt = for {
      user <- session
      rooms <- s2.rooms(user)
    } yield Ok(views.html.chat(user, rooms))
    Json.toJson("ok")

    userRoomOpt.getOrElse(Redirect(routes.Application.index()).withNewSession.flashing("error" -> "Unknown user"))
  }

  def connect = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
      val userOpt = request.queryString.get("user").headOption.flatMap(_.headOption)
      val opt = userOpt.orElse(request.session.get(SessionName))

      Future.successful(opt match {
        case Some(uid) => Right(UserSocket.props(uid, s2))
        case None => Left(Forbidden)
      })
    }
}



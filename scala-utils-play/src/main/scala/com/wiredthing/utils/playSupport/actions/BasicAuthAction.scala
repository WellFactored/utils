package com.wiredthing.utils.playSupport.actions

import com.wiredthing.utils.NonBlankString
import com.wiredthing.utils.NonBlankString._
import com.wiredthing.utils.base64.Base64._
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

case class BasicAuth(username: NonBlankString, password: Option[NonBlankString]) {
  def encodeBase64: String = {
    val s = password match {
      case Some(pwd) => s"$username:$pwd"
      case None => s"$username:"
    }

    s.getBytes.toBase64
  }

  def headerValue: String = s"Basic $encodeBase64"
}

class BasicAuthRequest[A](val auth: BasicAuth, request: Request[A]) extends WrappedRequest[A](request)

object BasicAuthAction
  extends ActionBuilder[BasicAuthRequest]
  with ActionRefiner[Request, BasicAuthRequest]
  with BasicAuthExtraction {
  override protected def refine[A](request: Request[A]): Future[Either[Result, BasicAuthRequest[A]]] = Future.successful {
    extractBasicAuth(request) match {
      case \/-(auth) => Logger.debug(s"extracted basic auth $auth"); Right(new BasicAuthRequest(auth, request))
      case -\/(e) => Logger.debug(s"Failed basic auth: '$e'"); Left(Unauthorized)
    }
  }
}

trait BasicAuthExtraction {
  val Pattern = "Basic (.*)".r

  def extractBase64Auth(auth: String): \/[String, String] = auth match {
    case Pattern(s) => \/-(s)
    case _ => -\/(s"Auth string is malformed: '$auth'")
  }

  def decodeBase64(base64: String): \/[String, String] =
    base64.fromBase64.map(\/-(_)).getOrElse(-\/("Base64 string is malformed"))

  object Nbs {
    def unapply(s: String): Option[NonBlankString] = s.toNbs
  }

  def extractBasicAuth(s: String): \/[String, BasicAuth] = {
    // Conveniently, splitAt with a negative index will return a pair with the first element empty and
    // the second element holding the original string. E.g. "foo".splitAt(0) = ("", "foo"), so
    // this code will give a blank username error if the string does not contain a ':'
    val split: (String, String) = s.splitAt(s.indexOf(':'))
    split match {
      case (Nbs(u), "") => \/-(BasicAuth(u, None))
      case (Nbs(u), p) => \/-(BasicAuth(u, p.substring(1).toNbs))
      case _ => -\/("Username is blank")
    }
  }

  def decodeBasicAuth(auth: String): \/[String, BasicAuth] = for {
    b64 <- extractBase64Auth(auth)
    decoded <- decodeBase64(b64)
    auth <- extractBasicAuth(decoded)
  } yield auth

  def extractBasicAuth(request: Request[_]): \/[String, BasicAuth] =
    request.headers.get("Authorization")
      .map(decodeBasicAuth)
      .getOrElse(-\/("No Authorization header found in request"))
}
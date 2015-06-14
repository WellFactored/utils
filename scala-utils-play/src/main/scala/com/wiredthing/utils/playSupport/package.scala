package com.wiredthing.utils

import play.api.mvc._
import play.mvc.Http.HeaderNames

package object playSupport {

  implicit class RequestHeaderWrapper(val rh: RequestHeader) extends AnyVal {
    def withHeaders(headers: (String, String)*): RequestHeader = rh.copy(headers = rh.headers.add(headers: _*))

    def withCookie(cookie: Cookie): RequestHeader = {
      val existingCookies = rh.headers.get(play.api.http.HeaderNames.COOKIE).map(Cookies.decode).getOrElse(Seq())
      val newCookies = cookie +: existingCookies.filterNot(_.name == cookie.name)

      rh.withHeaders(HeaderNames.COOKIE -> Cookies.encode(newCookies))
    }

    def withSessionProperty(p: (String, String)): RequestHeader = withCookie(Session.encodeAsCookie(rh.session + p))
  }

}

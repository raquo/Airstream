package com.raquo.airstream

import org.scalajs.dom

package object web {

  /**
    * Constructs and returns an absolute websocket URL from a relative one.
    */
  def websocketPath(relative: String): String = {
    val prefix = dom.document.location.protocol match {
      case "https:" => "wss:"
      case _        => "ws:"
    }
    val suffix = if (relative.startsWith("/")) relative else s"/$relative"
    s"$prefix//${dom.document.location.hostname}:${dom.document.location.port}$suffix"
  }
}

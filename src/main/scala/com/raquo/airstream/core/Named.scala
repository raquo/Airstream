package com.raquo.airstream.core

import scala.scalajs.js

/** This trait lets the user set an ad-hoc name for this instance. Used for debugging and tracing.
  *
  * Subclasses: [[Observable]], [[Observer]]
  */
trait Named {

  /** This name should identify the instance (observable or observer) uniquely enough for your purposes.
    * You can read / write it to simplify debugging.
    * Airstream uses this in `debugLog*` methods. In the future, we will expand on this.
    * #TODO[Debug] We don't use this to its full potential yet.
    */
  protected[this] var maybeDebugName: js.UndefOr[String] = js.undefined

  /** This is the method that subclasses override to preserve the user's ability to set custom debug names. */
  protected def defaultDebugName: String = super.toString

  /** Override [[defaultDebugName]] instead of this, if you need to. */
  final override def toString: String = debugName

  final def debugName: String = maybeDebugName.getOrElse(defaultDebugName)

  /** Set the debug name for this instance (observable or observer).
    * - This method modifies the instance and returns `this`. It does not create a new instance.
    * - New name you set will override the previous name, if any.
    *   This might change in the future. For the sake of sanity, don't call this more than once for the same instance.
    * - If debug name is set, toString will output it instead of the standard type@hashcode string
    */
  def setDebugName(name: String): this.type = {
    maybeDebugName = name // @TODO[Warn] Maybe we should emit a warning if name was already set
    this
  }
}

package com.raquo.airstream.core

import scala.scalajs.js

/** This trait lets the user set an ad-hoc name for this instance. Used for debugging and tracing.
  *
  * Subclasses: [[BaseObservable]], [[Observer]]
  */
trait Named {

  /** This name should identify the instance (observable or observer) uniquely enough for your purposes.
    * You can read / write it to simplify debugging.
    * Airstream uses this in `debugLog*` methods. In the future, we will expand on this.
    * #TODO[Debug] We don't use this to its full potential yet.
    */
  protected[this] var maybeDisplayName: js.UndefOr[String] = js.undefined

  /** This is the method that subclasses override to preserve the user's ability to set custom display names. */
  protected def defaultDisplayName: String = super.toString

  /** Override [[defaultDisplayName]] instead of this, if you need to. */
  final override def toString: String = displayName

  final def displayName: String = maybeDisplayName.getOrElse(defaultDisplayName)

  /** Set the display name for this instance (observable or observer).
    * - This method modifies the instance and returns `this`. It does not create a new instance.
    * - New name you set will override the previous name, if any.
    *   This might change in the future. For the sake of sanity, don't call this more than once for the same instance.
    * - If display name is set, toString will output it instead of the standard type@hashcode string
    */
  def setDisplayName(name: String): this.type = {
    maybeDisplayName = name // @TODO[Warn] Maybe we should emit a warning if name was already set
    this
  }
}

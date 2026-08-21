package com.raquo.airstream.util

/** #NOTE CONTRIBUTORS: DO NOT OVER-USE FEATURE FLAGS!
  *
  * They should only be used for temporary migration and diagnostic helpers.
  *
  * For example, a boolean flag here could let users disable
  * new changes to Airstream behaviour, helping users verify
  * whether that change is causing the problems they might be
  * observing during a migration to a new version of Airstream.
  *
  * Third party libraries and any other non-end-user code:
  *  - must never change these values
  *  - must never require that users change these values from defaults
  *  - should NOT modify their own logic based on these flags,
  *    as that is likely to be counter-productive to the user's
  *    debugging efforts.
  */
object FeatureFlags {

  /** If true, EventBus will check .isStarted condition inside
    * the new transaction, not before creating said transaction.
    *
    * This fixes https://github.com/raquo/Airstream/issues/155
    *
    * #Warning: Should be enabled (true).
    *  - Do not disable except temporarily, to aid migration to V18.
    *  - Will be removed in a future version.
    */
  @deprecated("V18_EVENTBUS_ISSTARTED_FIX_155 disabled: reverted to pre-v18 behaviour. See https://github.com/raquo/Airstream/issues/155", since = "18.0.0-M6")
  var V18_EVENTBUS_ISSTARTED_FIX_155: Boolean = true
}

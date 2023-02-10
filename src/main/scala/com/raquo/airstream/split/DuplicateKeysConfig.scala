package com.raquo.airstream.split

/** .split() operator does not tolerate duplicate keys,
  * i.e. the Seq you provide it must not contain records
  * that have the same `key(record)`.
  *
  * However, checking for duplicate keys can get expensive when
  * splitting very large lists, so this setting can be used to
  * disable the checks.
  *
  * When warnings are enabled, YOUR CODE WILL STILL BREAK if the
  * .split() operator encounters duplicate keys, but it will
  * first print a warning in the browser console listing the
  * duplicate keys at fault.
  *
  * We enable this setting by default to aid in debugging. As the
  * end user, you might want to disable this either globally or
  * for specific .split() usages to improve performance on very
  * large lists.
  *
  * #TODO[Docs]: Add a short section to the docs about this.
  *
  * #TODO: Add more granular control later, if there is demand for that.
  *  For example, we could instruct Airstream to skip duplicate keys, or
  *  to raise an exception if a duplicate happens. In the latter case
  *  perhaps we could do that by catching the right exception, without
  *  the overhead of checking for duplicates. But not sure how bulletproof
  *  that logic would be.
  */
class DuplicateKeysConfig(private var _shouldWarn: Boolean) {

  def shouldWarn: Boolean = _shouldWarn

  /** Only ever call this on `DuplicateKeysConfig.default`, if you want to change the default. */
  private def setShouldWarn(newValue: Boolean): Unit = {
    _shouldWarn = newValue
  }
}

object DuplicateKeysConfig {

  /** Note: If you want to set a default, do it immediately at
    * application startup time to avoid the perils of global mutable vars.
    */
  def setDefault(newDefault: DuplicateKeysConfig): Unit = {
    // Do not reuse the mutable reference, copy its properties
    default.setShouldWarn(newDefault.shouldWarn)
  }

  val default: DuplicateKeysConfig = new DuplicateKeysConfig(_shouldWarn = true)

  val warnings: DuplicateKeysConfig = new DuplicateKeysConfig(_shouldWarn = true)

  val noWarnings: DuplicateKeysConfig = new DuplicateKeysConfig(_shouldWarn = false)
}

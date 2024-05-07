package com.raquo.airstream.status

/** Represents a combination of input event with the status
  * (availability) of output event(s) derived from it.
  * The output events typically come from a stream that is
  * derived from the input stream, but emitting asynchronously,
  * for example `inputStream.delay(1000)`.
  */
sealed trait Status[+In, +Out] {

  def isResolved: Boolean

  @inline def isPending: Boolean = !isResolved

  def mapInput[In2](project: In => In2): Status[In2, Out]

  def mapOutput[Out2](project: Out => Out2): Status[In, Out2]

  def fold[A](resolved: Resolved[In, Out] => A, pending: Pending[In] => A): A

  def toResolvedOption: Option[Resolved[In, Out]]

  def toPendingOption: Option[Pending[In]]

  def toResolvedInputOption: Option[In] = toResolvedOption.map(_.input)

  def toResolvedOutputOption: Option[Out] = toResolvedOption.map(_.output)

  def toPendingInputOption: Option[In] = toPendingOption.map(_.input)
}

/** Waiting for output for the latest input event. */
case class Pending[+In](input: In) extends Status[In, Nothing] {

  override def isResolved: Boolean = false

  override def mapInput[In2](project: In => In2): Status[In2, Nothing] = copy(input = project(input))

  override def mapOutput[Out2](project: Nothing => Out2): Pending[In] = this

  override def fold[A](
    resolved: Resolved[In, Nothing] => A,
    pending: Pending[In] => A
  ): A = {
    pending(this)
  }

  override def toResolvedOption: Option[Resolved[In, Nothing]] = None

  override def toPendingOption: Option[Pending[In]] = Some(this)
}

/** Output event received for this input, for the `ix`-th time (ix starts at 1). */
case class Resolved[+In, +Out](input: In, output: Out, ix: Int) extends Status[In, Out] {

  override def isResolved: Boolean = true

  override def mapInput[In2](project: In => In2): Status[In2, Out] = copy(input = project(input))

  override def mapOutput[Out2](project: Out => Out2): Status[In, Out2] = copy(output = project(output))

  override def fold[A](
    resolved: Resolved[In, Out] => A,
    pending: Pending[In] => A
  ): A = {
    resolved(this)
  }

  override def toResolvedOption: Option[Resolved[In, Out]] = Some(this)

  override def toPendingOption: Option[Pending[In]] = None
}

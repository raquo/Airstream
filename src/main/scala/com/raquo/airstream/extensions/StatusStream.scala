package com.raquo.airstream.extensions

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.status.{Pending, Resolved, Status}

/** See also [[StatusObservable]] for generic status operators */
class StatusStream[In, Out](
  private val stream: EventStream[Status[In, Out]]
) extends AnyVal {

  /** Emit `x` if parent stream emits `Resolved(_, x, _)`, do nothing otherwise */
  def collectOutput: EventStream[Out] = stream.collect {
    case Resolved(_, output, _) => output
  }

  /** Emit `pf(x)` if parent stream emits `Resolved(_, x, _)`, do nothing otherwise */
  def collectOutput[Out2](pf: PartialFunction[Out, Out2]): EventStream[Out2] = {
    stream.collectOpt {
      case Resolved(_, output, _) => pf.lift(output)
      case _ => None
    }
  }

  /** Emit `pf(Resolved(...))` if parent stream emits Resolved(...), do nothing otherwise */
  def collectResolved: EventStream[Resolved[In, Out]] = stream.collect {
    case r: Resolved[In @unchecked, Out @unchecked] => r
  }

  /** Emit `pf(Resolved(...))` if parent stream emits Resolved(...) and `pf` is defined for it, do nothing otherwise */
  def collectResolved[Out2](pf: PartialFunction[Resolved[In, Out], Out2]): EventStream[Out2] = {
    stream.collectOpt {
      case r: Resolved[In @unchecked, Out @unchecked] => pf.lift(r)
      case _ => None
    }
  }

  /** Emit `Pending(input)` if parent stream emits that, do nothing otherwise */
  def collectPending: EventStream[Pending[In]] = stream.collect {
    case p: Pending[In @unchecked] => p
  }

  /** Emit `input` if parent stream emits `Pending(input)`, do nothing otherwise */
  def collectPendingInput: EventStream[In] = stream.collect {
    case Pending(input) => input
  }

}

package com.raquo.airstream.core

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.{Calculation, Effect, TestableOwner}
import com.raquo.airstream.ownership.{DynamicOwner, DynamicSubscription, Owner, Subscription}
import com.raquo.airstream.state.Var

import scala.collection.mutable

class PullResetSignalSpec extends UnitSpec {

  it("signal.changes & startWith") {

    test(CACHE_INITIAL_VALUE = false/*, EMIT_CHANGE_ON_RESTART = true*/)
    //test(CACHE_INITIAL_VALUE = false, EMIT_CHANGE_ON_RESTART = false)
    //test(CACHE_INITIAL_VALUE = true, EMIT_CHANGE_ON_RESTART = true)
    test(CACHE_INITIAL_VALUE = true/*, EMIT_CHANGE_ON_RESTART = false*/)

    def test(CACHE_INITIAL_VALUE: Boolean/*, EMIT_CHANGE_ON_RESTART: Boolean*/): Unit = {

      withClue(s"Test with cacheInitialValue=$CACHE_INITIAL_VALUE" /* + s"emitChangeOnRestart=$EMIT_CHANGE_ON_RESTART"*/) {

        implicit val testOwner: TestableOwner = new TestableOwner

        val effects = mutable.Buffer[Effect[Int]]()
        val calculations = mutable.Buffer[Calculation[Int]]()

        var downInitial = 0

        val $var = Var(1)

        val upSignal = $var
          .signal
          .setDisplayName("varSignal")
          .map(identity)
          .setDisplayName("varSignal.map(identity)")
          .map(Calculation.log("up", calculations))
          .setDisplayName("varSignal.map(identity).map")

        val changesStream = upSignal.changes //(if (EMIT_CHANGE_ON_RESTART) upSignal.changesEmitChangeOnRestart else upSignal.changes)
          .setDisplayName("upSignal.changes")
          .map(Calculation.log("changes", calculations))
          .setDisplayName("upSignal.changes.map")

        val downSignal = changesStream
          .startWith(downInitial, cacheInitialValue = CACHE_INITIAL_VALUE)
          .setDisplayName("changes.startWith")
          .map(Calculation.log("down", calculations))
          .setDisplayName("downSignal")

        // #TODO[Scala3] Scala 3.0.2 requires the manual `Int` type ascription here, boo
        val upObs = Observer[Int](Effect.log("up-obs", effects))
        val changesObs = Observer[Int](Effect.log("changes-obs", effects))
        val downObs = Observer[Int](v => {
          Effect.log("down-obs", effects)(v)
        })

        // --

        upSignal.addObserver(upObs)

        val downSub1 = downSignal.addObserver(downObs)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 1),
          Calculation("down", 0)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 1),
          Effect("down-obs", 0),
        )
        calculations.clear()
        effects.clear()

        // --

        downSub1.kill()

        downInitial = 10

        val downSub2 = downSignal.addObserver(downObs)

        if (CACHE_INITIAL_VALUE) {
          calculations shouldBe mutable.Buffer()
          effects shouldBe mutable.Buffer(
            Effect("down-obs", 0),
          )
        } else {
          calculations shouldBe mutable.Buffer(
            Calculation("down", 10)
          )
          effects shouldBe mutable.Buffer(
            Effect("down-obs", 10),
          )
        }

        calculations.clear()
        effects.clear()

        // --

        $var.set(2)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 2),
          Calculation("changes", 2),
          Calculation("down", 2)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 2),
          Effect("down-obs", 2)
        )
        calculations.clear()
        effects.clear()

        // -- again, because there is no _.distinct now

        $var.set(2)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 2),
          Calculation("changes", 2),
          Calculation("down", 2)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 2),
          Effect("down-obs", 2)
        )
        calculations.clear()
        effects.clear()

        // --

        downSub2.kill()

        downInitial = 20

        val downSub3 = downSignal.addObserver(downObs)

        calculations shouldBe mutable.Buffer()
        effects shouldBe mutable.Buffer(
          Effect("down-obs", 2)
        )

        calculations.clear()
        effects.clear()

        // --

        downSub3.kill()

        $var.set(3)

        calculations shouldBe mutable.Buffer(
          Calculation("up", 3)
        )
        effects shouldBe mutable.Buffer(
          Effect("up-obs", 3)
        )
        calculations.clear()
        effects.clear()

        // --

        downSignal.addObserver(downObs)

        //if (EMIT_CHANGE_ON_RESTART) {
        //  // Emitting `2` is undesirable here, which is why `emitChangeOnRestart` is off by default.
        //  // It would be great if we could get only `3`, but it's not possible to fetch this without
        //  // processing the event through the streams, because it can have a) asynchronous and
        //  // b) filtering components to it, and there's no way for us to simulate that without actually
        //  // running that logic. We can only do this for signals because signals always have a current
        //  // value and are always synchronous.
        //
        //  calculations shouldBe mutable.Buffer(
        //    Calculation("down", 2),
        //    Calculation("changes", 3),
        //    Calculation("down", 3)
        //  )
        //  effects shouldBe mutable.Buffer(
        //    Effect("down-obs", 2),
        //    Effect("down-obs", 3)
        //  )
        //} else {

          calculations shouldBe mutable.Buffer(
            Calculation("changes", 3),
            Calculation("down", 3)
          )
          // The signal re-starts with an old value because it can't pull a fresh value from the streams,
          // but then the .changes stream pulls the new value from its parent, and propagates it down,
          // but that only happens in a new transaction, so we have
          effects shouldBe mutable.Buffer(
            Effect("down-obs", 2),
            Effect("down-obs", 3),
          )
        //}

        calculations.clear()
        effects.clear()

      }
    }
  }

  it("signal.changes / startWith potential glitch") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val log = mutable.Buffer[String]()

    val $v = Var(1)

    def $changes(name: String) = $v
      .signal.setDisplayName("VarSignal")
      .changes.setDisplayName("VarSignal.changes." + name)

    val $isPositive = $changes("IsPositive").map { num =>
      val isPositive = num > 0
      log += s"$num isPositive = $isPositive"
      isPositive
    }.setDisplayName("IsPositive")

    val $isEven = $changes("IsEven").map { num =>
      val isEven = num % 2 == 0
      log += s"$num isEven = $isEven"
      isEven
    }.setDisplayName("IsEven")

    val $combined = $changes("Combined").combineWithFn($isPositive, $isEven) { (num, isPositive, isEven) =>
      log += s"$num isPositive = $isPositive, isEven = $isEven"
      (isPositive, isEven)
    }.setDisplayName("Combined")

    val $result = $combined.startWith((false, false)).setDisplayName("Result")

    val sub1 = $result.addObserver(Observer.empty)

    log.toList shouldBe Nil

    // --

    $v.set(2)

    log.toList shouldBe List(
      "2 isPositive = true",
      "2 isEven = true",
      "2 isPositive = true, isEven = true"
    )
    log.clear()

    // --

    $v.set(3)

    log.toList shouldBe List(
      "3 isPositive = true",
      "3 isEven = false",
      "3 isPositive = true, isEven = false"
    )
    log.clear()

    // --

    sub1.kill()

    $v.set(-4)

    log.toList shouldBe List()

    // --

    // This part needs special logic to avoid glitches.
    // In signal.changes, we use Transaction.onStart.pendingCallbacks to collect all sync-related events onStart,
    // and fire them off all in one Transaction.
    // We're only adding one observer here, but the case of multiple observers is actually more complicated,
    // see the test below

    $combined.addObserver(Observer.empty)

    log.toList shouldBe List(
      "-4 isEven = true",
      "-4 isPositive = false",
      "-4 isPositive = false, isEven = true"
    )
    log.clear()

    // --

    $v.set(-6)

    log.toList shouldBe List(
      "-6 isPositive = false",
      "-6 isEven = true",
      "-6 isPositive = false, isEven = true"
    )
    log.clear()

  }

  it("signal.changes sync with multiple observers (Transaction.onStart.pendingCallbacks)") {

    val log = mutable.Buffer[String]()

    val v = Var(1)

    // #Note: in this test the changes are shared, intentionally.
    val changes = v
      .signal.setDisplayName("VarSignal")
      .changes.setDisplayName("VarSignal.changes")

    val isPositive = changes.map { num =>
      val isPositive = num > 0
      log += s"$num isPositive = $isPositive"
      isPositive
    }.setDisplayName("IsPositive")

    val isEven = changes.map { num =>
      val isEven = num % 2 == 0
      log += s"$num isEven = $isEven"
      isEven
    }.setDisplayName("IsEven")

    val combined = changes.combineWithFn(isPositive, isEven) { (num, isPositive, isEven) =>
      log += s"$num isPositive = $isPositive, isEven = $isEven"
      (isPositive, isEven)
    }.setDisplayName("Combined")

    val owner = new TestableOwner

    val subs1 = List(
      isPositive.addObserver(Observer.empty)(owner),
      isEven.addObserver(Observer.empty)(owner),
      combined.addObserver(Observer.empty)(owner)
    )

    log.toList shouldBe Nil

    // --

    subs1.foreach(_.kill())

    v.set(-2)

    // #Warning This is a known glitch: of all the new observers, only the first one
    //  manages to receive the sync event from `changes`. This is because the whole
    //  onWillStart / onStart loop completes before the other observers are added,
    //  and even though the event is emitted in a new Transaction, in this case, the
    //  transaction payload is executed immediately without delay, because there is
    //  no current transaction that we need to wait for.
    //  - This glitch can be avoided by wrapping subs creation in `new Transaction`,
    //    `DynamicSubscription`, or `Transaction.onStart.shared` – see the tests below

    val subs2 = List(
      isPositive.addObserver(Observer.empty)(owner),
      isEven.addObserver(Observer.empty)(owner),
      combined.addObserver(Observer.empty)(owner)
    )

    log.toList shouldBe List(
      "-2 isPositive = false"
    )
    log.clear()

    // --

    subs2.foreach(_.kill())

    v.set(5)

    val subs3 = Transaction.onStart.shared {
      List(
        isPositive.addObserver(Observer.empty)(owner),
        isEven.addObserver(Observer.empty)(owner),
        combined.addObserver(Observer.empty)(owner)
      )
    }

    log.toList shouldBe List(
      "5 isPositive = true",
      "5 isEven = false",
      "5 isPositive = true, isEven = false"
    )
    log.clear()

    // --

    subs3.foreach(_.kill())

    v.set(-8)

    val dynOwner = new DynamicOwner(() => throw new Exception("Accessing dynamic owner after it is killed"))
    DynamicSubscription.unsafe(
      dynOwner,
      activate = { o =>
        isPositive.addObserver(Observer.empty)(o)
        isEven.addObserver(Observer.empty)(o)
        combined.addObserver(Observer.empty)(o)
      }
    )

    // This uses nesting too, so it should have no glitches.
    dynOwner.activate()

    log.toList shouldBe List(
      "-8 isPositive = false",
      "-8 isEven = true",
      "-8 isPositive = false, isEven = true"
    )
    log.clear()

    // --

    dynOwner.deactivate()

    v.set(11)

    // With `new Transaction`, these could be delayed if we're already
    // inside a transaction (in the test, we aren't), and also we can't
    // return the value from inside the transaction (due to the potential
    // delay)
    new Transaction(_ => {
      isPositive.addObserver(Observer.empty)(owner)
      isEven.addObserver(Observer.empty)(owner)
      combined.addObserver(Observer.empty)(owner)
    })

    log.toList shouldBe List(
      "11 isPositive = true",
      "11 isEven = false",
      "11 isPositive = true, isEven = false"
    )
    log.clear()
  }

  it("onStart shared transaction is resilient to exceptions") {

    val accessAfterKillErrorMsg = "Accessing dynamic owner after it is killed"

    val sharedBlockErrorMsg = "Shared block error"

    val errorNumber = 42

    val errorNumberMsg = s"Error$errorNumber"

    val log = mutable.Buffer[String]()

    val v = Var(-2)

    // #Note: in this test the changes are shared, intentionally.
    val changes = v
      .signal.setDisplayName("VarSignal")
      .changes.setDisplayName("VarSignal.changes")

    val isPositive = changes.map { num =>
      val isPositive = num > 0
      log += s"$num isPositive = $isPositive"
      isPositive
    }.setDisplayName("IsPositive")

    val isEven = changes.map { num =>
      if (num == errorNumber) {
        throw new Exception(errorNumberMsg)
      }
      val isEven = num % 2 == 0
      log += s"$num isEven = $isEven"
      isEven
    }.setDisplayName("IsEven")

    val combined = changes.combineWithFn(isPositive, isEven) { (num, isPositive, isEven) =>
      log += s"$num isPositive = $isPositive, isEven = $isEven"
      (isPositive, isEven)
    }.setDisplayName("Combined")

    val owner = new TestableOwner

    val subs1 = List(
      isPositive.addObserver(Observer.empty)(owner),
      isEven.addObserver(Observer.empty)(owner),
      combined.addObserver(Observer.empty)(owner)
    )

    log.toList shouldBe Nil

    // --

    subs1.foreach(_.kill())

    v.set(5)

    val subs2 = mutable.Buffer[Subscription]()

    val caught1 = intercept[Exception] {
      Transaction.onStart.shared {
        subs2.append(isPositive.addObserver(Observer.empty)(owner))
        subs2.append(isEven.addObserver(Observer.empty)(owner))
        throw new Exception(sharedBlockErrorMsg)
        // subs2.append(combined.addObserver(Observer.empty)(owner))
      }
    }
    assert(caught1.getMessage == sharedBlockErrorMsg)

    log.toList shouldBe List(
      "5 isPositive = true",
      "5 isEven = false"
    )
    log.clear()

    // --

    v.set(-6)

    log.toList shouldBe List(
      "-6 isPositive = false",
      "-6 isEven = true"
    )
    log.clear()

    // --

    subs2.append(combined.addObserver(Observer.empty)(owner))

    v.set(7)

    log.toList shouldBe List(
      "7 isPositive = true",
      "7 isEven = false",
      "7 isPositive = true, isEven = false"
    )
    log.clear()

    // --

    subs2.foreach(_.kill())

    v.set(-8)

    val dynOwner = new DynamicOwner(() => throw new Exception(accessAfterKillErrorMsg))

    var tempOwner: Owner = null

    DynamicSubscription.unsafe(
      dynOwner,
      activate = { o =>
        tempOwner = o
        isPositive.addObserver(Observer.empty)(o)
        isEven.addObserver(Observer.empty)(o)
        throw new Exception(sharedBlockErrorMsg)
        // combined.addObserver(Observer.empty)(o)
      }
    )

    val caught2 = intercept[Exception] {
      dynOwner.activate()
    }

    log.toList shouldBe List(
      "-8 isPositive = false",
      "-8 isEven = true"
    )
    log.clear()

    assert(caught2.getMessage == sharedBlockErrorMsg)

    // --

    v.set(9)

    log.toList shouldBe List(
      "9 isPositive = true",
      "9 isEven = false"
    )
    log.clear()

    // -- try to use expired owner

    dynOwner.deactivate()

    v.set(-10)

    val caught3 = intercept[Exception] {
      combined.addObserver(Observer.empty)(tempOwner)
    }

    assert(caught3.getMessage == accessAfterKillErrorMsg)

    log.toList shouldBe Nil

    // -- make sure stuff still works

    val caught4 = intercept[Exception] {
      dynOwner.activate()
    }

    assert(caught4.getMessage == sharedBlockErrorMsg)

    // this is added too late, so it does not get the -10 event yet
    combined.addObserver(Observer.empty)(tempOwner)

    log.toList shouldBe List(
      "-10 isPositive = false",
      "-10 isEven = true"
    )
    log.clear()

    // --

    v.set(11)

    log.toList shouldBe List(
      "11 isPositive = true",
      "11 isEven = false",
      "11 isPositive = true, isEven = false"
    )
    log.clear()


    // --

    dynOwner.deactivate()

    v.set(11)

    val unhandledErrors = mutable.Buffer[String]()

    val errorCallback = (err: Throwable) => {
      unhandledErrors.append(err.getMessage)
      ()
    }

    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)

    try {
      new Transaction(_ => {
        isPositive.addObserver(Observer.empty)(owner)
        isEven.addObserver(Observer.empty)(owner)
        throw new Exception(sharedBlockErrorMsg)
        // combined.addObserver(Observer.empty)(owner)
      })

      log.toList shouldBe List(
        "11 isPositive = true",
        "11 isEven = false"
      )
      log.clear()

      unhandledErrors.toList shouldBe List(
        sharedBlockErrorMsg
      )
      unhandledErrors.clear()

      // --

      new Transaction(_ => {
        combined.addObserver(Observer.empty)(owner)
      })

      v.set(-12)

      log.toList shouldBe List(
        "-12 isPositive = false",
        "-12 isEven = true",
        "-12 isPositive = false, isEven = true"
      )
      log.clear()

      unhandledErrors.toList shouldBe Nil

      // --

      v.set(errorNumber)

      log.toList shouldBe List(
        "42 isPositive = true"
      )
      log.clear()

      unhandledErrors.toList shouldBe List(
        errorNumberMsg,
        s"CombinedError: $errorNumberMsg"
      )
      unhandledErrors.clear()

      // --

      v.set(13)

      log.toList shouldBe List(
        "13 isPositive = true",
        "13 isEven = false",
        "13 isPositive = true, isEven = false"
      )
      log.clear()

      unhandledErrors.toList shouldBe Nil

    } finally {
      AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
      AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
      assert(unhandledErrors.isEmpty)
    }

  }


  it("CombineEventStreamN") {

    implicit val testOwner: TestableOwner = new TestableOwner

    case class T1(v: Int)
    case class T2(v: Int)

    val bus1 = new EventBus[T1]()
    val bus2 = new EventBus[T2]()

    val combinedStream = EventStream.combine(bus1, bus2)

    val effects = mutable.Buffer[(T1, T2)]()

    val observer = Observer[(T1, T2)](effects += _)

    // --

    effects.toList shouldBe Nil

    // --

    val sub1 = combinedStream.addObserver(observer)

    effects.toList shouldBe Nil

    // --

    bus1.writer.onNext(T1(0))

    effects.toList shouldBe Nil

    // --

    bus2.writer.onNext(T2(0))

    effects.toList shouldBe List(
      (T1(0), T2(0))
    )
    effects.clear()

    // --

    bus2.writer.onNext(T2(1))

    effects.toList shouldBe List(
      (T1(0), T2(1))
    )
    effects.clear()

    // --

    bus1.writer.onNext(T1(10))

    effects.toList shouldBe List(
      (T1(10), T2(1))
    )
    effects.clear()

    // --

    sub1.kill()

    bus2.writer.onNext(T2(2))

    effects.toList shouldBe Nil

    // --

    combinedStream.addObserver(observer)

    effects.toList shouldBe Nil

    // --

    bus1.writer.onNext(T1(20))

    effects.toList shouldBe List(
      (T1(20), T2(1))
    )
    effects.clear()

    // --

    bus2.writer.onNext(T2(3))

    effects.toList shouldBe List(
      (T1(20), T2(3))
    )
    effects.clear()

  }

  it("CombineSignalN") {

    implicit val testOwner: TestableOwner = new TestableOwner

    case class T1(v: Int)
    case class T2(v: Int)

    val calculations = mutable.Buffer[Calculation[Int]]()
    val effects = mutable.Buffer[(T1, T2)]()

    val $var1 = Var(T1(0))
    val $var2 = Var(T2(0))

    val combinedSignal = $var1
      .signal
      .setDisplayName("var1.signal")
      .map { t =>
        calculations += Calculation("signal1", t.v)
        t
      }
      .setDisplayName("var1.signal.map")
      .combineWith(
        $var2
          .signal
          .setDisplayName("var2.signal")
          .map { t =>
            calculations += Calculation("signal2", t.v)
            t
          }
          .setDisplayName("var2.signal.map")
      )
      .setDisplayName("combineSignal")

    val observer = Observer[(T1, T2)](effects += _)

    // --

    calculations.toList shouldBe Nil
    effects.toList shouldBe Nil

    // --

    val sub1 = combinedSignal.addObserver(observer)

    calculations.toList shouldBe List(
      Calculation("signal1", 0),
      Calculation("signal2", 0),
    )
    effects.toList shouldBe List(
      (T1(0), T2(0))
    )

    calculations.clear()
    effects.clear()


    // --

    $var1.writer.onNext(T1(1))

    calculations.toList shouldBe List(
      Calculation("signal1", 1)
    )
    effects.toList shouldBe List(
      (T1(1), T2(0))
    )

    calculations.clear()
    effects.clear()

    // --

    $var2.writer.onNext(T2(2))

    calculations.toList shouldBe List(
      Calculation("signal2", 2)
    )
    effects.toList shouldBe List(
      (T1(1), T2(2))
    )
    calculations.clear()
    effects.clear()

    // --

    sub1.kill()

    $var2.writer.onNext(T2(3))

    calculations.toList shouldBe Nil
    effects.toList shouldBe Nil

    // --

    combinedSignal.addObserver(observer)

    calculations.toList shouldBe List(
      // Calculation("signal1", 1),
      Calculation("signal2", 3)
    )
    effects.toList shouldBe List(
      (T1(1), T2(3))
    )

    calculations.clear()
    effects.clear()

    // --


    $var1.writer.onNext(T1(10))

    calculations.toList shouldBe List(
      Calculation("signal1", 10)
    )
    effects.toList shouldBe List(
      (T1(10), T2(3))
    )

    calculations.clear()
    effects.clear()

  }

  it("MapSignal pull") {

    implicit val testOwner: TestableOwner = new TestableOwner

    val effects = mutable.Buffer[Effect[Int]]()
    val calculations = mutable.Buffer[Calculation[Int]]()

    val $var = Var(1)

    val signal = $var
      .signal
      .map(Calculation.log("signal", calculations))

    val signal_x10 = signal
      .map(_ * 10)
      .map(Calculation.log("signal_x10", calculations))

    // #TODO[Scala3] Scala 3.0.2 requires the manual `Int` type ascription here, boo
    val obs = Observer[Int](Effect.log("obs", effects))
    val obs_x10 = Observer[Int](Effect.log("obs_x10", effects))

    // --

    signal.addObserver(obs)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 1)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 1)
    )
    calculations.clear()
    effects.clear()

    // --

    val sub1_x10 = signal_x10.addObserver(obs_x10)

    calculations shouldBe mutable.Buffer(
      Calculation("signal_x10", 10)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs_x10", 10)
    )

    calculations.clear()
    effects.clear()

    // --

    $var.set(2)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 2),
      Calculation("signal_x10", 20)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 2),
      Effect("obs_x10", 20)
    )

    calculations.clear()
    effects.clear()

    // --

    sub1_x10.kill()

    $var.set(3)

    calculations shouldBe mutable.Buffer(
      Calculation("signal", 3)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs", 3)
    )
    calculations.clear()
    effects.clear()

    // --

    signal_x10.addObserver(obs_x10)

    calculations shouldBe mutable.Buffer(
      Calculation("signal_x10", 30)
    )
    effects shouldBe mutable.Buffer(
      Effect("obs_x10", 30)
    )
    calculations.clear()
    effects.clear()

  }

  it("Flattened signals pull check") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[String]]()

    val outerBus = new EventBus[Int].setDisplayName("OuterBus")

    val smallSignal = EventStream
      .fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true)
      .setDisplayName("SmallSeqStream")
      .startWith("small-0")
      .setDisplayName("SmallSignal")

    val bigSignal = EventStream
      .fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true)
      .setDisplayName("BigSeqStream")
      .startWith("big-0")
      .setDisplayName("BigSignal")

    val flatSignal = outerBus
      .events
      .startWith(0)
      .setDisplayName("OuterBus.startWith")
      .map {
        case i if i >= 10 => bigSignal
        case _ => smallSignal
      }
      .setDisplayName("MetaSignal")
      .flatten
      .setDisplayName("FlatSignal")
      .map(Calculation.log("flat", calculations))
      .setDisplayName("FlatSignal--LOG")

    // --

    flatSignal.addObserver(Observer.empty)

    assert(calculations.toList == List(
      Calculation("flat", "small-0"),
      Calculation("flat", "small-1"),
      Calculation("flat", "small-2"),
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(1)

    assert(calculations.toList == Nil)

    calculations.clear()

    // --

    outerBus.writer.onNext(2)

    assert(calculations.toList == Nil)

    calculations.clear()

    // --

    outerBus.writer.onNext(10) // #Note switch to big

    assert(calculations.toList == List(
      Calculation("flat", "big-0"),
      Calculation("flat", "big-1"),
      Calculation("flat", "big-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(11)

    assert(calculations.toList == Nil)

    calculations.clear()

    // --

    outerBus.writer.onNext(5) // #Note switch back to small

    assert(calculations.toList == List(
      Calculation("flat", "small-2") // Restore current value of small signal
    ))

    calculations.clear()

  }

  it("Flattened signals pull check 2") {

    implicit val owner: TestableOwner = new TestableOwner

    val calculations = mutable.Buffer[Calculation[String]]()

    // It's important that we reuse the exact same references to inner signals to check the logic
    // - fromSeq streams are used to ensure that onStart isn't called extraneously
    // - bus.events streams are used to ensure that onStop isn't called extraneously

    val outerBus = new EventBus[Int].setDisplayName("OuterBus")

    val smallBus = new EventBus[String].setDisplayName("SmallBus")

    val bigBus = new EventBus[String].setDisplayName("BigBus")

    val smallSignal = EventStream.merge(
      smallBus.events,
      EventStream.fromSeq("small-1" :: "small-2" :: Nil, emitOnce = true).setDisplayName("SmallSeqStream")
    ).setDisplayName("SmallMergeStream").startWith("small-0").setDisplayName("SmallSignal")

    val bigSignal = EventStream.merge(
      bigBus.events,
      EventStream.fromSeq("big-1" :: "big-2" :: Nil, emitOnce = true).setDisplayName("BigSeqStream")
    ).setDisplayName("BigMergeStream").startWith("big-0").setDisplayName("BigSignal")

    val flatSignal = outerBus.events.startWith(0).setDisplayName("OuterBus.startWith").flatMap {
      case i if i >= 10 => bigSignal
      case _ => smallSignal
    }.setDisplayName("FlatSignal").map(Calculation.log("flat", calculations)).setDisplayName("FlatSignal--LOG")

    // --

    flatSignal.addObserver(Observer.empty)

    assert(calculations.toList == List(
      Calculation("flat", "small-0"),
      Calculation("flat", "small-1"),
      Calculation("flat", "small-2"),
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-0")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-0")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(1)

    assert(calculations.toList == Nil)

    // --

    smallBus.writer.onNext("small-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(2)

    assert(calculations.toList == Nil)

    // --

    smallBus.writer.onNext("small-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(10) // #Note switch to big

    assert(calculations.toList == List(
      Calculation("flat", "big-0"),
      Calculation("flat", "big-1"),
      Calculation("flat", "big-2")
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small bus - unrelated change")

    assert(calculations.isEmpty)

    // --

    bigBus.writer.onNext("big-bus-1")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-1")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(11)

    assert(calculations.toList == Nil)

    // --

    bigBus.writer.onNext("big-bus-2")

    assert(calculations.toList == List(
      Calculation("flat", "big-bus-2")
    ))

    calculations.clear()

    // --

    outerBus.writer.onNext(5) // #Note switch back to small

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-2") // Restore current value of small signal
    ))

    calculations.clear()

    // --

    smallBus.writer.onNext("small-bus-3")

    assert(calculations.toList == List(
      Calculation("flat", "small-bus-3")
    ))

    calculations.clear()

    // --

    bigBus.writer.onNext("big bus - unrelated change")

    assert(calculations.isEmpty)
  }
}

package com.raquo.airstream.ownership

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.fixtures.Effect
import com.raquo.airstream.state.Var

import scala.collection.mutable

class DynamicOwnerSpec extends UnitSpec {

  private def makeDynamicOwner(
    effects: mutable.Buffer[Effect[String]],
    injectPrepend: Boolean
  ): DynamicOwner = {
    val dynOwner = new DynamicOwner(() => fail("Attempted to use permakilled owner!"))
    if (injectPrepend) {
      createSub("dynSubPrepend1", dynOwner, effects, prepend = true)
      createSub("dynSubPrepend2", dynOwner, effects, prepend = true)
    }
    dynOwner
  }

  private def createSub(
    label: String,
    dynOwner: DynamicOwner,
    effects: mutable.Buffer[Effect[String]],
    onActivate: () => Unit = () => (),
    onDeactivate: () => Unit = () => (),
    prepend: Boolean = false
  ): DynamicSubscription = {
    DynamicSubscription
      .unsafe(
        dynOwner,
        owner => {
          effects.append(Effect(label, "activate"))
          onActivate()
          new Subscription(owner, cleanup = () => {
            effects.append(Effect(label, "deactivate"))
            onDeactivate()
          })
        },
        prepend = prepend
      )
      .setDisplayName(label)
  }

  it("Dynamic owner activation and deactivation") {

    val bus1 = new EventBus[Int]
    val var2 = Var(0)

    val effects = mutable.Buffer[Effect[Int]]()

    val obs1 = Observer[Int](effects += Effect("obs1", _))
    val obs2 = Observer[Int](effects += Effect("obs2", _))

    val dynOwner = new DynamicOwner(() => fail("Attempted to use permakilled owner!"))

    DynamicSubscription.unsafe(dynOwner, owner => bus1.events.addObserver(obs1)(owner))

    bus1.writer.onNext(100)

    effects shouldBe mutable.Buffer()
    dynOwner.isActive shouldBe false
    dynOwner.maybeCurrentOwner shouldBe None

    // --

    dynOwner.activate()

    effects shouldBe mutable.Buffer()
    dynOwner.isActive shouldBe true

    // --

    bus1.writer.onNext(200)
    effects shouldBe mutable.Buffer(Effect("obs1", 200))
    effects.clear()

    // --

    val dynSub2 = DynamicSubscription.subscribeObserver(dynOwner, var2.signal, obs2)
    effects shouldBe mutable.Buffer(Effect("obs2", 0))
    effects.clear()

    // --

    dynOwner.deactivate()
    bus1.writer.onNext(300)
    var2.writer.onNext(3)

    effects shouldBe mutable.Buffer()

    // --

    dynOwner.activate() // this subscribes to the signal. It remembers 3 despite deactivation because Var is a StrictSignal. Not the best test I guess.
    bus1.writer.onNext(400)
    var2.writer.onNext(4)

    effects shouldBe mutable.Buffer(Effect("obs2", 3), Effect("obs1", 400), Effect("obs2", 4))
    effects.clear()

    // --

    bus1.writer.onNext(500)
    var2.writer.onNext(5)

    effects shouldBe mutable.Buffer(Effect("obs1", 500), Effect("obs2", 5))
    effects.clear()

    // --

    dynSub2.kill() // permanently deactivated and removed from owner
    bus1.writer.onNext(600)
    var2.writer.onNext(6)

    effects shouldBe mutable.Buffer(Effect("obs1", 600))
    effects.clear()

    // --

    dynOwner.deactivate()
    bus1.writer.onNext(700)
    var2.writer.onNext(7)

    effects shouldBe mutable.Buffer()

    // --

    dynOwner.activate()

    effects shouldBe mutable.Buffer()

    // --

    bus1.writer.onNext(800)
    var2.writer.onNext(8)

    effects shouldBe mutable.Buffer(Effect("obs1", 800))
    effects.clear()
  }

  Seq(false, true).foreach { injectPrepend =>

    // https://github.com/raquo/Airstream/issues/145

    val clue = if (injectPrepend) "[with-prepend] " else "[no-prepend] "

    def prependEffects(effect: String) =
      if (injectPrepend) {
        List(
          // Inverted order because prepending
          Effect("dynSubPrepend2", effect),
          Effect("dynSubPrepend1", effect)
        )
      } else {
        Nil
      }

    it(clue + "remove dyn sub during activation - baseline") {

      // #Note: No dyn sub removal in this test

      val effects = mutable.Buffer[Effect[String]]()

      val dynOwner = makeDynamicOwner(effects, injectPrepend).setDisplayName("dynOwner")

      lazy val dynSub1 = createSub("dynSub1", dynOwner, effects)
      lazy val dynSub2 = createSub("dynSub2", dynOwner, effects)
      lazy val dynSub3 = createSub("dynSub3", dynOwner, effects)

      // Add the subs to dyn owner in strict order
      val _ = (dynSub1, dynSub2, dynSub3)

      assertEquals(dynOwner.isActive, false)
      assertEquals(effects.toList, Nil)

      // --

      dynOwner.activate()

      assertEquals(
        effects.toList,
        prependEffects("activate") ++ List(
          Effect("dynSub1", "activate"),
          Effect("dynSub2", "activate"),
          Effect("dynSub3", "activate")
        )
      )
      effects.clear()

      // --

      dynOwner.deactivate()

      assertEquals(
        effects.toList,
        prependEffects("deactivate") ++ List(
          Effect("dynSub1", "deactivate"),
          Effect("dynSub2", "deactivate"),
          Effect("dynSub3", "deactivate")
        )
      )
      effects.clear()
    }

    it(clue + "remove dyn sub during activation - removing sub that was not yet iterated over") {

      val effects = mutable.Buffer[Effect[String]]()

      val dynOwner = makeDynamicOwner(effects, injectPrepend).setDisplayName("dynOwner")

      lazy val dynSub1 = createSub("dynSub1", dynOwner, effects, onActivate = () => dynSub2.kill())
      lazy val dynSub2 = createSub("dynSub2", dynOwner, effects)
      lazy val dynSub3 = createSub("dynSub3", dynOwner, effects)

      // Add the subs to dyn owner in strict order
      val _ = (dynSub1, dynSub2, dynSub3)

      assertEquals(dynOwner.isActive, false)
      assertEquals(effects.toList, Nil)

      // --

      dynOwner.activate()

      assertEquals(
        effects.toList,
        prependEffects("activate") ++ List(
          Effect("dynSub1", "activate"),
          Effect("dynSub3", "activate")
        )
      )
      effects.clear()

      // --

      dynOwner.deactivate()

      assertEquals(
        effects.toList,
        prependEffects("deactivate") ++ List(
          Effect("dynSub1", "deactivate"),
          Effect("dynSub3", "deactivate")
        )
      )
      effects.clear()
    }

    it(clue + "remove dyn sub during activation - removing sub that was already iterated over") {

      val effects = mutable.Buffer[Effect[String]]()

      val dynOwner = makeDynamicOwner(effects, injectPrepend).setDisplayName("dynOwner")

      lazy val dynSub1 = createSub("dynSub1", dynOwner, effects)
      lazy val dynSub2 = createSub("dynSub2", dynOwner, effects, onActivate = () => dynSub1.kill())
      lazy val dynSub3 = createSub("dynSub3", dynOwner, effects)

      // Add the subs to dyn owner in strict order
      val _ = (dynSub1, dynSub2, dynSub3)

      assertEquals(dynOwner.isActive, false)
      assertEquals(effects.toList, Nil)

      // --

      dynOwner.activate()

      assertEquals(
        effects.toList,
        prependEffects("activate") ++ List(
          Effect("dynSub1", "activate"),
          Effect("dynSub2", "activate"),
          Effect("dynSub1", "deactivate"),
          Effect("dynSub3", "activate")
        )
      )
      effects.clear()

      // --

      dynOwner.deactivate()

      assertEquals(
        effects.toList,
        prependEffects("deactivate") ++ List(
          Effect("dynSub2", "deactivate"),
          Effect("dynSub3", "deactivate")
        )
      )
      effects.clear()
    }

    it(clue + "remove dyn sub during activation - removing sub that is being activated") {

      val effects = mutable.Buffer[Effect[String]]()

      val dynOwner = makeDynamicOwner(effects, injectPrepend).setDisplayName("dynOwner")

      lazy val dynSub1 = createSub("dynSub1", dynOwner, effects)
      lazy val dynSub2: DynamicSubscription = createSub("dynSub2", dynOwner, effects, onActivate = () => dynSub2.kill())
      lazy val dynSub3 = createSub("dynSub3", dynOwner, effects)

      // Add the subs to dyn owner in strict order
      val _ = (dynSub1, dynSub2, dynSub3)

      assertEquals(dynOwner.isActive, false)
      assertEquals(effects.toList, Nil)

      // --

      dynOwner.activate()

      assertEquals(
        effects.toList,
        prependEffects("activate") ++ List(
          Effect("dynSub1", "activate"),
          Effect("dynSub2", "activate"),
          Effect("dynSub2", "deactivate"),
          Effect("dynSub3", "activate")
        )
      )
      effects.clear()

      // --

      dynOwner.deactivate()

      assertEquals(
        effects.toList,
        prependEffects("deactivate") ++ List(
          Effect("dynSub1", "deactivate"),
          Effect("dynSub3", "deactivate")
        )
      )
      effects.clear()
    }

    it(clue + "remove dyn sub during activation - remove sub before adding another sub") {

      val effects = mutable.Buffer[Effect[String]]()

      val dynOwner = makeDynamicOwner(effects, injectPrepend).setDisplayName("dynOwner")

      lazy val dynSub1 = createSub("dynSub1", dynOwner, effects, onActivate = () => dynSub2.kill())
      lazy val dynSub2: DynamicSubscription = createSub("dynSub2", dynOwner, effects)
      lazy val dynSub3 = createSub("dynSub3", dynOwner, effects, onActivate = () => createSub("dynSub4", dynOwner, effects))

      // Add the subs to dyn owner in strict order
      val _ = (dynSub1, dynSub2, dynSub3)

      assertEquals(dynOwner.isActive, false)
      assertEquals(effects.toList, Nil)

      // --

      dynOwner.activate()

      assertEquals(
        effects.toList,
        prependEffects("activate") ++ List(
          Effect("dynSub1", "activate"),
          Effect("dynSub3", "activate"),
          Effect("dynSub4", "activate")
        )
      )
      effects.clear()

      // --

      dynOwner.deactivate()

      assertEquals(
        effects.toList,
        prependEffects("deactivate") ++ List(
          Effect("dynSub1", "deactivate"),
          Effect("dynSub3", "deactivate"),
          Effect("dynSub4", "deactivate")
        )
      )
      effects.clear()
    }

    it(clue + "remove dyn sub during activation - remove already-iterated sub after it adds another sub") {

      val effects = mutable.Buffer[Effect[String]]()

      val dynOwner = makeDynamicOwner(effects, injectPrepend).setDisplayName("dynOwner")

      var dynSub4Opt: Option[DynamicSubscription] = None

      lazy val dynSub1 = createSub(
        "dynSub1",
        dynOwner,
        effects,
        onActivate = () => dynSub4Opt = Some(createSub("dynSub4", dynOwner, effects)),
        onDeactivate = () => dynSub4Opt.foreach(_.kill())
      )
      lazy val dynSub2: DynamicSubscription = createSub("dynSub2", dynOwner, effects, onActivate = () => dynSub1.kill())
      lazy val dynSub3 = createSub("dynSub3", dynOwner, effects)

      // Add the subs to dyn owner in strict order
      val _ = (dynSub1, dynSub2, dynSub3)

      assertEquals(dynOwner.isActive, false)
      assertEquals(effects.toList, Nil)

      // --

      dynOwner.activate()

      assertEquals(
        effects.toList,
        prependEffects("activate") ++ List(
          Effect("dynSub1", "activate"),
          Effect("dynSub4", "activate"),
          Effect("dynSub2", "activate"),
          Effect("dynSub1", "deactivate"),
          Effect("dynSub4", "deactivate"),
          Effect("dynSub3", "activate"),
        )
      )
      effects.clear()

      // --

      dynOwner.deactivate()

      assertEquals(
        effects.toList,
        prependEffects("deactivate") ++ List(
          Effect("dynSub2", "deactivate"),
          Effect("dynSub3", "deactivate"),
        )
      )
      effects.clear()
    }

    it(clue + "remove dyn sub during activation - remove yet-to-be-iterated sub after it adds another sub") {

      val effects = mutable.Buffer[Effect[String]]()

      val dynOwner = makeDynamicOwner(effects, injectPrepend).setDisplayName("dynOwner")

      var dynSub4Opt: Option[DynamicSubscription] = None

      lazy val dynSub1 = createSub("dynSub1", dynOwner, effects)
      lazy val dynSub2: DynamicSubscription = createSub("dynSub2", dynOwner, effects, onActivate = () => dynSub3.kill())
      lazy val dynSub3 = createSub(
        "dynSub3",
        dynOwner,
        effects,
        onActivate = () => dynSub4Opt = Some(createSub("dynSub4", dynOwner, effects)),
        onDeactivate = () => dynSub4Opt.foreach(_.kill())
      )

      // Add the subs to dyn owner in strict order
      val _ = (dynSub1, dynSub2, dynSub3)

      assertEquals(dynOwner.isActive, false)
      assertEquals(effects.toList, Nil)

      // --

      dynOwner.activate()

      assertEquals(
        effects.toList,
        prependEffects("activate") ++ List(
          Effect("dynSub1", "activate"),
          Effect("dynSub2", "activate"),
        )
      )
      effects.clear()

      // --

      dynOwner.deactivate()

      assertEquals(
        effects.toList,
        prependEffects("deactivate") ++ List(
          Effect("dynSub1", "deactivate"),
          Effect("dynSub2", "deactivate"),
        )
      )
      effects.clear()
    }
  }
}

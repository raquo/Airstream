# Airstream

[![Build Status](https://circleci.com/gh/raquo/Airstream.svg?style=svg)](https://circleci.com/gh/raquo/Airstream)
[![Chat on https://discord.gg/JTrUxhq7sj](https://img.shields.io/badge/chat-on%20discord-7289da.svg)](https://discord.gg/JTrUxhq7sj)
![Maven Central](https://img.shields.io/maven-central/v/com.raquo/airstream_sjs1_2.13.svg)

Airstream is a small state propagation and streaming library for Scala.js. Primary differences from other solutions:

- **Mandatory [ownership](#ownership) of leaky resources** – it is impossible to create a subscription without specifying when it shall be destroyed. This helps prevent memory leaks and unexpected behaviour.

- **No [FRP glitches](#frp-glitches)** – neither observables themselves nor their observers will ever see inconsistent state within a transaction, at no runtime cost.

- **One integrated system for two core types of observables** – Event streams alone are not a good enough abstraction for anything other than events.
  - EventStream for events (lazy, no current value)
  - Signal for state (lazy, has current value, only state-safe operators)

- **Small size, simple implementation** – easy to understand, easy to create custom observables. Does not bloat your Scala.js bundle size.

Airstream has a very generic design, but is primarily intended to serve as a reactive layer for unidirectional dataflow architecture in UI components. As such, it is not burdened by features that cause more problems than they solve in frontend development, such as backpressure and typed effects.

I created Airstream because I found existing solutions were not suitable for building reactive UI components. My original need for Airstream was to replace the previous reactive layer of [Laminar](https://laminar.dev), but I'll be happy to see it used by other reactive UI libraries as well. Another piece of Laminar you can reuse is [Scala DOM Types](https://github.com/raquo/scala-dom-types).

```
"com.raquo" %%% "airstream" % "0.14.5"  // Requires Scala.js >= 1.9.0
```



## Table of Contents

* [Community](#community)
* [Documentation](#documentation)
  * [EventStream](#eventstream)
  * [Laziness](#laziness)
    * [Starting Observables](#starting-observables)
    * [Stopping Observables](#stopping-observables)
    * [Memory Management Implications](#memory-management-implications)
  * [Signal](#signal)
    * [Getting Signal's current value](#getting-signals-current-value) 
  * [Relationship between EventStream and Signal](#relationship-between-eventstream-and-signal)
  * [Observer](#observer)
  * [Ownership](#ownership)
    * [Ownership & Memory Management](#ownership--memory-management)
    * [OneTimeOwner](#onetimeowner)
    * [ManualOwner](#manualowner)
    * [Dynamic Ownership](#dynamic-ownership)
  * [Sources of Events](#sources-of-events)
    * [Creating Observables from Futures and Promises](#creating-observables-from-futures)
    * [EventStream.fromSeq](#eventstreamfromseq)
    * [EventStream.periodic](#eventstreamperiodic)
    * [EventStream.empty](#eventstreamempty)
    * [EventStream.withCallback and withObserver](#eventstreamwithcallback-and-withobserver)
    * [EventBus](#eventbus)
    * [Var](#var)
    * [Val](#val)
    * [Ajax](#ajax)
    * [Websockets](#websockets)
    * [DOM Events](#dom-events)
    * [Custom Event Sources](#custom-event-sources)
    * [Extending Observables](#extending-observables)
  * [Sources & Sinks](#sources--sinks)
  * [FRP Glitches](#frp-glitches)
    * [Other Libraries](#other-libraries)
    * [Topological Rank](#topological-rank)
    * [Transactions](#transactions)
    * [Merge Glitch-By-Design](#merge-glitch-by-design)
    * [Scheduling of Transactions](#scheduling-of-transactions)
  * [Operators](#operators)
    * [N-arity Operators](#n-arity-operators)
    * [Compose Changes](#compose-changes)
    * [Sync Delay](#sync-delay)
    * [Splitting Observables](#splitting-observables)
    * [Flattening Observables](#flattening-observables)
  * [Debugging](#debugging)
  * [Error Handling](#error-handling)
* [Limitations](#limitations)
* [My Related Projects](#my-related-projects)



## Community

* [Discord](https://discord.gg/JTrUxhq7sj) for chat and random questions (Airstream shares this server with [Laminar](https://laminar.dev))
* [Github issues](https://github.com/raquo/Airstream/issues) for bugs, feature requests, and more in-depth discussions



## Documentation

[API doc](https://javadoc.io/doc/com.raquo/airstream_sjs1_2.13/latest/com/raquo/airstream/index.html)

The documentation provided below is a high level overview that occasionally dives into gritty details for things that are hard to figure out on your own. It is not a full replacement to discovering available methods by reading the code (which is quite simple, and has comments) or simply with an IDE's autocomplete functionality.

This documentation explains how _Airstream_ works. Although I try to explain all required concepts and the rationale for doing things a certain way, if you need a primer on reactive programming using streams, consider [this guide](https://gist.github.com/staltz/868e7e9bc2a7b8c1f754) by André Staltz or its [video adaptation](https://egghead.io/courses/introduction-to-reactive-programming). 

This documentation is intended to be read top to bottom, sections further down the line assume knowledge of concepts and behaviours introduced in earlier sections.

For examples of Airstream usage, see [laminar-examples](https://github.com/raquo/laminar-examples), [Laminar](https://github.com/raquo/Laminar) source code, as well as Airstream tests.


### EventStream

EventStream is a reactive variable that represents a stream of discrete events.

EventStream has no concept of "current value". It is a stream of discrete events, and there is no such thing as a "current event".

EventStream is a **lazy** observable. That means that it will not receive or process events unless it has at least one Observer listening to it (more on this below).

Generally, when you add an Observer to a stream, it starts to send events to the observer from that point on.

The result of calling `observable.addObserver(observer)(owner)` or `observable.foreach(onNext)(owner)` is a Subscription. To remove the observer manually, you can call `subscription.kill()`, but usually it's the `owner`'s job to do that. Hold that thought for now, read about owners later in the [Ownership](#ownership) section.


### Laziness

Before exploring Signals, the other kind of Observable, let's outline how exactly laziness works in Airstream. All Airstream Observables are lazy, but we will use EventStream-s here to make our explanation less abstract.

Every Observable has zero or more observers – both "external" observers that you add manually using `addObserver` or `foreach` methods, and InternalObserver-s. More on those soon.


#### Starting Observables

When a stream acquires its first observer (does not matter if external or internal), it is said to be **started**. So when you call `addObserver` on a stream for the first time, you **start** the stream. Airstream will then call the `stream.onStart` method, which must ensure that this stream wakes up and starts working. Someone started observing (caring about the output of) this stream – and so the stream must ensure that the events start coming in.

Usually the stream accomplishes that by adding an InternalObserver to the parent (upstream) stream – the stream on which this one depends. For example, let's consider this scenario:

```scala
val foo: EventStream[Foo] = ???
val bar: EventStream[Bar] = foo.map(fooToBar)
val baz: EventStream[Baz] = bar.map(barToBaz)
val qux: EventStream[Qux] = baz.map(bazToQux)
val rap: EventStream[Rap] = qux.map(quxToRap)

baz.addObserver(bazObserver)
```

Until `baz.addObserver(bazObserver)` was called, these streams would not be receiving or emitting any events because they have no observers, internal or external. After `baz.addObserver` is called, an external observer `bazObserver` is added to `baz`, starting it. Then, `baz.onStart` is called, adding `baz` as an InternalObserver to `bar`. `baz` will now receive and process any events emitted by `bar`.

But this means that `bar` just got its first observer – even if an internal one, it still matters – someone started caring, even if indirectly. So its `onStart` method is called, and it adds `bar` as the first InternalObserver to `foo`. Now, `foo` is started as well, and its `onStart` method does _something_ to ensure that `foo` will now start sending out events. We don't actually know what `foo`'s `onStart` method does because we didn't define `foo`'s implementation. For example, it could be adding a DOM listener to a DOM element.

Now we see how adding an observer resulted in a chain of activations of all upstream streams that were required, directly or indirectly, to get the events out of the stream we actually wanted to observe. The `onStart` method ensured – recursively – that the observed stream is now running.

Adding another observer to the now already running streams – `foo` or `bar` or `baz` – would not need to cause such a chain reaction because the stream to which it is being added already has observers (internal or external).

Lastly, notice that the `qux` and `rap` streams are untouched by all this. No one cares for their output yet, so those streams will not receive any events, and neither `bazToQux` nor `quxToRap` will ever run (well, not until we add observers that need them to run, directly or indirectly).

On a lower level, how exactly is it that `qux` will not run? Put simply, it needs to be getting events from `baz` to process them and produce its own events, but it's getting nothing from `baz` simply because at this point `baz` does not know that `qux` exists. `baz` sends out its events to all of its observers, but so far nothing added `qux` as an observer to `baz`.

For extra clarity, while the stream `rap` does depend on `qux`, `rap` itself has no observers, so it is **stopped**. Nothing started it yet, and so nothing triggered its `onStart` method which would have added `rap` as an InternalObserver to `qux`, starting `qux` recursively as described above. 


#### Stopping Observables

Just like Observers can be added to streams, they can also be removed, e.g. with `subscription.kill()`. When you remove the last observer (internal or external) from a stream, the stream is said to be **stopped**. The same domino effect as when starting streams applies, except the `onStop` method recursively undoes everything that was done by `onStart` – instead of adding an InternalObserver to parent stream, we remove it, and if that causes the grand-parent stream to be stopped, we call its `onStop` method, and the chain continues upstream. 

When the dust settles, streams that are now without observers (internal or external) will be stopped, and those that still have observers will otherwise be untouched, except they will stop referencing the now-stopped observables in their lists of internal observers.


#### Memory Management Implications

Every observable that depends on another – parent, or upstream observable, – always has a reference to that parent, regardless of whether it's started or stopped.

However, the parent/upstream observable has no references to its child/downstream observable(s) until the child observable is started. Only then does the parent obtain a reference to the child, adding it to the list of its internal observers. 

This has straightforward memory management implications: nothing in Airstream is keeping references to a *stopped* observable. So, if you don't have any of your own references to a stopped Observable, it will be garbage collected, as expected.  

However, a _started_ observable has additional references to it from:
 1) The parent/upstream observable on which this observable depends (via the parent's list of internal observers)
 2) The Subscription objects created by `addObserver` or `foreach` calls on this observable, if this observable has external observers. Those subscriptions are in turn referenced by their Owner-s (more on those later)

Remember that if a given observable is started, its parent is also guaranteed to be started, and so on. This creates a potentially long chain of observables that typically terminate with external observers on the downstream end, and some kind of event producer on the upstream end. All of these reference each other, directly or indirectly, and so will not be garbage collected unless there are no more references in your program to _any_ observable or observer in this whole graph.

Now imagine that in the chain of activated observables mentioned above the most downstream observable is related to a UI component that has since then been destroyed. You would want that now-irrelevant observable to be stopped in order for it to be garbage collected, since it's not needed anymore, but it will continue to run for as long as it has its observer. And if you forgot to remove that observer when you destroyed the UI component it related to, you got yourself a memory leak.  

This is a common memory management pattern for almost all streaming libraries out there, so this should come as no surprise to anyone familiar with event streams.

Some reactive UI libraries such as Outwatch give you a way to bind the lifecycle of subscriptions to the lifecycle of corresponding UI components, and that automatically kills the subscription (removes the observer) when the UI component it relates to is destroyed. However, the underlying streaming libraries that such UI libraries use have no concept of such binding, and so in those libraries you can manually call `stream.addObserver` and create a subscription that will not be automatically killed when the UI component that it conceptually relates to is unmounted.

What makes Airstream special is a built-in concept of ownership. When creating a leaky resource, e.g. when calling `addObserver`, you _have to_ also provide a reference to an Owner who will eventually kill the subscription. For example, that owner could be a UI component to which the subscription relates, and it could automatically kill all subscriptions that it owns when it is destroyed, allowing the now-irrelevant observables to be stopped and garbage collected. This is essentially how [Laminar](https://github.com/raquo/Laminar)'s `ReactiveElement` works. For more details, see the [Ownership](#ownership) section.



### Signal

Signal is a reactive variable that represents a time-varying value, or an accumulated value. In other words, "state".

Similar to EventStream, Signal is **lazy**, so everything in the [Laziness](#laziness) section applies to Signals as well.

Unlike EventStream, a Signal always has a current value. For instance, you could create a Signal by calling `val signal = eventStream.startWith(initialValue)`. In that example, `signal`'s current value would first equal to `initialValue`, and then any time `eventStream` emits a value, `signal`'s current value would be updated to the emitted value, and then `signal` would emit this new current value.

However, all of that would only happen if `signal` had one or more observers (because of [Laziness](#laziness)). If `signal` had no observers, its current value would be stuck at the last current value it saved while it had observers, or at `initialValue` if it _never_ had observers.

Unlike EventStream, Signal only fires an event when its next value is different from its current value. The comparison is made using Scala's `==` operator.

When adding an Observer to a Signal, the observer will immediately receive the signal's current value, as well as any future values. If you don't want the observer to receive the current value, observe the stream `signal.changes` instead.

Note: Signal's initial value is evaluated lazily. For example:

```scala
val fooStream: EventStream[Foo] = ???
val fooSignal: Signal[Foo] = fooStream.startWith(myFoo)
val barSignal: Signal[Bar] = fooSignal.map(fooToBar)
```

In this example, `barSignal`'s initial value would be equal to `fooToBar(myFoo)`, but that expression will not be evaluated until it is needed (i.e. until `barSignal` acquires an observer). And once evaluated, it will not be re-evaluated again.

Similarly, `myFoo` expression will _not_ be evaluated immediately as it is passed by name. It will only be evaluated if and when it is needed (e.g. to pass it down to an observer of `barSignal`).    

#### Getting Signal's current value

Signal's laziness means that its current value might get stale / inconsistent in the absence of observers. Airstream therefore does not allow you to access a Signal's current value _without proving that it has observers_. 

You can use `stream.withCurrentValueOf(signal).map((lastStreamEvent, signalCurrentValue) => ???)` to access `signal`'s current value. The resulting stream will still be lazy, but this way the processing of `currentValue` is just as lazy as `currentValue` itself, so there is no risk of looking at a stale `currentValue`.

If you don't need lastStreamEvent, use `stream.sample(signal).map(signalCurrentValue => ???)` instead. Note: both of these output streams will emit only when `stream` emits, as documented in the code. If you want updates from signal to also trigger an event, look into the `combineWith` operator.

Note: `withCurrentValueOf` and `sample` operators are also available on signals, not just streams.

If you want to get a Signal's current value without the complications of sampling, or even if you just want to make sure that a Signal is started, just call `observe` on it. That will add a noop observer to the signal, and return an `OwnedViewer` instance which being a `StrictSignal`, does expose `now()` and `tryNow()` methods that safely provide you with its current value.


### Relationship between EventStream and Signal

Signals and EventStreams are distinct concepts with different use cases as described above, but both are Observables.

You can `foldLeft(initialValue)(fn)` an EventStream into a Signal, or make a Signal directly with `stream.startWith(initialValue)`, or `stream.startWithNone` (which creates a "weak" signal, one that initially starts out with `None`, and has events wrapped in `Some`).

You can get an EventStream of changes from a Signal – `signal.changes` – this stream will re-emit whatever the parent signal emits (subject to laziness of the stream), minus the Signal's initial value.

If you have an observable, you can refine it to a Signal with `Observable#toWeakSignal` or `Observable#toSignalIfStream(ifStream = streamToSignal)`, and to a Stream with `Observable#toStreamOrSignal(ifSignal = signalToStream)`. For example, if you want to convert `Observable[String]` into `Signal[String]` with empty string as initial value in case this Observable is a stream, use `observable.toSignalIfStream(_.startWith(""))`.

See also: [Sources & Sinks](#sources--sinks)


### Observer

`Observer[A]` is a modest wrapper around an `onNext: A => Unit` callback that represents an _external_ observer (see sections above for the distinction with InternalObserver-s). Observers have no knowledge of which observables, if any, they're observing, they have no power to choose whether they want to observe an observable, etc.

Observers are intended to contain side effects, and to trigger evaluation of observables by their presence (remember, all Observables are lazy).

You usually create observers with `Observer.apply` or `myObservable.foreach`. There are a few more methods on Observer that support [error handling](#error-handling).

Observers have a few convenience methods:

`def contramap[B](project: B => A): Observer[B]` – This is useful for separation of concerns. For example your Ajax service might expose an `Observer[Request]`, but you don't want a simple `UserProfile` component to know about your Ajax implementation details (`Request`), so you can instead provide it with `requestObserver.contramap(makeUpdateRequest)` which is a `Observer[User]`.

`def filter(passes: A => Boolean): Observer[A]` – useful if you have an `Observable` that you need to observe while filtering out some events (there is no `Observable.filter` method, only `EventStream.filter`).

`def contramapSome` is just an easy way to get `Observer[A]` from `Observer[Option[A]]`

`def contracollect[B](pf: PartialFunction[B, A]): Observer[B]` – when you want to both `contramap` and `filter` at once.

`def contramapOpt[B](project: B => Option[A]): Observer[B]` – like `contracollect` but designed for APIs that return Options, such as `NonEmptyList.fromList`.

`delay(ms: Int)` – creates an observer that calls the original observer after the specified delay (for both events and errors)

`Observer.combine[A](observers: Observer[A])` creates an observer that triggers all of the observers provided to it. Unlike `Observer[A](nextValue => observers.foreach(_.onNext(nextValue)))`, the combined observer will also trigger its child observers in case of `.onError` (more about that in [Error Handling](#error-handling)).

### Ownership

Alright, this is it. By now you've read enough to have many questions about how ownership works. This assumes you've read all the docs above, but to recap the core problem that ownership solves:

* Adding an `Observer` to the lazily evaluated `Observable` is a leaky operation. That is, these resources will not be garbage collected even if the observable and the observer are both unreachable to _user_ code. This is because the observable's parent observables will keep an internal reference to it for as long as it has observers.
* Therefore, without Ownership you would have needed to remember to remove observers that you added when the observers are no longer needed.
* But doing that manually is insane, you will eventually forget and cause memory leaks and undesired behaviour. You should not need to take out your own garbage in a garbage collected language.

If any of the above does not make sense, the rest of this section might be confusing. Make sure you at least understand the entirety of the [Laziness](#laziness) section before proceeding.

Without further ado:

**Subscription** is a resource that must be killed in order to release memory or prevent some other leak. You can get it by calling `observable.addObserver(observer)`, `writeBus.addSource(stream)`, or other similar methods that take an implicit `owner` param.

Every Subscription has an **Owner**. An Owner is an object that keeps track of its subscriptions and knows when to kill them, and kills them _when it's time_ (determined in its sole discretion). Airstream does not offer any concrete Owner classes, just the base trait. Unless you use [Dynamic Ownership](#dynamic-ownership), you need to instantiate (and thus implement) your own Owner-s.

For example, until v0.8, my reactive UI library [Laminar](https://github.com/raquo/Laminar)'s ReactiveElement (an object representing a JS DOM Element) used to implement Owner. When a ReactiveElement was discarded (unmounted from the DOM), it would kill all of its `subscriptions`, i.e. all the Subscriptions that were bound to its lifetime. That would remove the observers that those subscriptions installed on the observables, stopping them if they have no other observers. Note: Laminar switched to [Dynamic Ownership](#dynamic-ownership) in v0.8 (more on that later).

When creating a Subscription, you can perform whatever leaky operations you wanted, and just provide the `cleanup` method to perform any required cleanup.

Subscriptions are bound to a specific Owner upon creation of the Subscription, and this link stays unchanged for the lifetime of the Subscription.

Subscriptions are normally killed _by their Owner_, but you can also `.kill()` the subscription manually. The Owner will be notified about this via `owner.onKilledExternally(subscription)` so that it can drop the reference to the killed `subscription` from its list.

Killing the same Subscription more than once throws an exception, don't do it.

Built-in Owner carefully tracks a list of its subscriptions, making sure to call the right hooks, and create and dispose the right references for memory management. If you extend Owner and change that logic, memory management of that owner and its subscriptions is on you. Generally you shouldn't need to mess with any of that logic though, just make sure to call `killSubscriptions` when it's time.


#### Ownership & Memory Management

In broad terms, ownership solves memory leaks by tying the lifecycle of Subscriptions which would be otherwise hard to track manually to the lifecycle of an Owner which is expected to be tracked automatically by a UI library like Laminar.

In practice, Airstream's memory management has no magic to it. It uses Javascript's standard garbage collection, same as the rest of your Scala.js code. You just need to understand what references what, and the documentation here explains it.

For example, a Subscription created by `observable.addObserver` method keeps references to both the Observable and the Observer (via the function passed as its `cleanup` param). That means that if you're keeping a reference to a Subscription, you're also keeping those references. Given that the Subscription has a `kill` method that lets you remove the observer from the observable, the presence of these references should be obvious. So like I said – no magic, you just need to internalize the basic ideas of lazy observables, just like you've already internalized the basic ideas of classes and functions.


#### OneTimeOwner

The basic `Owner` trait provides a high degree of flexibility, and therefore lacks some behaviour that you might expect in Owners.

For example, you can kill an `Owner` multiple times. Every time you do, its subscriptions will be killed, and the list of subscriptions cleared, but the `Owner` will remain usable after that, letting you add more subscriptions and kill them again later.

If you want an Owner that can only be killed once, and does not let you add subscriptions to it after it was killed, use the `OneTimeOwner` class instead. DynamicOwner below uses OneTimeOwner, and that is how Laminar provides element Owners that can not be used after the element is unmounted and its owner is killed.

If you try to create a subscription that uses a OneTimeOwner, the subscription will be killed immediately, and `OneTimeOwner`'s `onAccessAfterKilled` callback will be fired. You can throw in that callback, then subscription initialization will throw too.

Note that the subscription itself does not contain any activation logic (i.e. what needs to happen when subscription is activated), that user-provided logic is external to subscription initialization, and is typically run **before** the subscription is initialized, so before `OneTimeOwner` can prevent that from happening. So when try to use a dead OneTimeOwner, instead of completely ignoring the effective payload of the subscription, unless you take special measures, it will still execute, but the subscription will be cancelled and cleaned up immediately. But if the subscription's payload was to e.g. make a network request, you can't put that back in the bottle.

Bottom line, you should not be deliberately sending events to dead `OneTimeOwner`-s. They just fix what otherwise could be a memory leak, not completely prevent your code from running. They report the error so that you can fix your code that's doing this.


#### ManualOwner

The basic `Owner` trait also doesn't allow external code to kill it, because some owners are supposed to manage themselves. All you need to overcome that is expose the `killSubscriptions` method to the public, or just use the `ManualOwner` class that does this. 


#### Dynamic Ownership

Dynamic Ownership is not a replacement for standard Ownership described above. Rather, it is a self-contained feature built on top of regular Ownership. No APIs in Airstream itself require Dynamic Ownership, it is intended to be consumed by the user or by other libraries depending on Airstream.

The premise of Dynamic Ownership is the same as regular ownership: you can create DynamicSubscription-s owned by DynamicOwner, but here is what's different:

Regular Subscription-s can never recover from being `kill()`-ed, whereas DynamicSubscription-s can be activated and deactivated and then activated again and so on, as many times as their DynamicOwner wants. For example:

```scala
val stream: EventStream[Int] = ???
val observer: Observer[Int] = ???
 
val dynOwner = new DynamicOwner
 
val dynSub = DynamicSubscription(
  dynOwner,
  activate = (owner: Owner) => stream.addObserver(observer)(owner)
)
 
// Run dynSub's activate method and save the resulting non-dynamic Subscription
dynOwner.activate()
 
// Kill the regular Subscription that we saved 
dynOwner.deactivate()
 
// Run dynSub's activate method again, obtaining a new Subscription
dynOwner.activate()
 
// Kill the new Subscription that we saved 
dynOwner.deactivate()
```

Every time a `DynamicOwner` is `activate()`-d, it creates a new `OneTimeOwner`, and uses it to `activate` every `DynamicSubscription` that it owns. It saves the resulting non-dynamic `Subscription`, which the `DynamicOwner` later `kills()` when it's `deactivate()`-d.

Now you can see how this integrates with regular ownership. Anything that requires a non-dynamic `Owner` produces a `Subscription`. So to create a `DynamicSubscription` you need to provide an `activate` method that does this. That could be a call to `addObserver`, `addSource`, etc.

As a result, we have a dynamic owner that can add or remove `observer` from `stream` at any time. In Laminar starting with v0.8 every ReactiveElement has a `DynamicOwner`. When the element is mounted, that owner is activated, activating all dynamic subscriptions using a newly created non-dynamic owner. Then when the element is later unmounted, those subscriptions are deactivated, removing observers from observables, sources from event buses, etc.

Previously in Laminar v0.7 every ReactiveElement used to extend the non-dynamic `Owner`, so once it was unmounted, all the subscriptions were killed forever, so if the user re-mounted that element, its subscriptions would not have come back to life. But now that Laminar uses Dynamic Ownership, you can re-mount previously unmounted elements, and their dynamic subscriptions will spring back to life.

Note that a `DynamicSubscription` is not automatically activated upon creation. Its DynamicOwner controls its activation and deactivation. You can still permanently `kill()` a DynamicSubscription manually – it will be deactivated if it's currently active, and removed from its DynamicOwner.

##### Dynamic Ownership & Memory Management

I created Dynamic Ownership specifically to solve this long-standing Laminar [memory management issue](https://github.com/raquo/Laminar/issues/33): if a non-dynamic Subscription is created when ReactiveElement is initialized, and killed when that element is unmounted, what happens to elements that get initialized but are never mounted into the DOM? That's right, their subscriptions are never killed and so they are essentially never garbage collected.

Laminar v0.8 had to fix this by creating `Subscription`-s every time the element is mounted, and killing them when the element was unmounted. Long story short, Dynamic Ownership is exactly this, slightly generalized for wider use.

There is really nothing special in Dynamic Ownership memory management. It's just a helper to create and destroy subscriptions repeatedly. In practice DynamicSubscription's activate method generally contains the same references that `Subscription`'s cleanup method would, so it's all the same considerations as before.

##### Transferable Subscription

What, a helper for subscription helpers? Yes, indeed. This one behaves like a `DynamicSubscription` that lets you transfer it from one active `DynamicOwner` to another active `DynamicOwner` without deactivating and re-activating the subscription.

The API is simple:

```scala
class TransferableSubscription(
  activate: () => Unit,
  deactivate: () => Unit
) { 
  def setOwner(nextOwner: DynamicOwner): Unit
  def clearOwner(): Unit
}
```

Note that you don't get access to `Owner` in activate. This is the tradeoff required to achieve this flexibility safely. `TransferableSubscription` is useful in very specific cases when you only care about continuity of active ownership, such as when moving an element from one mounted parent to another mounted parent in Laminar (you wouldn't expect Unmount / Mount events to fire in this case).



### Sources of Events

We now understand how events propagate through streams and signals, but the events in Airstream have to _originate_ somewhere, right?


#### Creating Observables from Futures

`EventStream.fromFuture[A]` creates a stream that emits the value that the future completes with, when that happens.
* The event is emitted asynchronously relative to the future's completion
* Creating a stream from an already completed future results in a stream that emits no events (look into the source code of this method for an easy way to get different behaviour)

`Signal.fromFuture[A]` creates a Signal of `Option[A]` that emits the value that the future completes with, when that happens, wrapped in `Some()`.
* The initial value of this signal is equal to `Some(value)` if the future was already completed when the initial value was evaluated, or `None` otherwise.
* If the Signal was created from a not yet completed future, the completion event is emitted asynchronously relative to when the future completes, because that is how `future.onComplete` works.
* Unlike other signals, this signal keeps its current value from going stale even in the absence of observers
* Being a StrictSignal, this signal exposes `now` and `tryNow` methods that provide its current value. However, note that there might be asynchronous delay between the completion of the Future and this signal's current value updating, as explained above.

Note that all observables created from futures fire their events in a new transaction because they don't have a parent observable to be synchronous with.

Now that you have an `Observable[Future[A]]`, you can flatten it into `Observable[A]` in a few ways, see [Flattening Observables](#flattening-observables).

A failed future results in an error (see [Error Handling](#error-handling)).

#### `EventStream.fromJsPromise` and `Signal.fromJsPromise`

Super thin wrappers over `EventStream.fromFuture` and `Signal.fromFuture` (see above) for more convenient integration with JS libraries.

#### `EventStream.fromSeq`

```scala
object EventStream {
  def fromSeq[A](events: Seq[A]): EventStream[A] = ...
  ...
}
```

This method creates an event stream that synchronously emits events from the provided sequence one by one to any newly added observer.

Each event is emitted in a separate transaction, meaning that the propagation of the previous event will fully complete before the propagation of the new event starts.

**Note:** you should avoid using this factory, at least with multiple events. You generally shouldn't need to emit more than one event at the same time like this stream does. If you do, I think your model is likely abusing the concept of "event". This method is provided as a kludge until I can make a more confident determination.


#### `EventStream.fromValue`

Like `EventStream.fromSeq` (see right above), but only allows for a single event.


#### `EventStream.fromTry`

Like `EventStream.fromValue` (see right above), but also allows an error.

This is provided as a kludge until it becomes more clear that this is not needed.


#### `EventStream.periodic`

An event stream that emits events at an interval. `EventStream.periodic` emits the index of the event, starting with `0` for the initial event that's emitted without delay. If you want to skip the initial event, pass `emitInitial = false`. The `resetOnStop` option determines whether the index will be reset to `0` when the stream is stopped due to lack of observers. You can also reset the stream to any index manually by calling `resetTo(value)` on it. This will immediately emit this index if `emitInitial` is true, or emit the next index after a delay otherwise.

The underlying `PeriodicEventStream` class offers more functionality, including the ability to emit values other than index, set a custom interval for every subsequent event, and stop the stream while it still has observers.


#### `EventStream.empty`

A stream that never emits any events.


#### `EventStream.withCallback` and `withObserver`

`EventStream.withCallback[A]` Creates and returns a stream and an `A => Unit` callback that, when called, passes the input value to that stream. Of course, as streams are lazy, the stream will only emit if it has observers.

```scala
val (stream, callback) = EventStream.withCallback[Int]
callback(1) // nothing happens because stream has no observers
stream.foreach(println)
callback(2) // `2` will be printed  
```

`EventStream.withJsCallback[A]` works similarly except it returns a js.Function for easier integration with Javascript libraries.

`EventStream.withObserver[A]` works similarly but creates an observer, which among other conveniences passes the errors that it receives into the stream.


#### EventBus

`new EventBus[MyEvent]` is a more powerful way to create a stream on which you can manually trigger events. The resulting EventBus exposes two properties:

**`events`** is the stream of events emitted by the EventBus.

**`writer`** is a WriteBus object that lets you trigger EventBus events in a few ways.

WriteBus extends Observer, so you can call `onNext(newEventValue)` on it, or pass it as an observer to another stream's `addObserver` method. This will cause the event bus to emit `newEventValue` in a new transaction.

Or you can just call `eventBus.emit(newEvent)` for the same effect.

What sets EventBus apart from e.g. `EventStream.withObserver` is that you can also call `eventBus.addSource(otherStream)(owner)`, and the event bus will re-emit every event emitted by that stream. This is somewhat similar to adding `writer` as an observer to `otherStream`, except this will not cause `otherStream` to be started unless/until the EventBus's own stream is started (see [Laziness](#laziness)).

You've probably noticed that `addSource` takes `owner` as an implicit param – this is for memory management purposes. You would typically pass a WriteBus to a child component if you want the child to send any events to the parent. Thus, we want `addSource` to be automatically undone when said child is discarded (see [Ownership](#ownership)), even if `writer.stream` is still being observed.

An EventBus can have multiple sources simultaneously. In that case it will emit events from all of those sources in the order in which they come in. **EventBus always emits every event in a new [Transaction](#transactions).** Note that EventBus lets you create loops of Observables. It is up to you to make sure that a propagation of an event through such loops eventually terminates (via a proper `.filter(passes)` gate for example, or the implicit `==` equality filter in Signal).

You can manually remove a previously added source stream by calling `kill()` on the Subscription object returned by the addSource call.

EventBus is particularly useful to get a single stream of events from a dynamic list of child components. You basically pass down the `writer` to every child component, and inside the child component you can add a source stream to it, or add the `writer` as an observer to some stream. Then when any given child component is discarded (i.e. its owner kills its subscriptions), its connection to the event bus will also be severed.

Typically you don't pass EventBus itself down to child components as it provides both read and write access. Instead, you pass down either the writer or the event stream, depending on what is needed. This separation of concerns is the reason why EventBus doesn't just extend WriteBus and EventStream, by the way.

WriteBus comes with a few ways to create new writers. Consider this:

```scala
val eventBus = new EventBus[Foo]
val barWriter: WriteBus[Bar] = eventBus.writer
  .filterWriter(isGoodFoo)
  .contramapWriter(barToFoo)
```

Now you can send `Bar` events to `barWriter`, and they will appear in `eventBus` processed with `barToFoo` then and filtered by `isGoodFoo`. This is useful when you want to get events from a child component, but the child component does not or should not know what `Foo` is. Generally if you don't need such separation of concerns, you can just `map`/`filter` the stream that's feeding the EventBus instead.

WriteBus also offers a powerful `contracomposeWriter` method, which is like `contramapWriter` but with `compose` rather than `map` as the underlying transformation.

##### Batch EventBus Updates

EventBus emits every event in a new transaction. However, similar to Var [batch updates](#batch-updates) you can call `EventBus.emit` or `EventBus.emitTry` to send values into several EventBus-es simultaneously, within the same transaction, to avoid [glitches](#frp-glitches) downstream.

```scala
val valuesEventBus = new EventBus[Int]
val labelsEventBus = new EventBus[String]
 
EventBus.emit(
  valuesEventBus -> 100,
  labelsEventBus -> "users"
)
```

Similar to Vars, you can't emit more than one event into the same EventBus in the same transaction. Airstream will throw if you attempt to do this, so you can't have duplicate inputs like `EventBus.emit(bus1 -> ev1, bus1 -> ev2, bus2 -> ev3)`. If you need to emit more than one event into the same EventBus, just call the method twice, and they will be sent in separate transactions.


#### Var

Var is a reactive variable that you can update manually, and that exposes its current value at all times, as well as a `.signal` of its current value.

Creating a Var is straightforward: `Var(initialValue)`, `Var.fromTry(tryValue)`.

##### Simple Updates

You can update a Var using one of its methods: `set(value)`, `setTry(Try(value))`, `update(currentValue => nextValue)`, `tryUpdate(currentValueTry => Try(nextValue))`. Note that `update` will send a VarError into unhandled if the Var's current value is an error. Use `set*` or `tryUpdate` methods to update failed Vars.

##### Observers Feeding into Var

Every Var provides a `writer` which is an Observer that writes input values into the Var. It may be useful to provide your code with write-only access to a Var, or to a subset of the data in the Var by means of the Observer's `contramap` method.

In addition to `writer`, Var also offers `updater`s, making it easy to create an Observer that updates the Var based on both the Observer's input value and the Var's current value:

```scala
val v = Var(List(1, 2, 3))
val adder = v.updater[Int]((currValue, nextInput) => currValue :+ nextInput)

adder.onNext(4)
v.now() // List(1, 2, 3, 4)

val inputStream: EventStream[Int] = ???

inputStream.foreach(adder)
inputStream --> adder // Laminar syntax
```

`updater` will send a VarError into unhandled if you ask it to update a Var that is in a failed state. In such cases, use `writer` or `tryUpdater` instead.

Vars of Options, i.e. `Var[Option[A]]`, also offer `someWriter: Observer[A]` for convenience.

##### Reading Values from a Var

You can get the Var's current value using `now()` and `tryNow()`. `now` throws if the current value is an error. Var also exposes a `signal` of its values.

SourceVar, i.e. any Var that you create with `Var(...)`, follows **strict** (not lazy) execution – it will update its current value as instructed even if its signal has no observers. Unlike most other signals, the Var's signal is also strict – its current value matches the Var's current value at all times regardless of whether it has observers. Of course, any downstream observables that depend on the Var's signal are still lazy as usual.

Being a `StrictSignal`, the signal also exposes `now` and `tryNow` methods, so if you need to provide your code with read-only access to a Var, sharing only its signal is the way to go.

##### Var Transaction Delay

Var emits every event in a new [transaction](#transactions). This has important ramifications when writing to and reading from a Var. Consider the following code:

```scala
val myVar = Var(0)

println("Start")

myVar.set(1)
println(s"After set: ${myVar.now()}")

myVar.update(_ + 1)
println(s"After update: ${myVar.now()}")

new Transaction { _ =>
  println(s"After trx: ${myVar.now()}")
}

println("Done")
```

If you put this code in your app's `main` method or inside a `setTimeout` callback, it will print:

```
Start
After set: 1
After update: 2
After trx: 2
Done
```

But if you try to run this same code _while another transaction is being executed_, for example inside one of your observers, in response to an incoming stream event, this is what will be printed:

```
Start
Done
After set: 0
After update: 0
After trx: 2
```

Why the difference? Var's current value exposed by `now()` only updates when the Var emits the updated value, and as we now know, this always happens in a new transaction. But we ran our code inside an observer, that is, while another transaction was running. And the new transaction will only run after the current transaction has finished propagating, so the Var' current value will not update until then.

This is why reading `myVar.now()` after calling `myVar.set(1)` gives you a stale value in this case. Var tries very hard to do the right thing though. While you can't expect to see the new value in `now()`, the `update` method does provide the updated value, `1`, as the input to its callback. This is because the update callback is also scheduled for a new transaction, and so it is executed after the transaction in which the Var's value was set to `1` has finished propagating.

Finally, in both cases the code prints "After trx: 2". This is because that println is only executed in a new transaction. Similar to the update callback, this only gets run when the previously scheduled transactions have finished propagating, so it will always see the final Var value.

So there you have it, you have two ways to read the Var's **new** current value: either call `now()` inside a new transaction, or use `update`. And of course you can also listen to the Var's signal.

Keep in mind that transaction scheduling is fully synchronous, we do not introduce an asynchronous delay anywhere, we merely order the execution chunks to make the maximum amount of sense possible. Read more about transaction scheduling in the [Transactions](#transactions) section.

##### Derived Vars

If you have a `Var[A]`, you can get zoomed / derived `Var[B]` by providing `A => B`, `B => A`, and an owner. The result is a `DerivedVar`, essentially a combination of `var.signal.map` and `writer.contramap` packaged in a Var, and so to simulate the strictness of Var, creating DerivedVar requires an owner.

When you have a derived var, updates to it are propagated to the source var and vice versa, as long as derived var's signal has listeners. Don't try to set/update a derived Var's value when it has no listeners. The value will not be updated, and Airstream will emit an unhandled error.

```scala
val owner: Owner = ???
val source = Var(1)
val derived = source.zoom(_ + 100)(_ - 100)(owner)

source.set(2) // source.now() == 2, derived.now() == 102

derived.set(103) // source.now() == 3, derived.now() == 103

owner.killSubscriptions()

source.set(3) // derived var did not update: derived.now() == 102

derived.set(104) // neither var updated: source.now() == 3, derived.now() == 103
```

Note: DerivedVar starts out with a subscription owned by `owner`, that counts as a listener of course. However, just like `OwnedSignal` in general, if it obtains any other listeners, it will continue running even if the original owner kills its subscription.

##### Batch Updates

Similar to EventBus, Var emits each event in a new [transaction](#transactions). However, similar to `EventBus.emit`, you can put values into multiple Vars "at the same time", in the same transaction, to avoid [glitches](#frp-glitches) downstream. To do that, use the `set` / `setTry` / `update` / `tryUpdate` methods on the Var **companion object**. For example:

```scala
val value = Var(1)
val isEven = Var(false)

val sumSignal = x.signal.combineWith(y.signal)

// batch updates!
Var.set(x -> 2, y -> true)
```

With such a batched update, `sumSignal` will only emit `(1, false)` and `(2, true)`. It will **not** emit an inconsistent value like `(1, true)` or `(2, false)`.

Batch updates are also atomic in the following ways:
* `update` and `tryUpdate` will only execute the provided mods when the transaction is actually executed, not immediately as it's scheduled. This ensures that the mods operate on the latest available Var state.
* Similar to `Var#update`, `Var.update` throws if you try to apply `mod` to a failed Var. In the batch case _none_ of the input Vars will be updated, although _some_ of the mod functions will be executed. For this reason, mod functions should be pure of side effects. Use `tryUpdate`when you need more control over error handling.
* Similar to `Var#tryUpdate`, `Var.tryUpdate` throws if any of the provided mods throw. None of the Vars will update in this case. You should return Failure() from your mod instead of throwing if this is not what you want.

Also, since an Airstream observable can't emit more than once per transaction, the inputs to batch Var methods must have no duplicate vars. For example, you can't do this: `Var.set(var1 -> 1, var1 -> 2, var2 -> 3)`. Airstream will detect that you're attempting to put two events into `var1` in the same transaction, and will throw. Use two separate calls if you want to send two updates into the same Var.  

Keep in mind that derived vars count as the underlying source vars for duplicate detection purposes, so you can't update vars `var1` and `var1.zoom(fa)(fb)` in the same transaction.

Those are the only ways in which setting / updating a Var can throw an error. If any of those happen when batch-updating Var values, Airstream will throw an error, and all of the involved Vars will fail to update, keeping their current value.

Remember that this atomicity guarantee only applies to failures which would have caused an individual `update` / `tryUpdate` call to throw. For example, if the `mod` function provided to `update` throws, `update` will not throw, it will instead successfully set that Var `Failure(err)`.


#### Val

`Val(value)` / `Val.fromTry(tryValue)` is a Signal "constant" – a Signal that never changes its value. Unlike other Signals, its value is evaluated immediately upon creation, and is exposed in public `now()` and `tryNow()` methods.

Val is useful when a component wants to accept either a Signal or a constant value as input. You can just wrap your constant in a Val, and make the component accept a `Signal` (or a `StrictSignal`) instead.


#### Ajax

Airstream now has a built-in way to perform Ajax requests:

```scala
AjaxEventStream
  .get("/api/kittens") // EventStream[dom.XMLHttpRequest]
  .map(req => req.responseText) // EventStream[String]
```

Methods for POST, PUT, PATCH, and DELETE are also available.

The request is made every time the stream is started. If the stream is stopped while the request is pending, the old request will not be cancelled, but its result will be discarded.

If the request times out, is aborted, returns an HTTP status code that isn't 2xx or 304, or fails in any other way, the stream will emit an `AjaxStreamError`.

If you want a stream that never fails, a stream that emits an event regardless of all those errors, call `.completeEvents` on your ajax stream.

You can listen for `progress` or `readyStateChange` events by passing in the corresponding observers to `AjaxEventStream.get` et al, for example:

```scala
val (progressObserver, $progress) = EventStream.withObserver[(dom.XMLHttpRequest, dom.ProgressEvent)]

val $request = AjaxEventStream.get(
  url = "/api/kittens",
  progressObserver = progressObserver
)

val $bytesLoaded = $progress.mapN((xhr, ev) => ev.loaded)
```

In a similar manner, you can pass a `requestObserver` that will be called with the newly created `dom.XMLHttpRequest` just before the request is sent. This way you can save the pending request into a Var and e.g. `abort()` it if needed.

Warning: dom.XmlHttpRequest is an ugly, imperative JS construct. We set event callbacks for `onload`, `onerror`, `onabort`, `ontimeout`, and if requested, also for `onprogress` and `onreadystatechange`. Make sure you don't override Airstream's listeners in your own code, or this stream will not work properly.


#### Websockets

Airstream has no official websockets integration yet.

For several users' implementations, search the [Laminar gitter room](https://gitter.im/Laminar_/Lobby), and the issues in this repo.


#### DOM Events

```scala
val element: dom.Element = ???
DomEventStream[dom.MouseEvent](element, "click") // EventStream[dom.MouseEvent]
```

This stream, when started, registers a `click` event listener on `element`, and emits all events the listener receives until the stream is stopped, at which point the listener is removed.

Airstream does not know the names & types of DOM events, so you need to manually specify both. You can get those manually from MDN or programmatically from event props such as `onClick` available in Laminar. 

`DomEventStream` works not just on elements but on any `dom.raw.EventTarget`. However, make sure to check browser compatibility for fancy EventTarget-s such as XMLHttpRequest.



### Custom Event Sources

If simpler event sources (see above) do not suit your needs, consider using `CustomSource`. This mechanism lets you create a custom stream or signal as long as it does not depend on other Airstream observables. So. it's good for integrating with third party sources of events.

You can create custom event sources using `EventStream.fromCustomSource` and `Signal.fromCustomSource`, which are convenience wrappers over the underlying  `CustomEventSource` and `CustomSignalSource` classes. This section will explain how to use those underlying classes, and after that the understanding of `fromCustomSource` methods should come naturally.

Airstream's `DomEventStream.apply` creates a stream of events by wrapping the DOM API into `CustomStreamSource`. Let's see how it works:

```scala
def apply[Ev <: dom.Event](
  eventTarget: dom.EventTarget,
  eventKey: String,
  useCapture: Boolean = false
): EventStream[Ev] = {

  CustomStreamSource[Ev]( (fireValue, fireError, getStartIndex, getIsStarted) => {

    val eventHandler: js.Function1[Ev, Unit] = fireValue

    CustomSource.Config(
      onStart = () => {
        eventTarget.addEventListener(eventKey, eventHandler, useCapture)
      },
      onStop = () => {
        eventTarget.removeEventListener(eventKey, eventHandler, useCapture)
      }
    )
  })
}
```

When we create a `CustomStreamSource`, we need to provide a callback that accepts some useful arguments and returns an instance of `CustomSource.Config`, which is essentially a bundle of two callbacks: `onStart` which fires when your stream is started, and `onStop` which fires when your stream is stopped (see [Laziness](#laziness)).

Here we see that DomEventStream registers `fireValue` as an event listener on the DOM element when the stream starts, and unregisters that listener when the stream stops. This way the resulting stream will properly clean up its resources.

Side note: `val eventHandler` is cached to avoid implicitly creating a new instance of `js.Function1`. We need to keep this exact reference to be able to unregister the listener. Just a bit of Scala-vs-js friction here.

Let's look at the methods that `CustomStreamSource` makes available to us:

* **fireValue** - call this with a value to make the custom stream emit that value in a new transaction
* **fireError** - call this with a Throwable to make the custom stream emit an error (see [Error Handling](#error-handling))
* **getStartIndex** – call this to check how many times the custom stream has been started. Airstream uses this for the `emitOnce` param in streams like `EventStream.fromSeq`.
* **getIsStarted** – call this to check if the custom stream is currently started 

`CustomSource.Config` instances have a `when(passes: () => Boolean)` method that returns a config that, when the predicate does not pass, will **not** call your `onStart` callback when the stream starts, and will not call your `onStop` callback when the stream is subsequently stopped (we assume that your `onStop` code cleans up after your `onStart` code). To clarify, the predicate is evaluated when the custom stream is about to start. And the stream **will* actually start – you can't break this part of Airstream contract – the predicate only controls whether your callbacks defined in the config will be run. You can see this predicate being useful to implement the `emitOnce` param in streams like `EventStream.fromSeq`.

**CustomSignalSource** is the Signal version of CustomStreamSource, and works similarly, just with a slightly different set of params:

```scala
class CustomSignalSource[A] (
  getInitialValue: => Try[A],
  makeConfig: (SetCurrentValue[A], GetCurrentValue[A], GetStartIndex, GetIsStarted) => CustomSource.Config
)
```

`fireValue` and `fireError` are merged into one `setCurrentValue` callback that expects a `Try[A]`, and this being a Signal, we also provide a `getCurrentValue` param to check the custom signal's current value.

Generally signals need to be started in order for their current value to update. Stopped signals generally don't update without listeners, unless they are a `StrictSignal` like `Var#signal`. `CustomSignalSource` is not a `StrictSignal` so there is no expectation for it to keep updating its value when it's stopped. Users should keep listening to signals that they care about.


#### Extending Observables

If you need a custom observable that depends on another Airstream observable, you can subclass `WritableEventStream` or `WritableSignal`. See existing classes for inspiration, such as `MapSignal` and `MapEventStream`.

You will likely want to mix in the `SingleParentObservable` trait and either `InternalTryObserver` (for signals) or `InternalNextErrorObserver` (for streams). Then you will just need to implement `onTry` (for signals) or `onNext` / `onError` (for streams) methods, which will be triggered when the parent observable emits. In turn, those methods should call `fireValue`, `fireError` or `fireTry` to make your custom observable emit its own value. Also, for signals you will need to implement `initialValue` which you should derive from the parent observable's **current** value (NOT from the parent observable's `initialValue`).

If you want to put asynchronous logic in your observable, make sure to have a good understanding of Airstream transactions and topoRank, and consult with other asynchronous observables implementations such as `DelayEventStream`.

If your custom observable does not depend on any Airstream observables, e.g. if you're writing a compatibility layer for a third party library, you generally should be able to use the simpler [Custom Sources](#custom-event-sources) API.


##### Accessing protected members

Some values and methods that you might want to access on observables are `protected`. That means that the compiler will only let you access those values and methods on the same instance. So, you can read `this.topoRank`, but you can't read `parentObservable.topoRank`. To get around this, use the `Protected` object: `Protected.topoRank(parentObservable)`.

Aside from `topoRank`, you will need to access `tryNow()` and `now()` this way, e.g. when implementing a custom signal's `initialValue`. These methods require an implicit evidence of type `Protected`, which is automatically in scope if you're calling these methods from inside your custom observable. You're not supposed to access a signal's current value from the outside, without proving that the signal is running (e.g. by subscribing to it), otherwise you might get a stale value.

Honestly all this "protected" business smells funny to me, but I couldn't figure out a better way to allow third party extensions without making these protected members public.



### Sources & Sinks

A `Source[A]` in Airstream is something that exposes a `toObservable` method, something that can be (explicitly, not implicitly) converted into an `Observable[A]`. For example, the observables themselves are Sources, but so are EventBus-es (`def toObservable = this.events`) and Var-s (`def toObservable = this.signal`).

Source is further subtyped into – `EventSource` (EventStream, EventBus) and `SignalSource` (Signal, Var). Predictably, `eventSource.toObservable` returns an EventStream, whereas `signalSource.toObservable` returns a Signal.

These types are useful when you want to create a method that can accept "anything that you can get a stream from", including not just observables but things like event buses and futures. For example, it's used in Laminar:

```scala
val textBus = new EventBus[String]
val textFuture = Future.successful("")
val elementFuture = Future.successful(span(""))
div(value <-- textBus.events)
div(value <-- textBus) // Also works because this <-- accepts Source[String]
div(value <-- textFuture) // Works via Future[A] => EventSource[A] implicit conversion
div(child.maybe <-- elementFuture) // Works via Future[A] => SignalSource[Option[A]] implicit
```

The counterparty to `Source` in Airstream is `Sink`. `Sink[A]` is something that exposes a `toObserver` method that can be explicitly (not implicitly) convert a Sink to an Observer. So Observers are sinks, as are EventBus-es and Var-s, and even `js.Function1[A, Unit]` has an implicit conversion to `Sink[A]`.

However, there is no implicit conversion from `A => Unit` to `Sink` because unfortunately  Scala requires a lambda's type param to have a type ascription to implicitly convert it into a Sink[A], so syntax like `div(value <-- (_ => println("x"))` would not be possible with such an implicit defined. In Laminar we get around this by overloading the `<--` method to accept either a `Sink[A]` or `A => Unit`. If you need this conversion, just wrap your function in `Observer`. You'll still need to ascribe the types though.

Speaking of implicits, why don't we have EventBus extend both `EventStream[A]` and `Observer[A]` instead of having separate Source and Sink types? On a technical level simply because Observable and Observer have overlapping methods defined, such as `filter` and `delay`, but more importantly, it would just be confusing. The whole point of Source and Sink is to not expose any methods other than toObservable, so that these types are only used as input types to methods that the developer wants to be flexible.



### FRP Glitches

#### Other Libraries

A glitch in Functional Reactive Programming is a situation where inconsistent state is allowed to exist and exposed to either an observable or an observer. For example, consider the typical diamond case:

```scala
val numbers: EventStream[Int] = ???
val isPositive: EventStream[Boolean] = numbers.map(_ > 0)
val doubledNumbers: EventStream[Int] = numbers.map(_ * 2)
val combinedStream: EventStream[(Int, Boolean)] = doubledNumbers.combineWith(isPositive)
combinedStream.addObserver(combinedStreamObserver)(owner)
```

Now, without thinking too hard, what do you think `combinedStream` will emit when `numbers` emits `1`, assuming `-1` was previously emitted? You might expect that `isPositive` would emit `true`, `doubledNumbers` would emit `2`, and then combinedStream would emit a tuple `(2, true)`. That would make sense, and this is how Airstream works at no cost to you, and yet this is not how most streaming and state propagation libraries behave.

Most streaming libraries will introduce a **glitch** in this scenario, as they are implemented with unconditional depth-first propagation. So in other libraries when the event from `numbers` (`1`) propagates, it goes to `isPositive` (`true`), then to `combinedStream` (`(-1, true)`). And that's a glitch. `(-1, true)` is not a valid state, as -1 is not a positive number. Immediately afterwards, `doubledNumbers` will emit `2`, and finally combinedStream would emit `(2, true)`, the correct event.

Such behaviour is problematic in a few ways – first, you are now propagating two events on equal standing. Any observables (and in most other libraries, even observers!) downstream of `combinedStream` will see two events come in, the first one carrying invalid/incorrect state, and they will probably perform incorrect calculations or side effects because of that.

In general, glitches happen when you have an observable that _synchronously depends_ on multiple observables that _synchronously depend_ on a common ancestor or one of themselves. I'm using the term `synchronously depends` to describe a situation where emitting an event to a parent observable might result in the child observable also emitting it – synchronously. So `map` and `filter` would fall into this category, but `delay` wouldn't. 


#### Topological Rank

In the diamond-combine case described above Airstream avoids a glitch because CombineObservable-s (those created using the `combineWith` method) do not propagate downstream immediately. Instead, they are put into a `pendingObservables` queue in the current Transaction (we'll get to those soon). When the rest of the propagation within a transaction finishes, the propagation of the first pending observable is resumed. When that is finished, we propagate the first remaining pending observable, and so on.

So in our example, what happens in Airstream: after `isPositive` emits `true`, `combinedStream` is notified that one of its parents emitted a new event. Instead of emitting its own event, it adds itself to the list of pending observables. Then, as the `isPositive` branch finished propagating (for now), `doubledNumbers` emits `2`, and then again notifies `combinedStream` about this. `combinedStream` is already pending, so it just grabs and remembers the new value from this parent. At this point the propagation of `numbers` is complete (assuming no other branches exist), and Airstream checks `pendingObservables`on the current transaction. It finds only one – `combinedStream`, and re-starts the propagation from there. The only thing left to do in our example is to send the new event – `(2, true)` to `combinedStreamObserver`.

Now, only this simple example could work with such logic. The important bit that makes this work for complex observable graphs is [topological rank](https://en.wikipedia.org/wiki/Topological_sorting). Topological rank in Airstream is defined as follows: if observable A _synchronously depends_ (see definition above) on observable B, its topological rank will be greater than that of B. In practical terms, `doubledNumbers.topoRank = numbers.topoRank + 1` and `combinedStream.topoRank == max(isPositive.topoRank, doubledNumbers.topoRank) + 1`.

In case of `combineWith`, Airstream uses topological rank for one thing – do determine which of the pending observables to resolve first. So when I said that Airstream continues the propagation of the "first" pending observable, I meant the one with the lowest `topoRank` among pending observables. This ensures that if you have more than one combined observable pending, that the one that doesn't depend on the other one will be propagated first.

So this is how Airstream avoids the glitch in the diamond-combine case.


#### Transactions

Before we dive into other kinds of glitches (ha! you thought that was it!?), we need to know what a Transaction is.

Philosophically, a Transaction in Airstream encapsulates a part of the propagation that 1) happens **synchronously**, and 2) contains **no loops** of observables. Within the confines of a single Transaction Airstream guarantees a) **no glitches**, and b) that no observable will emit more than once.

Async streams such as `stream.delay(500)` emit their events in a new transaction because Airstream executes transactions sequentially – and there is no sense in keeping other transactions blocked until some Promise or Future decides to resolve itself.

Events that come from outside of Airstream – see [Sources of Events](#sources-of-events) – each come in a new Transaction, and those source observables have a `topoRank` of 1. I guess it makes sense why `EventStream.periodic` would behave that way, but why wouldn't `EventBus` reuse the transaction of whatever event came in from one of its source streams?

And the answer is the limitation of our topological ranking approach: it does not work for loops of observables. A topoRank is a property of an observable, not of the event coming in. And an observable's topoRank is static, determined at its creation. EventBus on its creation has no sources, and allows you to fire events into it manually, so its stream needs to emit all those events in a new Transaction because there is no way to guarantee correct topological ranking to avoid glitches.

That said, in practice this is not a big deal because the events that an EventBus receives from different sources should be usually independent of each other because they are coming from different child components or from different browser events.

Apart from EventBus there is another way to create a loop – the `eventStream.flatten` method. And that one too, produces an event stream that emits all events in a new transaction, for all the same reasons.

Loops and potentially-loopy constructs necessarily require a new transaction as a tradeoff. Some other libraries do some kinds of dynamic topological sorting which is less predictable and whose performance worsens as your observables graph gets more complicated, but with Airstream there are no such costs. The only – and tiny – cost is when Airstream inserts a CombineObservable into the list of pending observables – that list is sorted by a static `topoRank` field, so it takes O(n) where n is the number of currently pending observables, which is usually zero or not much more than that.

Lastly, keep in mind that emitting events inside Observer-s will necessarily happen in a new transaction as you will need to use EventBus / Var APIs that create new transactions. Observers are generally intended for side effects. Those effects might be emitting other events, but in that case we consider them independent events, not a continuation of the current transaction. Philosophically, Observers should not know what they're observing (and they can observe multiple things at a time).


#### Merge Glitch-By-Design

Consider this:

```scala
val numbers: EventStream[Int] = ???
val tens: EventStream[Boolean] = numbers.map(_ * 10)
val hundreds: EventStream[Int] = tens.map(_ * 10)
val multiples: EventStream[Int] = EventStream.merge(tens, hundreds)
multiples.addObserver(multiplesObserver)(owner)
```

Same deal – what do you expect `multiples` to emit when `numbers` emits 1? I admit, it's a loaded question because of how I named `multiples`. I expect it to emit 10, and then 100. 10 comes first not because magic, but because the stream `hundreds` _synchronously depends_ on `tens`. Or, more precisely, because its `topoRank` is higher. This behaviour is especially desirable when your events are effectively operations – you don't want the merged stream to swallow operations or to put them in the wrong order.

TODO[API] Consider ordering synchronous events by the order their streams are given to merge instead of topoRank.

In Airstream, MergeEventStream will not emit more than one event in the same Transaction, because a Transaction by its very definition is about propagating a single event (that it happens to sometimes be split into multiple branches e.g. `tens` and `hundreds` is irrelevant, it's still the same change propagating), whereas a merged stream is capable of creating new events out of thin air as shown here. So in this example `multiples` will emit 10 in the same transaction that `numbers` emitted 1 in, and it will then create a new transaction which will emit 100 when it gets its turn.

MergeEventStream uses the same pendingObservables mechanism as CombineObservable because both extend SyncObservable.


#### Scheduling of Transactions

When you call methods like `Var#set`, `EventBus.emit`, etc. we create a new transaction. If another transaction is currently executing, which is often the case (e.g. if you're doing this inside a `stream.foreach` callback), this transaction will not be executed immediately, but will be scheduled to be executed later, because to avoid glitches, the current transaction needs to finish first before any other transaction can put more events onto the observable graph.

So if you set a Var's value, you will not be able to read it in the same transaction, because this instruction will only be executed after the current transaction finishes:

```scala
val logVar: Var[List[Event]] = ???
stream.foreach { ev =>
  logVar.set(logVar.now() :+ ev)
  logVar.set(logVar.now() :+ ev)
  println(logVar.now())
  // NONE of the logVar.now() calls here will contain any `ev`
  // because they are all executed before the .set transaction executes.
  // Because of this, after all of the transactions are executed,
  // logVar will only contain one instance of `ev`, not two.
}
```

If you need to read of Var after writing to it, you can use `Var#update`, which will evaluate its mod only when its transaction runs, so it will always look at the freshest state of the Var:

```scala
val logVar = Var(List[Event]())
stream.foreach { ev =>
  logVar.update(_ :+ ev)
  logVar.update(_ :+ ev)
  // After both transactions execute, logVar will have two `ev`-s in it
}
```

Let's expand our example above:

```scala
val bus = new EventBus[Event]
val logVar = Var(List[Event]())
val countVar = Var(0)
bus.events.foreach { ev =>
  logVar.update(_ :+ ev)
  logVar.update(_ :+ ev)
  // After both transactions execute, logVar will have two `ev`-s in it
}
logVar.signal.foreach { log =>
  sideEffect(log.size)
  countVar.update(_ += 1)
}
```

Let's say you fires an event into `bus`, and its transaction A started executing. The callback provided to `bus.events.foreach` will schedule two transactions to update `logVar`, B and C. After that, transaction A will finish as there are no other listeners.

Transaction B will immediately start executing. `ev` will be appended to `logVar` state, then this new state will be propagated to `logVar.signal`. `sideEffect(1)` will be called, and another transaction D to update `countVar` will be scheduled. After that, transaction B will finish as there are no other listeners.

Now, which transaction will execute next, C (the second update to `logVar`), or D (update to `countVar`)? Since Airstream v0.11.0, D will execute next, because its considered to be a child of the transaction B that just finished, **because it was scheduled while transaction B was running**. After a transaction finishes, Airstream first executes any pending transactions that were scheduled while it was running, in the order in which they were scheduled. This is recursive, so effectively we iterate over an hierarchy of transactions in a depth-first search.

In practice, this makes sense: in the code, the first `logVar.update(_ :+ ev)` is seen before the second `logVar.update(_ :+ ev)`, so the first transaction will completely finish, including any descendant transactions it creates, before we hand over control to its sibling transaction.

Remember that all of this happens synchronously. There can be no async boundaries within a transaction. Any event fires after an async delay is necessarily fired in a new transaction that is initialized / scheduled **after** the async delay, so it's not part of the pending transaction queue until the async delay resolves, and when it does, it's guaranteed that there are no pending transactions in the queue as Javascript is single-threaded.



### Operators

Airstream offers standard observables operators like `map` / `filter` / `compose` / `combineWith` etc. You will need to read the [API doc](https://javadoc.io/doc/com.raquo/airstream_sjs1_2.13/latest/com/raquo/airstream/index.html) or the actual code or use IDE autocompletion to discover those that aren't documented here or in other section of the Documentation. In the code, see `BaseObservable`, `Observable`, `EventStream`, and `Signal` traits and their companion objects.

Some of the more interesting / non-standard operators are documented below:


#### N-arity Operators

Airstream offers several methods and operators that work on up to 9 observables or tuples up to Tuple9:

**mapN((a, b, ...) => ???)**

Available on observables of `(A, B, ...)` tuples

**filterN((a, b, ...) => ???)**

Available on observables of `(A, B, ...)` tuples

**observableA.combineWith(observableB, observableC, ...)**

There is a bit of magic to this method for convenience. `streamOfA.combineWith(streamOfB)` returns a stream of `(A, B)` tuples only if neither A nor B are tuple types. Otherwise, `combineWith` flattens the tuple types, so for example both `streamOfA.combineWith(streamOfB).combineWith(streamOfC)` and `streamOfA.combineWith(streamOfB, streamOfC)` return a stream of `(A, B, C)`, **not** `((A, B), C)`. We achieve this using implicit `Composition` instances provided by the [tuplez](https://github.com/tulz-app/tuplez#composition) library.

**observableA.combineWithFn(observableB, ...)((a, b, ...) => ???)** 

Similar to `combineWith`, but you get to provide the combinator instead of relying on tuples. For example: `streamOfX.combineWithFn(streamOfY)(Point)` where Point is `case class Point(x: Int, y: Int)`.

**EventStream.combine(streamA, streamB, ...)** et al.

N-arity `combine` and `combineWithFn` methods are also available on EventStream and Signal companion objects.

**observableA.withCurrentValueOf(signalB, signalC, ...)**

Same auto-flattening of tuples as `combineWith`.

**observable.sample(signalA, signalB, ...)**

Returns an observable of `(A, B, ...)` tuples


#### Compose Changes

Some operators are available only on Event Streams, not Signals. This is by design. For example, `filter` is not applicable to Signals because a Signal can't exist without a current value, so `signal.filter(_ => false)` would not make any sense. Similarly, you can't `delay(ms)` a signal because you can't delay its initial value.

However, you can still use those operators with Signals, you just need to be explicit that you're applying them only to the Signal's changes, not to the initial value of the Signal:

```scala
val signal: Signal[Int] = ???
val delayedSignal = signal.composeChanges(changes => changes.delay(1000)) // all updates delayed by one second
val filteredSignal = signal.composeChanges(_.filter(_ % 2 == 0)) // only allows changes with even numbers (initial value can still be odd)
```

For more advanced transformations, `composeAll` operator lets you transform the Signal's initial value as well.


#### Sync Delay

Suppose you have two streams that emit in the same [Transaction](#transactions). Generally you don't know in which order they will emit, unless one of them depends on the other.

If this matters to you, you can use `delaySync` operator to establish the desired order:

```scala
val stream1: EventStream[Int] = ???
val stream2: EventStream[Int] = ???
 
val stream1synced = stream1.delaySync(after = stream2)
```

`stream1synced` synchronously re-emits all values that `stream1` feeds into it. Its only guarantee is that if `stream1` and `stream2` emit in the same transaction, `stream1synced` will emit AFTER `stream2` (assuming it has observers of course, or it won't emit at all, as usual). Otherwise, `stream2` does not affect `stream1synced` in any way. Don't confuse this with the `sample` operator.

Note: `delaySync` is better than a simple `delay` because it does not introduce an asynchronous boundary. `delaySync` does not use a `setTimeout` under the hood. In Airstream terms, `stream1synced` _synchronously depends_ on `stream1`, so all events in `stream1synced` fire in the same transaction as `stream1`, which is not the case with `stream1.delay(1000)` – those events would fire in a separate Transaction, and at an async delay.

Under the hood `delaySync` uses the same `pendingObservables` machinery as `combinedWith` operator – see [Topological Rank](#topological-rank) docs for an explanation. 


#### Splitting Observables

Airstream offers powerful `split` and `splitToSignals` operators that split an observable of `M[Input]` into an observable of `M[Output]` based on `Input => Key`. The functionality of these operators is very generic, so we will explore its properties by diving into concrete examples.

Note: These operators are available on qualifying streams and signals by means of `SplittableSignal` and `SplittableEventStream` value classes.

##### Example 0: Tests

This operator is particularly hard to put into words, at least on my first try. You might want to read the `split signal into signals` test in `SplitEventStreamSpec.scala`

And hey, don't be a stranger, remember we have [Discord](https://discord.gg/JTrUxhq7sj) for chat.

##### Example 1: Latest Version of Foo by Id

_If you are familiar with Laminar, consider skipping to the second example_

Suppose you have an `Signal[List[Foo]]`, and you want to get `Signal[Map[String, Signal[Foo]]]` where the keys of the map are Foo ids, and the values of the map are signals of the latest version of a Foo with that id.

The important part here is the desire to obtain individual signals of Foo by id, not to transform a `List` into a `Map`. Here is how we could do this:

```scala
case class Foo(id: String, version: Int)
val inputSignal: Signal[List[Foo]] = ???
 
val outputSignal: Signal[List[(String, Signal[Foo])]] = inputSignal.split(
  key = _.id
)(
  project = (key, initialFoo, thisFooSignal) => (key, thisFooSignal)
)
 
val resultSignal: Signal[Map[String, Signal[Foo]]] = outputSignal.map(list => Map(list: _*))
```

Let's unpack all this.

In this example our input is a signal of a list of Foo-s, and we `split` it into a signal of a list of `(fooId, fooSignal)` pairs. In each of those pairs, `fooSignal` is a signal that emits a new `Foo` whenever `inputSignal` emits a value that contains a Foo such that `foo.id == fooId`.

So essentially each of the pairs in `outputSignal` contains a a foo id and a signal of the latest version of a Foo for this id, as found in `inputSignal`.

Finally, in `resultSignal` we trivially transform `outputSignal` to convert a list to a map.

##### Example 2: Dynamic Lists of Laminar Elements

Suppose you want to render a list of `Foo`-s into a list of elements. You know how to render an individual `Foo` from its signal, but the list of Foo-s changes over time, and you want to avoid unnecessarily re-creating DOM elements.

This is what you can do:

```scala
case class Foo(id: String, version: Int)
   
def renderFoo(fooId: String, initialFoo: Foo, fooSignal: Signal[Foo]): Div = {
  div(
    "foo id: " + fooId,
    "first seen foo with this id: " + initialFoo.toString,
    "last seen foo with this id: ",
    child <-- fooSignal.map(_.toString)
  )
}
 
val inputSignal: Signal[List[Foo]] = ???
val outputSignal: Signal[List[Div]] = inputSignal.split(
  key = _.id
)(
  project = renderFoo
)
```

This works somewhat similarly to React.js keys, if you're familiar with that API:
* As soon as a `Foo` with id="123" appears in inputSignal, we call `renderFoo` to render it. This gives us a `Div` element **that we will reuse** for all future versions of this foo.
* We remember this `Div` element. Whenever `inputSignal` updates with a new version of the id="123" foo, the `fooSignal` in `renderFoo` is updated with this new version.
* Similarly, when other foo-s are updated in `inputSignal` their updates are scoped to their own invocations of `renderFoo`. The grouping happens by `key`, which in our case is the id of Foo.
* When the list emitted by `inputSignal` no longer contains a Foo with id="123", we remove its Div from the output and forget that we ever made it.
* Thus the output signal contains a list of Div elements matching one-to-one to the Foo-s in the input signal list.

So in essence, our `outputSignal` is broadly equivalent to `inputSignal.map(_.map(renderFoo))`, except that `renderFoo` requires **memoization** and **data** that simple mapping can't provide, thus the need for `split`.

To drive the point home, let's see how we would likely do this without `split`:

```scala
case class Foo(id: String, version: Int)
   
def renderFoo(foo: Foo): Div = {
  div(
    "foo id: " + foo.id,
    "last seen foo with this id: " + foo.toString
  )
}
 
val inputSignal: Signal[List[Foo]] = ???
val outputSignal: Signal[List[Div]] = inputSignal.map(_.map(renderFoo))
```

Same input and output types, but the behaviour is very different.

* First, renderFoo is now called every time inputSignal updates, for every Foo. This means that we are recreating the entire list of DOM nodes from scratch on every update. This is very inefficient.
  * In contrast, with `split`, we would only call `renderFoo` whenever we would see a new Foo with a previously unseen id in `inputSignal`, and the `child <-- ...` modifier would take care of updating existing elements as new versions foo came in.
* Second, renderFoo no longer has access to `initialFoo` and `fooSignal`. It does not know anymore if the foo it's rendering has changed over time, it can't listen for those changes, etc.


##### `splitOne`

Now that you know how the `split` operator works, it's only a small leap to understand its special-cased cousin `splitOne`. Where `split` works on observables of `List[Foo]`, `Option[Foo]` etc., `splitOne` works on observables of Foo itself, that is, on any observable:

```scala
case class Word(text: Boolean, isImportant: Boolean)
   
def renderWord(isImportant: Boolean, initialWord: Word, wordSignal: Signal[Word]): Div = {
  val tag = if (isImportant) b else span
  tag(child.text <-- wordSignal.map(_.text))
}
 
val inputSignal: Signal[Word] = ???

val outputSignal: Signal[HtmlElement] = 
  inputSignal.split(key = _.isImportant)(project = renderWord)
```

The example is a bit contrived to demonstrate that `key` does not need to be a record ID but could be any property. In this case, `renderWord` will be called only when the next emitted word's `isImportant` value is different from that of the last emitted word.  


#### Flattening Observables

Flattening generally refers to reducing the number of nested container layers. In Airstream the precise type definition can be found in the `FlattenStrategy` trait.

Aside from the `def flatMap[...](implicit strategy: FlattenStrategy[...])` method on the `Observable` itself, a similar `flatten` method is available on all observables by means of `MetaObservable` implicit value class. All you need to use these methods is provide an instance of `FlattenStrategy` that works for your specific observable's type. While you can easily implement your own flattening strategy, we have a few predefined in Airstream:

**`SwitchStreamStrategy`** flattens an `Observable[EventStream[A]]` into an `EventStream[A]`. The resulting stream will emit events from the latest stream emitted by the parent observable. So, as the parent observable emits a new stream, the resulting flattened stream _switches_ to imitating this last emitted stream.
*  This strategy is the default for the parent observable type that it supports. So if you want to flatten an `Observable[EventStream[A]]` using this strategy, you don't need to pass it to `flatten` explicitly, it is provided implicitly.

**`ConcurrentStreamStrategy`** is essentially a dynamic version of `EventStream.merge`.
* The resulting stream re-emits all the events emitted by all of the streams emitted by the input observable.
* If you stop observing the resulting stream, it will forget all of the streams it previously listened to. When you start it up again, it will start listening to the input observable from scratch, as if it's the first time you started it.

**`SwitchSignalStrategy`** flattens an `Signal[Signal[A]]` into a `Signal[A]`. Works similar to `SwitchStreamStrategy` but with Signal mechanics, tracking the last emitted value from the last signal emitted by the parent signal. Note: 
* This strategy is the default for flattening `Signal[Signal[A]]`.
* When switching to a new signal, the flattened signal emits the current value of the new signal (unless it's the same as the flattened signal's previous current value, as usual).
* The flattened signal follows standard signal expectations – it's lazy. If you stop observing it, its current value might get stale even if the signal that it's tracking has other observers.

**`SwitchFutureStrategy`** flattens an `Observable[Future[A]]` into an `EventStream[A]`. We first create an event stream from each emitted future, then flatten the result using `SwitchStreamStrategy`. So this ends up behaving very similarly, producing a stream that emits the value from the last future emitted by the parent observable, discarding the values of all previously emitted futures.
* As of Airstream v0.12.0, this strategy is the default for flattening Observable[Future[A]]. Previously there was no default for such observables so you had to manually specify a strategy.

To summarize, the above strategies result in an observable that imitates the latest stream / signal / future emitted by the parent observable. So as soon as the parent observable emits a new future / signal / stream, it stops listening for values produced by previously emitted futures / signals / streams.

**`ConcurrentFutureStrategy`** also flattens an `Observable[Future[A]]` into an `EventStream[A]`. Whenever a future emitted by the parent observable completes, this stream emits that value, regardless of any other futures emitted by the parent.

**`OverwriteFutureStrategy`** is similar to `ConcurrentFutureStrategy` except it does not emit the values of previous futures if a value from a newer future has already been emitted. For example, suppose the parent observable emits three futures. Future 3 is the first one to be completed, and when that happens the resulting flattened stream emits that value. When futures 1 and 2 eventually complete, their values will be ignored because they are considered stale, _overwritten_ by the more up to date result of future 3. This strategy is best for use cases like fetching autocompletion results where you don't care about older results if you have a newer result. 

All built-in strategies result in observables that emit each event in a new transaction for hopefully obvious reasons. 

`SwitchFutureStrategy`, `ConcurrentFutureStrategy`, and `OverwriteFutureStrategy` treat futures slightly differently than `EventStream.fromFuture`. Namely, if the parent observable emits a future that has already resolved, it will be treated as if the future has just resolved, i.e. its value will be emitted (subject to the strategy's normal logic). This is useful to avoid "swallowing" already resolved futures and enables easy handling of use cases such as cached or default responses. If this behaviour is undesirable you can easily define an alternative flattening strategy – it's a matter of flipping a single boolean in the relevant classes.


### Debugging

#### Debugging Observables

```scala
val stream: EventStream[Int] = ???
val useJsLogger: Boolean = false

val debugStream = stream
  .debugWithName("MyStream") // optional: use this prefix when logging below
  .debugLogEvents(when = _ < 0, useJsLogger) // optional: only log negative numbers
  .debugSpyStarts(topoRank => ???)
  .debugBreakErrors()

// Before:
stream.addObserver(obs)

// After: when debugging, replace with:
debugStream.addObserver(obs)
```

Airstream offers many debugging operators for observables, letting you run a callbacks (`debugSpy*`), log (`debugLog*`), or set a JS breakpoint (`debugBreak*`) at the most important times in the observable's lifecycle, including when emitting events or errors, starting or stopping, and evaluating the initial value (for signals). Those methods are available implicitly on every observable via `DebuggableObservable` and `DebuggableSignal`.

To debug an observable, call one of the available debug* methods to produce a new observable that listens to the original one and re-emits all its events and errors **and** also performs the specified debug action.

For example, `stream.debugLog()` will create a new observable that will simply print every event **and error** that it emits, that `stream` feeds to it, and `stream.debugLog().debugBreakErrors()` will do the same plus also set a JS breakpoint on errors in `stream`.

Very importantly, `debugLog` does **not** monkey-patch the original observable to add debugging functionality to it. We create a new observable that depends on the original, and debug _that_. This is true for every debug operator: in `stream.debugLog().debugBreakErrors()`, `debugLog()` creates an observable based on `stream`, and `debugBreakErrors()` creates an observable based on `stream.debugLog()`.

To make it crystal clear: `stream.debugLog()` will only log the events that `stream.debugLog()` emits, so you need to make sure to listen to it, not (just) to `stream`. Easiest is to just use `stream.debugLog()` in place of the original `stream` in your code.

Another important consideration is the **order** of debug procedures. It follows the propagation order. For example, in `stream.debugLog().debugSpy(fn)` the observable `stream.debugLog()` will obviously emit before the observable `stream.debugLog().debugSpy(fn)` emits, and so you will see the event printed first, and only after that will `fn` be called with that event. So if you're ever confused about why your debugger prints stuff in a weird order (e.g. event fired before stream is started), make sure your debug operators are in the expected order.

You can also use `debugWith(debugger)` method instead of `debug*` methods to provide an `ObservableDebugger` with all of the behaviour that you want, instead of adding it piece by piece.

Also regarding timing, the per-event debuggers like (`debugSpy()` / `debugLog()` / `debugBreak()`) do their thing right _before_ the event is fired (by the debugged observable, not the original), and the start/stop debuggers do their thing right _after_ the observable is started or stopped.

**Note for signals:** Any `debug*` method that triggers when the signal emits an event or an error will also trigger when the signal is **started**. This ensures that you won't miss the signal's initial value when debugging, even though the Signal's initial value technically isn't ever "emitted", in Airstream terms.

Logging debug operators generally offer an optional `when` filter to reduce noise, and a `useJsLogger` option that you should enable when you're logging native JS values. Logging plain JS objects with `println` will often result in printing `[object Object]`, but JS `dom.console.log` can print them nicely.

Also, your logs will be prefixed with `sourceName` which defaults to `stream.toString` but you can provide a prettier name as a param to the `.debug()` method.

We try to minimize the impact of debugging on the execution of your observable graph. To that end, any errors you throw in the callbacks you provide to `debugSpy*` methods will be reported as unhandled (wrapped in `DebuggerError`), and will not affect the propagation of events.


##### displayName

When debugging, it's very useful to name your observables. There are two ways to do this: `stream.setDisplayName("MyStream")` patches `stream` in place to set its `displayName`. It returns the same `stream`, it does **not** create a new observable.

This should be the preferred method for adding **permanent** names to important observables. For example, if you have a global `val AuthSignal: Signal[AuthContext]`, you might want to add `.setDisplayName("AuthSignal")` to its definition.

`displayName`, if explicitly set, is returned by the observable's `toString` method. If not explicitly set, it defaults to `defaultDisplayName`, which in turn defaults to java.Object's default `type@hashcode` toString implementation. If subclassing Observable, you can't override its toString method directly, but you can override `defaultDisplayName`.

`displayName` is of course useful to give human-readable identifiers to observables – seeing `AuthSignal` when print-debugging is much more useful than `MapSignal@f161`.

`displayName` is also prepended to logs produced by `debugLog*` methods. However, given `stream.setDisplayName("MyStream")`, while the `displayName` of `stream` is "MyStream", the `displayName` of `stream.setDisplayName("MyStream").debugLog()` is "MyStream|Debug", to differentiate it from `stream`.

You can of course also `setDisplayName` on the debugged stream directly: `stream.debugLog().setDisplayName("MyDebuggedStream")`, but if you have a _chain_ of debug streams that you want to apply the same name to, you can use the `withDisplayName` method: `stream.withDisplayName("MyDisplayName").debugLogEvents().debugSpyErrors().debugLogStarts()` – in this case all three debug* streams will have their `displayName` set to "MyDisplayName", but `stream` will not. This is because unlike `setDisplayName`, `withDisplayName` does not patch the original observable, it creates a new debug observable and patches that instead. `withDisplayName` works with debug chains because unlike regular observables, debug observables inherit their parent debug observable's `displayName` by default.

Airstream does not require `displayName` to be unique, although if you want to keep your sanity, it should be descriptive enough to clearly tell you which instance it refers to.


##### debugTopoRank

[Topological Ranks](#topological-rank) of observables determine the order of Airstream observable graph propagation. The new observables created using `debug*` operators will necessarily have different `topoRank`-s from the original observables. Due to (mostly) depth-first propagation in Airstream, debugging observables generally don't affect your graph's propagation, but if you're specifically debugging an issue related to topoRanks, you might want to avoid adding temporary observables to your observable graph.

You can check the `topoRank` of an observable using `observable.debugTopoRank`. Unlike other `debug*` operators, this one does not affect the observable or create new observables, it just returns the observable's topoRank.


#### Debugging Observers

```scala
val stream: EventStream[Int] = ???
val obs: Observer[Int] = ???

val debuggedObs = obs
  .debugWithName("MyObserver")
  .debugLog()
  .debugSpyErrors(err => ???)

// Before:
stream.addObserver(obs)

// After: when debugging, replace with:
stream.addObserver(debuggedObs)
```

Observers have debugging functionality very similar to observables, but it works slightly differently. Whereas adding debuggers to observables is similar to mapping them, adding debuggers to observers is similar to **contra**mapping them.

Just as in typical contramapping, the order of execution is reversed, so in the example above `debugSpyErrors()` callback will run before the error is logged by `log()`, and all the debugging happens before the original observer runs.

Similarly to debugged observables, your debuggers throwing do not affect the execution of the observable graph, instead they result in an unhandled `DebuggerError` being reported.

Just like observables, observers can be named using `setDisplayName` and `debugWithName`, with similar semantics. Note that even though observers are executed in "reverse" order (contramap semantics), debugged observers inherit `displayName` from their parent, so the `displayName` of all debugged observers in `obs.debugWithName("MyObserver").debugLog().debugSpy(???)` is "MyObserver".



### Error Handling

Airstream error handling is very different from conventional streaming libraries. Before diving into implementation details we first need to understand these differences and the reasons why such a departure from the norm is beneficial.


#### Scala Exception Handling

First, to clarify what kind of errors we're talking about here: this whole section is concerned with exceptions thrown by _user-provided_ code inside Airstream observables. For example, consider the `project` function in `stream.map(project)` throwing. Without special error handling capabilities in Airstream, such an exception would terminate the propagation of this stream and bubble up the call stack.

However, this behaviour is vastly undesirable in FRP context because the caller (which would be the source of events) is not normally in a position to handle failures of child observables. For example, a DOM event listener that publishes DOM events onto a stream can't do much about some other component failing to process some of those events. So we need a different strategy to deal with errors in observables.


#### Conventional Streaming Libraries

Conventional streaming libraries basically don't propagate errors in observables. If your stream fails, it gets _completed_, meaning that it informs all of its dependant observables and observers that it will no longer produce events, and is now shutting down forever.

If you don't want this outcome, you need to _recover_ from such an error by creating a stream that takes two inputs: this original stream, and a stream factory that returns a new stream that you will switch to if the original stream errors. 

This model does not make any sense to me. Consider this chain of observables:

```scala
object OtherModule {
  def doubled(parentStream: EventStream[Double]): EventStream[Double] = {
    parentStream.map(num => num * 2)
  }
}
 
// ----
 
import OtherModule.doubled
 
val stream1: EventStream[Double] = ???
val invertedStream: EventStream[Double] = stream1.map(num => 1 / num)
 
doubled(invertedStream).foreach(dom.console.log(_))
```

Inside `doubled`, there is nothing you can do to recover from an error in `parentStream`. You don't know what that stream does, so once it's broke, it's broke. You can't replace it with anything meaningful, just maybe some sentinel value to indicate failure.

So essentially, this requires you to either manually guard every single user-provided input to every stream, or lose your sanity to crazy amounts of coupling. In this scenario the more likely outcome is that you'll just ignore error handling, and your program will stop working on an error that would have been recoverable if only it wasn't in your streaming code.

Fundamentally, the problem here is conceptual: conventional streaming libraries see any exception in a stream as a terminal diagnosis for it.

Yes, such an error could have made parts of your application state inconsistent, that is a legitimate problem. However, unconditionally killing parts of your program that depend on a stream is not a way to mitigate inconsistent state for the simple reason that _the streaming library has no idea which, if any, state became inconsistent_, therefore it has no idea whether allowing the error to propagate will _mitigate_ state breakage or make it _even worse_.


#### Airstream's Approach

Airstream aspires to replicate the feel of native exception handling in FRP. However, whereas in imperative code we typically want to propagate exceptions up the call stack, in Airstream we instead want to propagate errors in observables to their observers and dependent observables.

Think about it this way: in imperative code, you call a function which can throw, and you can either handle the error or decide to let your caller handle it, and so on, recursively. In Airstream, you make an observable that depends on another observable which can "throw". So you can either handle the error, or let any downstream observables or observers handle it.

This FRP adaptation of classic exception handling is somewhat similar to the approach of Scala.rx, however since Airstream offers a unified reactive system, we have to adjust this basic idea to support event streams as well.

To contrast our approach with conventional streaming libraries: where they see a failed _stream_, we only see a failed _value_, generally expecting the next emitted value to work fine. This is similar to plain Scala exceptions: if a function throws an exception, it does not suddenly become broken forever. You can call it again with perhaps a different value, and it will perhaps not fail that time. Yes, if such an exception produces invalid state, you as a programmer need to address it. Same in Airstream. We give you the tools (more on this below), you do the work, because you're the only one who knows _how_.

We elaborate on the call stack analogy in the subsection [Errors Multiply](#errors-multiply) below.


#### Error Propagation

Airstream does not complete or terminate observables on error. Instead, we essentially propagate error values the same way we propagate normal values. Each Observer and InternalObserver has `onNext(A)` and `onError(Throwable)` methods defined, so both observables and observers are capable of accepting error values as inputs.

If you do not take special action, Airstream observables will generally (but not always, we'll get to that) pass through the errors to their observers untouched. Eventually the error will reach one or more external Observers.

When an error reaches an external observer, its `onError` method will handle it. If you didn't specify `onError` (e.g. if you used the `Observer(onNext)` factory to create an observer) or if your `onError` partial function is not defined for a given error, Airstream will report this error as _unhandled_.

You can get notified about unhandled errors by registering a callback using `AirstreamError.registerUnhandledErrorCallback(fn: Throwable => Unit)`. Similarly, you can unregister it using `AirstreamError.unregisterUnhandledErrorCallback(fn: Throwable => Unit)`.

By default Airstream registers `AirstreamErrors.consoleErrorCallback` to log all errors to the console because there is nothing worse than silently swallowed unhandled errors. You can unregister it and/or register more callbacks, e.g. to terminate your program, or to report the error to a service like Sentry.

Now let's dive deeper into each step of this process.

##### User Input is Generally Guarded Against Exceptions

Airstream guards from exceptions almost all user (developer) inputs that are used to build observables. For example, if the `project` function that you provided to `stream.map(project)` throws an exception, the resulting stream will emit an error, kick-starting this whole error handling process.

However, user inputs that are supposed to return a `Try` are assumed to not throw. Various public class constructors that require the `new` keyword are generally not intended to be used directly, these can also have params that are not guarded against exceptions.

For clarity, every Airstream method and constructor available to you has a Scaladoc comment reaffirming whether its parameters are guarded or not.

##### Errors Do Not Affect Propagation

When an error happens in an observable (assuming that it was guarded against, as it should always be, see above), the observable emits this error to all of its observers – internal and external – in the same manner that it would emit a value. Think of it as propagating a `Try[A]` value instead of just `A`.

##### Errors in an Observer Do Not Affect Other Observers

Similarly, and unlike conventional streaming libraries, if an error happened in an observer's `onNext` or `onError` callback, this error will be reported as _unhandled_, but it will not prevent other observers or the code immediately following this observer's definition from running.

If you're concerned that the contents of your `onNext` / `onError` can fail and make your app state inconsistent, you do the same thing you've always done in this situation – `try` some code and `catch` the error, just keep it all inside the callback in question:

```scala
stream.addObserver(Observer(onNext = v =>
  try { riskyFoo(v) }
  catch { case err => recoverFromFooFail() }
))
```

Remember, this is just for observers. We have a better way for observables, read on.

##### Unhandled Errors Do Not Terminate The Program

In Airstream, unhandled errors do not result in the program terminating. By default they are reported to the console. You can specify different or additional handlers such as `AirstreamError.debuggerErrorCallback` or even a custom handler that effectively terminates your program.

Regardless of this seeming leniency, you should still handle all of your errors at some point before they become _unhandled_. In a good Airstream codebase every _unhandled_ error must be treated as a bug.

##### Error Timing is Consistent

Observables generally emit errors at the same time as they would have emitted a non-error value. For example, a `CombinedObservable` like `stream.combineWith(stream.map(foo))` will only emit a single value if `stream` emits a value (yes, Airstream deals with [FRP Glitches](#frp-glitches) for you). Similarly, it will only emit a single error if `stream` emits an error. However, that error will be wrapped in `CombinedError` because it needs to support the case when its parent observables simultaneously emit a different error.

On the other hand, `MergeEventStream` does no such synchronization, it emits the errors similarly to how it emits non-error events – as they come, putting all but the first seen one in a new transaction.

`DelayEventStream` re-emits incoming errors with the same delay that it uses for normal values.

##### Event Streams Generally Forget Errors

Similarly to how event streams generally do not keep track of their last emitted value, they also forget their last emitted error once they finish propagating it to their observers.

##### Signals Remember Errors

If a `Signal[A]` observable runs into an error that it doesn't handle itself, this error becomes its current state. `Signal`s' current state is actually `Try[A]`, not just `A`. 

`StrictSignal[A]` has a public `now(): A` method that returns its current value. Calling it is equivalent to `tryNow().get`– it will throw if current state is a Failure.

##### Errors Can Become Wrapped 

Errors originating in observables error handling code (e.g. the `pf` in `stream.recover(pf)`) are wrapped in `ErrorHandlingError`.

In Observers created using Airstream-provided constructors:

* If the user-provided observer callback (e.g. the `handleError` in `Observer.withRecover[Int](cb, handleError)`) throws while processing an incoming error, the error is wrapped into `ObserverErrorHandlingError` and sent off as unhandled error.

* If `handleObserverErrors` constructor param is `true` (that's the default), and the user-provided observer callback throws while processing an incoming event (as opposed to an incoming error), the error is wrapped in `ObserverError` and sent to this same observer's onError method. So, such an observer has a chance to process its own failure. The usefulness of that lies mostly in being able to automatically pass this error up the chain of observers.
  
  For example:

  ```scala
  val sourceObs = Observer.fromTry[Int](println)
  val derivedObs = sourceObs.contramap[Int] { value =>
    if (value >= 0) 1 else throw new Exception("negative!")
  }
  derivedObs.onNext(1) // prints "Success(1)"
  derivedObs.onNext(-1) // prints "Failure(ObserverError: java.lang.Exception: negative!)"
  // no errors are sent into unhandled because `println` is a total function, it handles them all.
  ```

You can always access the original errors on wrapped errors, and it will always provide you with a stack trace that includes the line where your code failed. Make sure to configure error reporting as well as Scala.js source maps to make use of this in fullOpt / production.

##### Errors Multiply

The same error might need to be handled multiple times to avoid becoming _unhandled_.

It is perhaps counter-intuitive, but it's obvious in retrospect:

```scala
val upStream = ???
val fooStream = upStream.map(foo)
val barStream = upStream.map(bar)
fooStream.foreach(dom.console.log("foo"))
barStream.foreach(dom.console.log("bar"))
```

If `upStream` emits an error, both `fooStream` and `barStream` will emit an error – the same instance of it, actually. Then it will end up reported as _unhandled_ – twice!

If we try to lean on our call stack analogy, it kinda breaks this time. Call stack is a stack, a linear data structure much like a list. An exception can only ever bubble to just one caller before bubbling up to its one and only caller. But in our case it feels like the bubbles are splitting into multiple parallel "streams".

Instead, think of `fooStream` and `barStream` as independent function **calls** that both require the value of `upStream`, except by magic of FRP `upStream` is only executed once and its value (well, its exception in this case) is shared with both `fooStream` and `barStream` calls. If you call a broken function twice, you get two exceptions (identical ones, thanks to FRP) and thus two call stacks to propagate through.

To be extra clear, if we add `.recoverIgnoreErrors` to the `fooStream` definition above, its observer will not receive the error coming from `upStream`, and will not report that error as _unhandled_. We did, after all, handle it, so that's fair. However, `barStream`'s observer would still receive that same error, and would still report it as _unhandled_. This is why handling errors in the right place is very important, just like handling exceptions is in plain Scala code.


#### Recovering from Errors

As we mentioned, generally observables propagate the errors they receive with no change. However, we can recover from errors, like this:

```scala
val signal0: Signal[A] = ???
val signal1 = signal0.map(whatever)
val signal2 = signal1.recover {
  case MyException(foo) if foo.isGood => Some(foo) // emit foo
  case MyException(foo) if !foo.isGood => None // skip this, emit nothing
  case o: MyOtherException => throw new Exception("lolwat") // emit new error
}
```

Importantly, this `recover` method does not affect `signal0`, or even `signal1`. Like any other operator it's just creating a new observable (`signal2`), except this one has a defined error handling strategy, so it will process any errors that come through it.

When `signal0` emits an error, `signal2` will feed it to the partial function we provided to it. That function should normally emit either `Some(value)` to make `signal2` emit some `value`, or `None` to just skip this event altogether, as if it never happened. Yes, this latter case means that `signal2`'s current state would remain at whatever it was before this error came in, meanwhile `signal0`'s current state would be updated to the error (same for `signal1`).

Any errors that this partial function is not defined for will pass through without being handled. If the partial function throws an error, it will be passed down wrapped in `ErrorHandlingError`.

In addition to `recover`, Observables have a couple shorthand operators:

**`recoverIgnoreErrors`** just skips all errors, emitting only good values. This is an FRP equivalent of an empty catch block.

Note that you can't ignore an error in a `Signal`'s initial value, as it _needs_ a `Try[A]` value to be its state whenever it's started. Therefore, if the initial value is an error, and `recover` returns `None` while handling it (that's what `recoverIgnoreErrors` does), the initial value is set to the original error.

**`recoverToTry`** transforms an `Observable[A]` into an `Observable[Try[A]]` that never emits an error (it emits `Failure(err)` as a value instead).

You can use it to get a stream of errors as plain values, for example:

```scala
stream.recoverToTry.collect { case Failure(err) => err } // EventStream[Throwable]
```


#### Handling Errors Using Observers

**`Observer.fromTry(nextTry => ...)`** and **`Observer.withRecover(onNext, onError: PartialFunction[Throwable, Unit])`** let you handle all or some of the errors coming from upstream observables. Errors for which `onError` is not defined get reported as unhandled.

**`Observer.ignoreErrors(onNext)`** is similar to `recoverIgnoreErrors` on observables – it simply silences any error it receives, so that it does not get reported as unhandled.

Observers that are derived from other observers, e.g. `observer.contramap[Int](...)`, pass the error to the original observer, and so maintain the original observer's error handling behaviour.

Airstream-provided Observer constructors that let you specify an error handling callback (e.g. `Observer.fromTry` and `Observer.withRecover`) also have a `handleObserverErrors` param. When this param is true (that's the default), if the observer's callback throws while processing an incoming event, the same observer's **error** callback (the onError method) gets called with the error wrapped in ObserverError. This lets you automatically propagate unexpected exceptions up the chain of observers instead of sending the error into unhandled right away. For more details, see [Errors Can Become Wrapped](#errors-can-become-wrapped) above.


#### Other Error Handling Considerations

* **`foldLeft`** operator is unable to proceed when encountering an error, so such an observable will enter a permanent error state if it encounters an error. You can not use the standard `recover` method to recover from this. You need to use `foldLeftRecover` instead of `foldLeft` to supply your error handling logic.

* **`filter`** operator can't filter if its passes function fails, so it will pass through all errors that it receives, unfiltered. You can filter errors using `recover`, by returning `None`.

* Remember that Signal's initial value is not evaluated until and unless it is needed. That is true even if the initial value would have been an error because obviously you can't know what it is without evaluating it. And if an error is not evaluated, then it can't possibly be reported anywhere because, well, it didn't actually happen. In practice this means that the initial value of a Signal whose only consumer is its `.changes` stream is completely ignored (because no one cares about it). @TODO[API] Should we reconsider this particular aspect of laziness? Either way, we should document the rationale for that some more.

* This is an unfortunately flawed edge case in our design. We might address that eventually. Please let me know if you run into this problem.

* Signals can also potentially face a similar issue, but the scenario allowing it is even more convoluted because a signal's initial value is evaluated lazily – only if and when its observers request it – so it can't really throw an error if no one is looking at it.





## Limitations

* Airstream only runs on Scala.js because its primary intended use case is unidirectional dataflow architecture on the frontend. I have no plans to make it run on the JVM. It would require too much of my time and too much compromise, complicating the API to support a completely different environment and use cases. 
* Airstream has no concept of observables "completing". Personally I don't think this is much of a limitation, but I can see it being viewed as such. See [Issue #23](https://github.com/raquo/Airstream/issues/23).


## My Related Projects

- [Laminar](https://laminar.dev) – Efficient reactive UI library for Scala.js that uses Airstream
- [Waypoint](https://github.com/raquo/Waypoint) – Efficient router for Laminar made with Airstream
- [XStream.scala](https://github.com/raquo/XStream.scala) – streaming library used by Laminar before Airstream

Other building blocks of Laminar:

- [Scala DOM Types](https://github.com/raquo/scala-dom-types) – Type definitions that we use for all the HTML tags, attributes, properties, and styles
- [Scala DOM TestUtils](https://github.com/raquo/scala-dom-testutils) – Test that your Javascript DOM nodes match your expectations



## Author

Nikita Gazarov – [@raquo](https://twitter.com/raquo)



## License

Airstream is provided under the [MIT license](https://github.com/raquo/Airstream/blob/master/LICENSE.md).

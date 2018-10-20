# Changelog

Breaking changes in **bold**.

#### v0.4 – Oct 2018 – TO BE RELEASED

* **New: Error Handling – see whole new section in docs**
* **API: a bunch of class / trait member fields are now `Try[A]` instead of `A`**
* **Build: Drop Scala 2.11 support**

#### v0.3 – Sep 2018 

* **Naming: Observer.map -> Observer.contramap**
* **Naming: WriteBus.mapWriter -> WriteBus.contramapWriter**
* **API: `EventStream.mapTo` now accepts `value` by name; previous behaviour available using `mapToValue` method**
* **API: `Observable.flatten` now needs a `FlattenStrategy` except for `SwitchEventStream` which is provided implicitly as a default**
* New: Integration with Futures. `EventStream.fromFuture`, `Signal.fromFuture`, `State.fromFuture`, `SwitchFutureStrategy`, `ConcurrentFutureStrategy`, `OverwriteFutureStrategy`
* New: `LazyObservable.flatMap` (available via implicits)
* New: `LazyObservable.map` returns a more specific type

#### v0.2 – Apr 2018

* **API: Signal only fires if `nextValue != prevValue`**
* **Naming: Var -> StateVar**
* New: Make `Val.now()` public
* New: signal.Var
* Fix: Ensure Signal's initialValue has been evaluated onStart

#### v0.1 – Apr 2018

Initial release. First version extracted from Laminar repo.

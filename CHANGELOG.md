# Changelog

Breaking changes in **bold**.

#### v0.4.1 – Dec 2018

* Fix: NPE from errors unhandled by `recover` partial function

#### v0.4 – Nov 2018

* **New: Error Handling – see whole new section in docs**
* **API: a few breaking changes for those who extend Airstream classes**
  * A bunch of class / trait member fields are now `Try[A]` instead of `A`
  * Split `fire` into `fireValue` & `fireError`, etc.
  * By and large this does not affect existing Airstream usage, just customization by subclassing
* **API: `Observable.removeObserver` and `Transaction.removeExternalObserver` are now private ([#10](https://github.com/raquo/Airstream/issues/10))**
* **Build: Drop Scala 2.11 support**
* New: `SignalViewer`
  * This serves as a warning about my intention to deprecate and eventually remove the entirety of the `State` type in Airstream. Its strictness has not proved useful, and yet has plenty of drawbacks. See [Laminar#37](https://github.com/raquo/Laminar/issues/37) for details. If you want to speak up against that, now is the time.
* New: `Ref` performance util (experimental)

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

Initial release. First version extracted from [Laminar](https://github.com/raquo/Laminar) repo.

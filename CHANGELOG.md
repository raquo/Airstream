# Changelog

Breaking changes in **bold**.

#### v0.6 – TBD

* **API: `fromValue`, `fromSeq`, and `fromTry` event streams now require `emitOnce` param**
* **API: `toSignal`, `toSignalWithTry` arguments are now passed by name**
  * This means that these values will now be evaluated only if / when the resulting Signal is started for the first time.
* New: `flatMap` ([#20](https://github.com/raquo/Airstream/pull/20)) – thanks, [@yurique](https://github.com/yurique)!
  * **API: FlattenObservable type params changed, might break your custom implementations – see diff** 
* New: `SwitchSignalStrategy` to flatten signals of signals
* New: `split` and `splitIntoSignals` operators
  * Powerful operators letting you split any observable into multiple streams or signals. Provide an easier way to efficiently render children nodes in Laminar.
* New: `composeChanges` and `composeChangesAndInitial` operators for `Signal`
* New: Report failures of unhandled error callbacks

#### v0.5.2 – Jan 2019

* API: Make `Val` a `StrictSignal` (it already behaved like one)

#### v0.5.1 – Dec 2018

* API: Make `FutureSignal` a `StrictSignal` (it already behaved like one)

#### v0.5 – Dec 2018

* **API: Eliminate the whole concept of State [#13](https://github.com/raquo/Airstream/pull/13)**
  * `State` and `StateVar` are no more. RIP
  * Merge `MemoryObservable` into `Signal`
  * Merge `LazyObservable` into `Observable`
  * Remove `toLazy` method as all observables are now lazy
  * **Migration guide**
    * Use `Signal` instead of `State` and `MemoryObservable`. This is NOT a drop in replacement as State was not lazy. Make sure you have observers (internal or external) on all Signals that would be replacing State, or else they won't run. Consult the updated documentation for a reminder on Signal semantics.
    * For `Signal` alternatives to `State`'s `now()` / `tryNow()` methods, see the _Getting Signal's current value_ section in the new docs.
    * Use `Observable` instead of `LazyObservable`.
    * Use `Var` instead of `StateVar`.
    * Use `toSignal` instead of `toState`.
    * Remove invocations of `toLazy` method, they are not needed anymore. 
* **API: Make Var and its signal strict (not lazy)**
  * Var does not provide a WriteBus anymore, only an Observer
  * Var now exposes `now` and `tryNow` methods, as does its signal
  * See Var docs for details 
* **API: Make SignalViewer a Signal itself**
  * A StrictSignal, to be precise 
* **API: Remove flatMap method**
  * Use map(...).flatten
  * I don't have time to sort out type inference and other stuff needed to get `flatMap` to work nicely.
* New: Update Vars using (currentValue => nextValue) functions
  * New methods on Var instances: `set`, `setTry`, `update`, `tryUpdate`
* New: Batch update Vars in a single transaction
  * Companion object methods: `Var.set`, `Var.setTry`, `Var.update`, `Var.tryUpdate`
  * See new Var docs for details

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

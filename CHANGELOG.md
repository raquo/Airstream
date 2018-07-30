# Changelog

Breaking changes in **bold**.

#### master

* New: `EventStream.fromFuture`, `Signal.fromFuture`, `State.fromFuture`
* New: `LazyObservable.flatMap` (available via implicits)
* New: `LazyObservable.map` returns a more specific type
* **API: `Observable.flatten` now needs a `FlattenStrategy` except for `SwitchEventStream` which is provided implicitly as a default**

#### v0.2 – Apr 2018

* **API: Signal only fires if `nextValue != prevValue`**
* **Naming: Var -> StateVar**
* New: Make `Val.now()` public
* New: signal.Var
* Fix: Ensure Signal's initialValue has been evaluated onStart

#### v0.1 – Apr 2018

Initial release. First version extracted from Laminar repo.

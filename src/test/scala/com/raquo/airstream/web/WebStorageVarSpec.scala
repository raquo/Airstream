package com.raquo.airstream.web

import com.raquo.airstream.UnitSpec
import com.raquo.airstream.core.AirstreamError
import com.raquo.airstream.fixtures.Effect
import org.scalajs.dom
import org.scalatest.BeforeAndAfter

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class WebStorageVarSpec extends UnitSpec with BeforeAndAfter {

  // #TODO[Test] can we test with disabled local storage, or across several frames?
  // #TODO[Test] verify effects too, to make sure there are no duplicates
  private val effects = mutable.Buffer[Effect[?]]()

  private val errorEffects = mutable.Buffer[Effect[Throwable]]()

  private val err1 = new Exception("err1")
  private val err2 = new Exception("err2")

  private val errorCallback = (err: Throwable) => {
    errorEffects += Effect("unhandled", err)
    ()
  }

  before {
    AirstreamError.registerUnhandledErrorCallback(errorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
  }

  after {
    AirstreamError.registerUnhandledErrorCallback(AirstreamError.consoleErrorCallback)
    AirstreamError.unregisterUnhandledErrorCallback(errorCallback)
    errorEffects.clear()
    dom.window.localStorage.clear()
    dom.window.sessionStorage.clear()
  }

  case class Foo(id: Int)

  it("localStorage basics") {

    assertEquals(WebStorageVar.isLocalStorageAvailable(), true)

    val fooVar = WebStorageVar
      .localStorage(key = "foo", syncOwner = None)
      .withCodec[Foo](
        encode = _.id.toString,
        decode = str => Try(Foo(str.toInt)),
        default = Success(Foo(-1))
      )

    assertEquals(dom.window.localStorage.getItem("foo"), "-1")
    assertEquals(fooVar.tryNow(), Success(Foo(-1)))

    // --

    fooVar.update(_.copy(id = 2))

    assertEquals(dom.window.localStorage.getItem("foo"), "2")
    assertEquals(fooVar.tryNow(), Success(Foo(2)))

    // --

    fooVar.set(Foo(id = 3))

    assertEquals(dom.window.localStorage.getItem("foo"), "3")
    assertEquals(fooVar.tryNow(), Success(Foo(3)))

    assertEquals(errorEffects.toList, Nil)

    // --

    fooVar.setTry(Failure(err1))

    assertEquals(dom.window.localStorage.getItem("foo"), "3")
    assertEquals(fooVar.tryNow(), Failure(err1))

    assertEquals(errorEffects.toList,
      List(
        Effect("unhandled", err1)
      ))
    errorEffects.clear()

    // --

    fooVar.pullOnce()

    assertEquals(dom.window.localStorage.getItem("foo"), "3")
    assertEquals(fooVar.tryNow(), Success(Foo(3)))

  }

  it("sessionStorage basics") {

    assertEquals(WebStorageVar.isSessionStorageAvailable(), true)

    val fooVar = WebStorageVar
      .sessionStorage(key = "foo", syncOwner = None)
      .withCodec[Foo](
        encode = _.id.toString,
        decode = str => Try(Foo(str.toInt)),
        default = Success(Foo(-1))
      )

    assertEquals(dom.window.sessionStorage.getItem("foo"), "-1")
    assertEquals(fooVar.tryNow(), Success(Foo(-1)))

    // --

    fooVar.update(_.copy(id = 2))

    assertEquals(dom.window.sessionStorage.getItem("foo"), "2")
    assertEquals(fooVar.tryNow(), Success(Foo(2)))

    // --

    fooVar.set(Foo(id = 3))

    assertEquals(dom.window.sessionStorage.getItem("foo"), "3")
    assertEquals(fooVar.tryNow(), Success(Foo(3)))

    assertEquals(errorEffects.toList, Nil)

    // --

    fooVar.setTry(Failure(err1))

    assertEquals(dom.window.sessionStorage.getItem("foo"), "3")
    assertEquals(fooVar.tryNow(), Failure(err1))

    assertEquals(errorEffects.toList,
      List(
        Effect("unhandled", err1)
      ))
    errorEffects.clear()

    // --

    fooVar.pullOnce()

    assertEquals(dom.window.sessionStorage.getItem("foo"), "3")
    assertEquals(fooVar.tryNow(), Success(Foo(3)))

  }
}

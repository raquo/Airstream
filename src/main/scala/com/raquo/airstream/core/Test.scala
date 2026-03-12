package com.raquo.airstream.core

import scala.util.Try

Signal.fromValue(2).scanLeftGeneratedRecover(_ => Try(2))(_ + _)
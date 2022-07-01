package com.jonathan.troubleshoot

import scala.util.Random

package object streaming {
  implicit class PimpedRandom(delegate: Random) {
    def between(range: Range): Int =
      delegate.nextInt(range.end - range.start + 1) + range.start
  }
}

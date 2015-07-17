package de.sciss.osc

import org.scalatest.{Matchers, FlatSpec}

/*
    To run only this test:

    test-only de.sciss.osc.Issue6

 */
class Issue6 extends FlatSpec with Matchers {

  "A Bundle" should "support the collection's split method" in {
    val m1 = Message("/foo")
    val m2 = Message("/bar")
    val b0 = Bundle.now(m1, m2)
    val (b1, b2) = b0.splitAt(1)
    assert(b1 === Bundle.now(m1))
    assert(b2 === Bundle.now(m2))
  }
}

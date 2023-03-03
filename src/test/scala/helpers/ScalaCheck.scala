package helpers

import utest.assertMatch
import org.scalacheck.Prop
import org.scalacheck.Test.{Parameters, Passed, check}

object ScalaCheck {

  def assertProps(prop: Prop): Unit = {
    if check(Parameters.default, prop).status != Passed then
      prop.check()

    assertMatch(check(Parameters.default, prop).status) { case Passed => }
  }
}

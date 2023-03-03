package game.mana

import utest.*
import org.scalacheck.*
import org.scalacheck.Test.{Parameters, check}
import helpers.*
import helpers.ScalaCheck.assertProps
import org.scalacheck.Gen.oneOf
import org.scalacheck.Prop.forAll

object ManaPoolTest extends TestSuite {
  implicit lazy val color: Arbitrary[Color] = Arbitrary(oneOf(Color.values))

  val tests: Tests = Tests {
    val colorGen = Gen.oneOf(Color.values)
    val manaPoolGen = Gen.nonEmptyMap(for {
      c <- colorGen
      n <- Gen.choose(0, 100)
    } yield (c, n))


    test("ManaPool") {

      test("Add mana of one color") {
        assertProps(forAll { (color: Color, n: Int) =>
          (ManaPool.empty() + (color, n)).pool(color) == n
        })
      }

      test("Add any amount of mana") {
        assertProps(forAll(manaPoolGen) { manas =>
          (ManaPool.empty() ++ manas).pool.forall { case (color, amount) => manas.getOrElse(color, 0) == amount }
        })
      }

      test("Remove mana cost") {
        val manaPool = ManaPool(Map(
          Color.red -> 10,
          Color.green -> 10,
          Color.white -> 10,
          Color.blue -> 10,
          Color.black -> 10,
          Color.colorless -> 10)
        )

        assert((manaPool - ManaCost("RR")).get.pool == manaPool.pool.updated(Color.red, 8))
        assert((manaPool - ManaCost("2")).get.pool.exists(_._2 == 8))
        assert((manaPool - ManaCost("LLG")).get.pool == manaPool.pool.updated(Color.colorless, 8).updated(Color.green, 9))
        assert((manaPool - ManaCost("UU")).get.pool == manaPool.pool.updated(Color.blue, 8))
        assert((manaPool - ManaCost("RRBBL")).get.pool == manaPool.pool.updated(Color.colorless, 9).updated(Color.red, 8).updated(Color.black, 8))

        val manaPoolShort = ManaPool(Map(
          Color.red -> 2,
          Color.green -> 1,
          Color.white -> 1,
          Color.blue -> 2,
          Color.black -> 2,
          Color.colorless -> 1)
        )
        assert((manaPoolShort - ManaCost("3UUBBRR")).get == ManaPool.empty())
      }

      test("Exception if not enough mana") {
        val manaPool = ManaPool(Map(
          Color.red -> 0,
          Color.green -> 0,
          Color.white -> 2,
          Color.blue -> 0,
          Color.black -> 0,
          Color.colorless -> 0)
        )

        assert((manaPool - ManaCost("WWW")).failed.get.getLocalizedMessage == "No enough mana in your mana pool")
      }
    }
  }
}
package ciris.cats.api

import ciris.PropertySpec

final class CirisInstancesForCatsSpec extends PropertySpec {
  "CirisInstancesForCats" when {
    "providing Show instances" should {
      "be able to provide all required instances" in {
        import _root_.cats.Show
        import _root_.cats.implicits._
        import ciris._
        import ciris.api._
        import ciris.cats._
        import ciris.ConfigError.right

        Show[ConfigEntry[List, String, String, Int]].show(
          ConfigEntry
            .applyF[List, String, String](
              "key",
              ConfigKeyType[String]("keyType"),
              List(Right("123"))
            )
            .mapValue(_.toInt)
        ) shouldBe "ConfigEntry(key, ConfigKeyType(keyType))"

        Show[ConfigEntry[List, String, String, Int]].show(
          ConfigEntry
            .applyF[List, String, String](
            "key",
            ConfigKeyType[String]("keyType"),
            List(Right("123"))
          )
            .mapValue(s => (s + "4").toInt)
        ) shouldBe "ConfigEntry(key, ConfigKeyType(keyType))"

        val configValue = ConfigValue.applyF[List, Int](List(right(123)))

        Show[ConfigValue[List, Int]].show(
          configValue
        ) shouldBe configValue.toString

        Show[_root_.ciris.api.Id[Int]]
          .show(123) shouldBe "123"

        Show[ConfigError].show(
          ConfigError("error")
        ) shouldBe "ConfigError(error)"

        Show[ConfigErrors].show(
          ConfigErrors(
            ConfigError("error1"),
            ConfigError("error2")
          )
        ) shouldBe "ConfigErrors(ConfigError(error1), ConfigError(error2))"

        Show[ConfigException].show(
          ConfigException(
            ConfigErrors(
              ConfigError("error")
            ))
        ) shouldBe
          """
            |ciris.ConfigException: configuration loading failed with the following errors.
            |
            |  - error.
            |
          """.stripMargin.trim + "\n"

        Show[ConfigKeyType[Int]].show(
          ConfigKeyType[Int]("name")
        ) shouldBe "ConfigKeyType(name)"

        Show[Secret[Int]].show(
          Secret(123)
        ) shouldBe "Secret(40bd001)"
      }
    }
  }
}

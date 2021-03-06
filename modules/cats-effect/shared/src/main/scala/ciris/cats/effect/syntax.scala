package ciris.cats.effect

import cats.Eval
import cats.effect.{IO, LiftIO}
import ciris.ConfigSource
import ciris.api.{Apply, Id}

object syntax {
  implicit def catsEffectConfigSourceIdSyntax[K, V](
    source: ConfigSource[Id, K, V]
  ): CatsEffectConfigSourceIdSyntax[K, V] = {
    new CatsEffectConfigSourceIdSyntax[K, V](source)
  }

  final class CatsEffectConfigSourceIdSyntax[K, V] private[ciris] (
    val source: ConfigSource[Id, K, V]
  ) extends AnyVal {

    /**
      * Suspends the reading of this configuration source into a context
      * `F`, while also memoizing values. Since memoization itself is an
      * effect, values read from the source will be wrapped in two `F`s,
      * with the first effect being for memoization.
      *
      * {{{
      * scala> import cats.effect.IO,
      *      |   ciris._,
      *      |   ciris.cats._,
      *      |   ciris.cats.effect.syntax._
      *
      * scala> val source = ConfigSource(ConfigKeyType[String]("key type")) { key =>
      *      |   Right(s"$$key: $${new java.util.Random().nextInt()}")
      *      | }.suspendMemoizeF[IO]
      * source: ConfigSource[[v]cats.effect.IO[cats.effect.IO[v]],String,String] = ConfigSource(ConfigKeyType(key type))
      *
      * scala> val memoizedValue = source.read("key").value
      * memoizedValue: cats.effect.IO[cats.effect.IO[Either[ciris.ConfigError,String]]] = IO$$1699125381
      *
      * scala> val twiceEqual =
      *      |   for {
      *      |     memoized <- memoizedValue
      *      |     value1 <- memoized
      *      |     value2 <- memoized
      *      |   } yield value1 == value2
      * twiceEqual: cats.effect.IO[Boolean] = IO$$1049599874
      *
      * scala> twiceEqual.unsafeRunSync()
      * res0: Boolean = true
      * }}}
      *
      * The implementation relies on `IO` for memoization, so there is
      * a requirement that there must be a `LiftIO` instance available
      * for `F`. If you're using `IO`, a `LiftIO` instance is already
      * available and defined as identity.
      *
      * @tparam F the context in which to suspend and memoize
      * @return a new `ConfigSource`
      */
    def suspendMemoizeF[F[_]](
      implicit F: Apply[F],
      L: LiftIO[F]
    ): ConfigSource[λ[v => F[F[v]]], K, V] = {
      type FF[A] = F[F[A]]

      implicit val applyFF: Apply[FF] =
        new Apply[FF] {
          override def product[A, B](ffa: FF[A], ffb: FF[B]): FF[(A, B)] =
            F.map(F.product(ffa, ffb)) { case (fa, fb) => F.product(fa, fb) }

          override def map[A, B](ffa: FF[A])(f: A => B): FF[B] =
            F.map(ffa)(fa => F.map(fa)(f))
        }

      ConfigSource.applyF[FF, K, V](source.keyType) { key =>
        IO(IO.eval(Eval.later(source.read(key).value)).to[F]).to[F]
      }
    }
  }
}

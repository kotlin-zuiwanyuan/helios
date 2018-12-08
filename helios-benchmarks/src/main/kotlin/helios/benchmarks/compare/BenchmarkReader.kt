package helios.benchmarks.compare

import arrow.core.*
import arrow.data.ListK
import arrow.data.k
import arrow.instances.either.monad.*
import arrow.typeclasses.*
import arrow.effects.*
import arrow.effects.instances.io.monad.monad
import arrow.instances.`try`.applicative.applicative
import arrow.instances.either.applicative.applicative
import java.io.BufferedReader
import java.io.FileReader

fun readBenchmark(filePath: String, benchmarkId: String): IO<Benchmark> {

  fun String.removeCommas(): String {
    fun String.removeHeadComma() =
      if (this.startsWith('"')) this.drop(1) else this

    fun String.removeTailComma() =
      if (this.endsWith('"')) this.dropLast(1) else this
    return this.removeHeadComma().removeTailComma()
  }

  fun parseBenchmark(
    benchmark: String,
    mode: String,
    threads: String,
    samples: String,
    score: String,
    scoreError: String,
    unit: String
  ): Either<Throwable, Benchmark> =
    Try.applicative().tupled(
      Try { threads.toInt() },
      Try { samples.toInt() },
      Try { score.toDouble() },
      Try { scoreError.toDouble() }
    ).fix()
      .map { tuple ->
        Benchmark(
          benchmark.removeCommas(),
          mode.removeCommas(),
          tuple.a,
          tuple.b,
          tuple.c,
          tuple.d,
          unit.removeCommas()
        )
      }
      .toEither()

  fun parseLine(line: String): Either<Throwable, Option<Benchmark>> {
    val cells = line.split(",")
    return when {
      cells.isEmpty() -> Either.right(None)
      cells.size == 7 -> {
        parseBenchmark(cells[0], cells[1], cells[2], cells[3], cells[4], cells[5], cells[6]).map { Option(it) }
      }
      else -> Either.left(IllegalArgumentException("Wrong line: $line"))
    }
  }

  fun readCSVFile(path: String): IO<List<Benchmark>> =
    IO.monad().binding {
      val file = IO { BufferedReader(FileReader(path)) }.bind()
      val lines = IO { file.readLines().drop(1) }.bind()
      val result: Either<Throwable, ListK<Option<Benchmark>>> =
        lines.k().traverse(Either.applicative()) { parseLine(it) }.fix()
      result.map { it.mapFilter { it }.list }
    }
      .fix()
      .attempt()
      .flatMap { it.flatten().fold({ ex -> IO.raiseError<List<Benchmark>>(ex) }, { list -> IO.just(list) }) }

  return readCSVFile(filePath)
    .map { list -> Option.fromNullable(list.find { it.benchmark == benchmarkId }) }
    .flatMap {
      it.fold(
        { IO.raiseError<Benchmark>(IllegalArgumentException("Benchmark id not found")) },
        { b -> IO.just(b) })
    }

}

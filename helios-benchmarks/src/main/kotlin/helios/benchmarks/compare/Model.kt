package helios.benchmarks.compare

data class Benchmark(
  val benchmark: String,
  val mode: String,
  val threads: Int,
  val samples: Int,
  val score: Double,
  val scoreError: Double,
  val unit: String
)

enum class BenchmarkResult {
  IMPROVED, WARN, ERROR
}
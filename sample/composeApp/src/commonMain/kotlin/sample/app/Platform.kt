package sample.app

expect val isWasm: Boolean

fun getYouTubeUrl(videoId: String): String {
  return if (isWasm) {
    "https://www.youtube.com/embed/$videoId"
  } else {
    "https://www.youtube.com/watch?v=$videoId"
  }
}

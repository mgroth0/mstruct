implementations {
  kjlibGit
  stream
}

implementations(
  libs.snakeyaml,
  libs.kotlinx.serialization.json,
  dependencies.kotlin("reflect")
)

apis {
  klib
  file
}

plugins {
  kotlin("plugin.serialization")
}
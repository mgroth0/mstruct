

implementations {
  kjlibGit
  stream
}

implementations(
  libs.snakeyaml,
)

apis(
  ":k:klib".jvm(),
  ":k:file".jvm(),
)

plugins {
  kotlin("plugin.serialization")
}
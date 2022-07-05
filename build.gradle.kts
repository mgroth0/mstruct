

modtype = LIB

implementations(
  libs.snakeyaml,
  projects.k.kjlib.kjlibGit
)

apis(
  ":k:klib".jvm(),
  ":k:file".jvm(),
)
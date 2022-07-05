

modtype = LIB

implementations(
  libs.snakeyaml,
  projects.k.kjlib.git
)

apis(
  ":k:klib".jvm(),
  ":k:file".jvm(),
)
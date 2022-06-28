

import matt.klib.str.upper
modtype = LIB

apis(
  ":k:klib".jvm(),
  ":k:file".jvm()
)

dependencies {
  implementation("org.yaml:snakeyaml:1.28")
}


import matt.klib.str.upper
modtype = LIB
dependencies {
  implementation("org.yaml:snakeyaml:1.28")
  if (rootDir.name.upper() == "FLOW") {
    api(project(":k:klib")) {
      targetConfiguration = "jvmRuntimeElements"
    }
  } else {
    api("matt.k:klib:+")
  }
}
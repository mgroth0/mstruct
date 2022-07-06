import kotlinx.html.currentTimeMillis

val start = currentTimeMillis()

println("1:${currentTimeMillis()-start}")

implementations {
  println("2:${currentTimeMillis()-start}")
  kjlibGit
  println("3:${currentTimeMillis()-start}")
  stream
  println("4:${currentTimeMillis()-start}")
}
println("5:${currentTimeMillis()-start}")

implementations(
  libs.snakeyaml,
)

println("6:${currentTimeMillis()-start}")

apis(
  ":k:klib".jvm(),
  ":k:file".jvm(),
)

println("7:${currentTimeMillis()-start}")

plugins {
  val pluginStart = System.currentTimeMillis()
  println("8(plugin):${System.currentTimeMillis()-pluginStart}")
  kotlin("plugin.serialization")
  println("9(plugin):${System.currentTimeMillis()-pluginStart}")
}

println("10:${currentTimeMillis()-start}")
println("11:${currentTimeMillis()-start}")
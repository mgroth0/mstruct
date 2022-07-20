package matt.mstruct.kt

import matt.file.UnnamedPackageIsOk

fun unnamedKt(s: String) = """
  @file:${UnnamedPackageIsOk::class.simpleName}
  
  import ${UnnamedPackageIsOk::class.qualifiedName}
  
  $s
""".trimIndent()
apis {
  klib
  file
}

implementations {
  kjlibGit
  stream
  libs.apply {
	toml
	snakeyaml
	`kotlinx-serialization-json`
	`kt-reflect`
  }
}
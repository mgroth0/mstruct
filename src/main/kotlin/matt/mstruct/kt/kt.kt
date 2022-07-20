package matt.mstruct.kt

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.KtLint.ExperimentalParams
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import matt.file.UnnamedPackageIsOk
import matt.klib.lang.err

fun unnamedKt(s: String) = """
  @file:${UnnamedPackageIsOk::class.simpleName}
  
  import ${UnnamedPackageIsOk::class.qualifiedName}
  
  $s
""".trimIndent()

private val ktFormatRules by lazy {
  listOf(
	StandardRuleSetProvider().get()
  )
}

fun formatKotlinCode(kt: String) {
  KtLint.format(
	ExperimentalParams(
	  text = kt,
	  cb = { e: LintError, corrected: Boolean ->
		println("LINT ERROR: $e")
		println("corrected=${corrected}")
		err("there was a lint error")
	  },
	  ruleSets = ktFormatRules
	)
  )


}
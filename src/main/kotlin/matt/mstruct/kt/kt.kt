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

fun String.formatKotlinCode() = KtLint.format(
  ExperimentalParams(
	text = this,
	cb = { e: LintError, corrected: Boolean ->
	  warn("LINT ERROR: $e")
	  warn("corrected=${corrected}")
	},
	ruleSets = ktFormatRules
  )
)


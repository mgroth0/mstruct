package matt.mstruct.kt

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.KtLint.ExperimentalParams
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import kotlin.reflect.KClass

private val STD_RULE_SET by lazy { StandardRuleSetProvider().get() }
private val STD_RULE_SETS by lazy { listOf(STD_RULE_SET) }

fun String.formatKotlinCode(
  removeRules: List<String> = listOf()
) = KtLint.format(
  //  EnumEntryNameCaseRule
  ExperimentalParams(
	text = this,
	cb = { _: LintError, _: Boolean ->
	  /*doing nothing here because these seem to be non actionable*/
	  /*warn("LINT ERROR: $e")
	  warn("corrected=${corrected}")*/
	},
	ruleSets = listOf(
	  RuleSet(
		STD_RULE_SET.id, /*i wanted to give it my own unique ID, but this produced the following warning:

		[WARN] Skipping rule with id 'my-rule-set:trailing-comma' as it requires that the rule with id 'standard:indent' is loaded. However, no rule with this id is loaded.

		*/
		*STD_RULE_SET.rules.filter { it.id !in removeRules }.toTypedArray()
	  )
	)
  )
)


data class KotlinCode(
  val fileAnnotations: List<KClass<out Annotation>> = listOf(),
  val packageStatement: String = "",
  val imports: String = "",
  val code: String = ""
) {
  override fun toString(): String {
	return """
	  ${fileAnnotations.joinToString("\n") { "@file:${it.simpleName}" }}
	  $packageStatement
	  ${fileAnnotations.joinToString("\n") { "import ${it.qualifiedName}" }}
	  $imports
	  $code
	""".trimIndent()
  }
}


fun safeKtName(s: String) = if ("-" in s || " " in s || s[0].isDigit()) "`$s`" else s
package matt.mstruct.kt

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.KtLint.ExperimentalParams
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import matt.file.UnnamedPackageIsOk

fun unnamedKt(s: String) = """
  @file:${UnnamedPackageIsOk::class.simpleName}
  
  import ${UnnamedPackageIsOk::class.qualifiedName}
  
  $s
""".trimIndent()

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
		"my-rule-set",
		*STD_RULE_SET.rules.filter { it.id !in removeRules }.toTypedArray()
	  )
	)
  )
)


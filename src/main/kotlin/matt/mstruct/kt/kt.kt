package matt.mstruct.kt

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.KtLint.ExperimentalParams
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.ruleset.standard.EnumEntryNameCaseRule
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import matt.file.UnnamedPackageIsOk
import matt.klib.log.warn

fun unnamedKt(s: String) = """
  @file:${UnnamedPackageIsOk::class.simpleName}
  
  import ${UnnamedPackageIsOk::class.qualifiedName}
  
  $s
""".trimIndent()

private val STD_RULE_SET by lazy { StandardRuleSetProvider().get() }
private val STD_RULE_SETS by lazy { listOf(STD_RULE_SET) }

fun String.formatKotlinCode(
  removeRules: List<Rule> = listOf()
) = KtLint.format(
  ExperimentalParams(
	text = this,
	cb = { e: LintError, corrected: Boolean ->

	  warn("LINT ERROR: $e")
	  warn("corrected=${corrected}")
	},
	ruleSets = listOf(
	  RuleSet(
		"MY-RULE-SET",
		*STD_RULE_SET.rules.filter { it !in removeRules }.toTypedArray()
	  )
	)
  )
)


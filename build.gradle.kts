import matt.file.kt
import matt.klib.lang.cap

//generateKt(
//  matt.mstruct.SourceSets.main,
//  "gen".kt,
//  run {
//	matt.mstruct.KotlinCode(
//
//	)
//  }
//
//) {
//
//  class GenProject(p: Project): Project by p {
//	val accessorName = path.substringAfter(":k:").split(":").withIndex().joinToString("") {
//	  if (it.index == 0) it.value else it.value.cap()
//	}
//  }
//
//  val mods = findProject(":k")!!.subprojects.filter {
//	it.modtype != ABSTRACT
//  }.map { GenProject(it) }
//
//
//  matt.mstruct.kt.unnamedKt(
//	"""
//		  enum class KSubProject(val path: String) {
//		     ${
//	  mods.joinToString(",") {
//		it.accessorName + "(\"${it.path}\")"
//	  }
//	}
//		  }
//		"""
//  )/*.formatKotlinCode(removeRules = listOf(com.pinterest.ktlint.ruleset.standard.EnumEntryNameCaseRule().id))*/
//
//
//}
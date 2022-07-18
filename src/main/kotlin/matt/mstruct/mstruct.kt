package matt.mstruct

import ModType
import MultiPlatformMod
import kotlinx.serialization.Serializable
import matt.file.MFile
import matt.file.commons.BUILD_JSON_NAME
import matt.file.commons.REGISTERED_FOLDER
import matt.file.commons.REL_LIBS_VERSIONS_TOML
import matt.file.commons.REL_ROOT_FILES
import matt.file.commons.RootProjects
import matt.file.commons.RootProjects.flow
import matt.file.commons.RootProjects.kcomp
import matt.file.commons.USER_HOME
import matt.file.mFile
import matt.kjlib.git.SimpleGit
import matt.klib.commons.thisMachine
import matt.klib.lang.NOT_IMPLEMENTED
import matt.klib.lang.err
import matt.klib.str.lower
import matt.klib.sys.Mac
import matt.klib.sys.OPEN_MIND
import matt.klib.sys.WINDOWS_11_PAR_WORK
import matt.mstruct.SourceSets.commonMain
import matt.mstruct.SourceSets.main
import matt.stream.recurse.recurse
import org.tomlj.Toml
import org.yaml.snakeyaml.Yaml
import kotlin.reflect.KClass


class PythonProject(val file: MFile) {
  val name: String
	get() = file.name
  val execCommand: Array<String>
	get() {
	  val theList = mutableListOf<String>()


	  theList.add(USER_HOME["miniconda3/envs/${execJson["env"]}/bin/python"].absolutePath)
	  theList.add("-u") /*and don't forget it!*/
	  theList.add(
		file.resolve("python").resolve((execJson["main"] as String).replace(".", "/") + ".py").absolutePath
	  )



	  return theList.toTypedArray()
	}
  val environmentalVars: Map<String, String>
	get() {
	  var PYTHONPATH = ":${file["python"].absolutePath}"

	  val deps = execJson["dependencies"] as Iterable<*>
	  for (d in deps) {
		d as String
		PYTHONPATH += ":${d}"
	  }
	  return mapOf("PYTHONPATH" to PYTHONPATH)
	}
  private val execJson: Map<String, Any>
	get() = Yaml().load(file["exec.yml"].readText())

  fun processBuilder() = ProcessBuilder(execCommand.toList()).apply {
	environment() += environmentalVars
  }
}


fun MFile.projectNameRelativeToRoot(root: RootProjects): String {
  return when {

	parentFile in root.subRootFolders + root.folder -> name

	root.subRootFolders.any { this in it }          -> {
	  val subRoot = root.subRootFolders.first { this in it }
	  relativeTo(subRoot).path.replace(MFile.separator, "-")
	}

	else                                            -> err("how to set name of ${this}?")
  }
}


const val STANDARD_GROUP_NAME_PREFIX = "matt.flow"


open class SubProject(arg: String, val root: RootProjects) {
  val nested = '.' in arg
  val names = arg.split(".")
  val pathRelativeToRoot = names.joinToString(MFile.separator)
  val fold = root.folder[pathRelativeToRoot]
  val modCategory = if (nested) names[0] else null
  val packPath = if (nested) arg.substringAfter('.') else null

  val groupName = when {
	nested && names.size == 1 -> "matt"
	nested                    -> (arrayOf("matt") + names.subList(1, names.size - 1)).joinToString(".") { it.lower() }
	else                      -> null
  }
  val name = if (nested) names.last() else null

  val mainPackage = if (nested) "$groupName.$name" else null
  val mainPackagePath = mainPackage?.replace('.', MFile.separatorChar)

  companion object {
	private const val urlPrefix = "https://github.com/mgroth0/"
  }

  val url = urlPrefix + when {
	nested -> names.subList(1, names.size).joinToString(MFile.separator)
	else   -> name
  }


  val buildGradleKts = fold["build.gradle.kts"]
  val buildJson = fold[BUILD_JSON_NAME]

  val git by lazy {
	SimpleGit(projectDir = fold, debug = true)
  }

  override fun toString() = "${SubProject::class.simpleName} $pathRelativeToRoot in $root project"
}

enum class SourceSets {
  main, test, resources, commonMain, jvmMain, jsMain
}

val ModType.mainSourceSet get() = if (this is MultiPlatformMod) commonMain.name else main.name

class NewSubMod(arg: String, root: RootProjects, type: ModType): SubProject(arg, root) {
  val kotlin = fold["src/${type.mainSourceSet}/kotlin"]
  val java = fold["src/${type.mainSourceSet}/java"]
}

@Serializable
sealed interface BuildJsonDependency {
  val cfg: String
}

@Serializable
class BuildJsonProjectDependency(
  override val cfg: String,
  val path: String
): BuildJsonDependency

@Serializable
class BuildJsonLibDependency(
  override val cfg: String,
  val key: String
): BuildJsonDependency

@Serializable
class BuildJsonModule(
  val modType: String = "ABSTRACT"
) {
  val dependencies = listOf<BuildJsonDependency>()
  fun realModType() = ModType::class.recurse {
	@Suppress("UNCHECKED_CAST")
	it.sealedSubclasses as Iterable<KClass<ModType>>?
  }.first { it.simpleName == modType }
}

private val toml by lazy {
  Toml.parse(
	(when (thisMachine) {
	  is Mac    -> flow.folder.resolve(REL_LIBS_VERSIONS_TOML)
	  OPEN_MIND -> mFile(OPEN_MIND.homeDir) + kcomp.name + REL_LIBS_VERSIONS_TOML
	  WINDOWS_11_PAR_WORK -> REL_LIBS_VERSIONS_TOML
	  else      -> NOT_IMPLEMENTED
	}).toPath()
  )
}
private val versionsTable by lazy { toml.getTable("versions")!! }

/*obviously this can be improved if needed
I also do this in buildSrc/build.gradle.kts
without calling this. It can't be helped easily.*/
fun tomlVersion(name: String) = versionsTable.getString(name)!!

val ROOT_FILES_EXTRACT_SCRIPT_REL = REL_ROOT_FILES + "extract.sh"

const val KBUILD_PROJ_PATH = ":k:kbuild"
val KBUILD_JAR = REGISTERED_FOLDER + "kbuild.jar"
package matt.mstruct

import ABSTRACT
import KSubProject
import ModType
import MultiPlatformMod
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import matt.file.JsonFile
import matt.file.MFile
import matt.file.commons.BUILD_JSON_NAME
import matt.file.commons.COMMON_LIBS_VERSIONS_FILE
import matt.file.commons.GRADLEW_NAME
import matt.file.commons.IdeProject
import matt.file.commons.IdeProject.all
import matt.file.commons.LIBS_VERSIONS_ONLINE_URL
import matt.file.commons.REGISTERED_FOLDER
import matt.file.commons.REL_ROOT_FILES
import matt.file.commons.USER_HOME
import matt.file.mFile
import matt.json.prim.loadJson
import matt.json.prim.save
import matt.kjlib.git.SimpleGit
import matt.kjlib.shell.ShellVerbosity
import matt.kjlib.shell.ShellVerbosity.Companion.STREAM
import matt.kjlib.shell.shell
import matt.klib.commons.GITHUB_USERNAME
import matt.klib.lang.err
import matt.klib.olist.BasicObservableList
import matt.klib.olist.withChangeListener
import matt.klib.prop.BasicProperty
import matt.klib.str.lower
import matt.mstruct.GradleTask.shadowJar
import matt.mstruct.SourceSets.commonMain
import matt.mstruct.SourceSets.main
import matt.stream.recurse.recurse
import org.tomlj.Toml
import org.yaml.snakeyaml.Yaml
import java.util.Properties
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


fun MFile.projectNameRelativeToRoot(root: IdeProject): String {
  return when {

	parentFile in root.subRootFolders + root.folder -> name

	root.subRootFolders.any { this in it }          -> {
	  val subRoot = root.subRootFolders.first { this in it }
	  relativeTo(subRoot).cpath.replace(MFile.separator, "-")
	}

	else                                            -> err("how to set name of ${this}?")
  }
}


const val STANDARD_GROUP_NAME_PREFIX = "matt.flow"


open class SubProject(arg: String, val root: IdeProject) {
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
	private const val urlPrefix = "https://github.com/${GITHUB_USERNAME}/"
  }

  val url = urlPrefix + when {
	nested -> names.subList(1, names.size).joinToString(MFile.separator)
	else   -> name
  }


  val buildGradleKts = fold["build.gradle.kts"]
  val buildJson = fold[BUILD_JSON_NAME]

  val git by lazy {
	SimpleGit(projectDir = fold, verbosity = STREAM)
  }

  override fun toString() = "${SubProject::class.simpleName} $pathRelativeToRoot in $root project"
}

enum class SourceSets {
  main, test, resources, commonMain, jvmMain, jsMain
}

val ModType.mainSourceSet get() = if (this is MultiPlatformMod) commonMain.name else main.name

class NewSubMod(arg: String, root: IdeProject, type: ModType): SubProject(arg, root) {
  val kotlin = fold["src/${type.mainSourceSet}/kotlin"]
  val java = fold["src/${type.mainSourceSet}/java"]
}

@Serializable
sealed class BuildJsonDependency {
  abstract val cfg: String
}

@Serializable
class BuildJsonProjectDependency(
  override val cfg: String,
  val path: String
): BuildJsonDependency() {
  override fun toString() = path
}

@Serializable
class BuildJsonLibDependency(
  override val cfg: String,
  val key: String
): BuildJsonDependency() {
  override fun toString() = key
}

@Serializable
class BuildJsonLibBundleDependency(
  override val cfg: String,
  val key: String
): BuildJsonDependency() {
  override fun toString() = key
}


@Suppress("UNCHECKED_CAST") val allModTypes by lazy {
  ModType::class.recurse(includeSelf = false) {
	it.sealedSubclasses as Iterable<KClass<ModType>>
  }.mapNotNull { it.objectInstance }.toList()
}


class BuildJsonModuleMutator(
  private val f: JsonFile
) {
  private var bj = f.loadJson<BuildJsonModule>()

  val modTypeProp = BasicProperty(bj.realModType()).withChangeListener {
	bj = bj.copy(modType = it::class.simpleName!!)
	f.save(bj, pretty = true)
  }
  var modType by modTypeProp

  val dependencies: BasicObservableList<BuildJsonDependency> = BasicObservableList(bj.dependencies).withChangeListener {
	@Suppress("UNCHECKED_CAST")
	bj = bj.copy(dependencies = it as List<BuildJsonDependency>)
	f.save(bj, pretty = true)
  }

  //  var dependencies
  //	get() = bj.dependencies
  //	set(value) {
  //	  bj = bj.copy(dependencies = value)
  //	  f.save(bj, pretty = true)
  //	}
}

@Serializable
data class BuildJsonModule(
  private val modType: String = ABSTRACT::class.simpleName!!,
  val dependencies: List<BuildJsonDependency> = listOf()
) {
  fun realModType() = allModTypes.first {
	it::class.simpleName == modType
  }
}

//private val ONLINE_LIBS_VERSIONS_TEXTTEXT by lazy { LIBS_VERSIONS_TOML_ONLINE.readText() }
//private val COMMON_LIBS_VERSIONS_FILE by lazy {  }


private val toml by lazy {
  COMMON_LIBS_VERSIONS_FILE
	.takeIf { it.exists() }
	?.let { Toml.parse(it.toPath()) }
	?: Toml.parse(LIBS_VERSIONS_ONLINE_URL.openStream())
  //  Toml.parse(
  //
  //	COMMON_LIBS_VERSIONS_FILE ?: ONLINE_LIBS_VERSIONS_TEXT.byteInputStream()
  //	/*
  //		(when (thisMachine) {
  //		  is Mac              -> flow.folder.resolve(REL_LIBS_VERSIONS_TOML).apply {
  //			warn("reliance on flow project has to go")
  //		  }
  //
  //		  OPEN_MIND           -> mFile(OPEN_MIND.homeDir) + kcomp.name + REL_LIBS_VERSIONS_TOML
  //		  WINDOWS_11_PAR_WORK -> kcomp.folder.resolve(REL_LIBS_VERSIONS_TOML)
  //		  else                -> NOT_IMPLEMENTED
  //		}).toPath()*/
  //  )
}
private val versionsTable by lazy { toml.getTable("versions")!! }
val librariesTable by lazy { toml.getTable("libraries")!! }
val bundlesTable by lazy { toml.getTable("bundles")!! }
val librariesTableAsJson by lazy {
  Json.decodeFromString<JsonObject>(
	librariesTable.toJson()
  )
}
val bundlesTableAsJson by lazy {
  Json.decodeFromString<JsonObject>(
	bundlesTable.toJson()
  )
}

/*obviously this can be improved if needed
I also do this in buildSrc/build.gradle.kts
without calling this. It can't be helped easily.*/
fun tomlVersion(name: String) = versionsTable.getString(name)!!

val ROOT_FILES_EXTRACT_SCRIPT_REL = REL_ROOT_FILES + "extract.sh"

val KBUILD_JAR = REGISTERED_FOLDER + "${KSubProject.kbuild.name}.jar"

val JAVA_HOME by lazy {
  mFile(
	Properties().apply {
	  load(
		(USER_HOME + ".gradle" + "gradle.properties").reader()
	  )
	}["org.gradle.java.home"].toString()
  )
}

fun IdeProject.gradle(task: String) = shell(
  (folder + GRADLEW_NAME).abspath,
  task,
  env = mapOf(
	"JAVA_HOME" to JAVA_HOME.abspath
  ),
  verbosity = STREAM,
  workingDir = folder
)


val MAIN_CONFIGS = listOf("api", "implementation", "compileOnly")

enum class GradleTask {
  shadowJar, run, kbuild
}

fun KSubProject.pathForTask(task: GradleTask) = "${path.removeSuffix(":")}:${task.name}"
val KSubProject.projectName get() = path.split(":").last()

fun IdeProject.execute(sub: KSubProject, task: GradleTask) = shell(
  (this.folder + GRADLEW_NAME).abspath,
  sub.pathForTask(task),
  workingDir = this.folder,
  env = mapOf(
	"JAVA_HOME" to matt.mstruct.JAVA_HOME.abspath
  ),
  verbosity = STREAM,
)
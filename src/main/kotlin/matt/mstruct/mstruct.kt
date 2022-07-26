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
import matt.file.commons.BIN_FOLDER
import matt.file.commons.BUILD_GRADLE_KTS_NAME
import matt.file.commons.BUILD_JSON_NAME
import matt.file.commons.COMMON_LIBS_VERSIONS_FILE
import matt.file.commons.GRADLEW_NAME
import matt.file.commons.GRADLE_PROPERTIES_FILE_NAME
import matt.file.commons.IdeProject
import matt.file.commons.LIBS_VERSIONS_ONLINE_URL
import matt.file.commons.REGISTERED_FOLDER
import matt.file.commons.REL_ROOT_FILES
import matt.file.commons.USER_HOME
import matt.file.mFile
import matt.json.prim.loadJson
import matt.json.prim.save
import matt.kjlib.git.SimpleGit
import matt.kjlib.shell.ShellVerbosity.Companion.STREAM
import matt.kjlib.shell.shell
import matt.klib.commons.GITHUB_USERNAME
import matt.klib.commons.thisMachine
import matt.klib.lang.NOT_IMPLEMENTED
import matt.klib.lang.err
import matt.klib.olist.BasicObservableList
import matt.klib.olist.withChangeListener
import matt.klib.prop.BasicProperty
import matt.klib.str.lower
import matt.klib.sys.Linux
import matt.klib.sys.Machine
import matt.klib.sys.NEW_MAC
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


  val buildGradleKts = fold + BUILD_GRADLE_KTS_NAME
  val buildJson = fold + BUILD_JSON_NAME

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

@Serializable sealed class BuildJsonDependency {
  abstract val cfg: String
}

@Serializable class BuildJsonProjectDependency(
  override val cfg: String, val path: String
): BuildJsonDependency() {
  override fun toString() = path
}

@Serializable class BuildJsonLibDependency(
  override val cfg: String, val key: String
): BuildJsonDependency() {
  override fun toString() = key
}

@Serializable class BuildJsonLibBundleDependency(
  override val cfg: String, val key: String
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
	bj = bj.copy(dependencies = it.collection as List<BuildJsonDependency>)
	f.save(bj, pretty = true)
  }

  //  var dependencies
  //	get() = bj.dependencies
  //	set(value) {
  //	  bj = bj.copy(dependencies = value)
  //	  f.save(bj, pretty = true)
  //	}
}

@Serializable data class BuildJsonModule(
  private val modType: String = ABSTRACT::class.simpleName!!, val dependencies: List<BuildJsonDependency> = listOf()
) {
  fun realModType() = allModTypes.first {
	it::class.simpleName == modType
  }
}

//private val ONLINE_LIBS_VERSIONS_TEXTTEXT by lazy { LIBS_VERSIONS_TOML_ONLINE.readText() }
//private val COMMON_LIBS_VERSIONS_FILE by lazy {  }


private val toml by lazy {
  COMMON_LIBS_VERSIONS_FILE.takeIf { it.exists() }?.let { Toml.parse(it.toPath()) } ?: Toml.parse(
	LIBS_VERSIONS_ONLINE_URL.openStream()
  ) //  Toml.parse(
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
		(USER_HOME + ".gradle" + GRADLE_PROPERTIES_FILE_NAME).reader()
	  )
	}["org.gradle.java.home"].toString()
  )
}

fun IdeProject.gradle(task: String) = shell(
  (folder + GRADLEW_NAME).abspath, task, env = mapOf(
	"JAVA_HOME" to JAVA_HOME.abspath
  ), verbosity = STREAM, workingDir = folder
)


val JVM_ONLY_CONFIGS = arrayOf("api", "implementation", "compileOnly")
val KOTLIN_MULTIPLATFORM_CONFIGS = arrayOf(

  "commonMainImplementation",
  "commonMainApi",

  "jvmMainImplementation",
  "jvmMainApi",

  "jsMainImplementation",
  "jsMainApi",

  "nativeMainImplementation",
  "nativeMainApi",
)

val ALL_CONFIGS = arrayOf(*JVM_ONLY_CONFIGS, *KOTLIN_MULTIPLATFORM_CONFIGS)

enum class GradleTask {
  shadowJar, run, kbuild
}

fun KSubProject.pathForTask(task: GradleTask) = "${path.removeSuffix(":")}:${task.name}"
val KSubProject.projectName get() = path.split(":").last()

fun IdeProject.execute(sub: KSubProject? = null, task: GradleTask) = shell(
  (this.folder + GRADLEW_NAME).abspath,
  sub?.pathForTask(task) ?: task.name,
  workingDir = this.folder,
  env = mapOf(
	"JAVA_HOME" to matt.mstruct.JAVA_HOME.abspath
  ),
  verbosity = STREAM,
)

val LINUX_GRADLE_EXECUTABLE_FILE by lazy { mFile("/opt/gradle/gradle-${tomlVersion("gradle")}/bin/gradle") }

val Machine.systemGradleExecutable: MFile
  get() {

	val gradleVersion = tomlVersion("gradle")

	return when (this) {
	  is NEW_MAC -> mFile(
		"/Users/matthewgroth/.gradle/wrapper/dists/gradle-$gradleVersion-all/6qsw290k5lz422uaf8jf6m7co/gradle-$gradleVersion/bin/gradle"
	  )

	  is Linux   -> LINUX_GRADLE_EXECUTABLE_FILE
	  else       -> NOT_IMPLEMENTED
	}
  }


val KSubProject.registeredBinDistFolder
  get() = BIN_FOLDER + "dist" + path.split(":").drop(2).joinToString(MFile.separator)

val KSubProject.registeredBinDistLibFolder get() = registeredBinDistFolder + "lib"
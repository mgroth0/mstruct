package matt.mstruct

import matt.klib.commons.RootProject
import matt.klib.commons.USER_HOME
import matt.klib.commons.get
import matt.klib.commons.plus
import matt.klib.file.MFile
import matt.klib.file.ext.resolve
import matt.klib.lang.err
import org.yaml.snakeyaml.Yaml


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


fun MFile.projectNameRelativeToRoot(root: RootProject): String {
  val kFold = root.folder + "k"
  val kJFold = root.folder + "KJ"
  return when {
	parentFile in listOf(root.folder, kFold, kJFold) -> name
	this in kFold                                    -> relativeTo(
	  kFold
	).path.replace(MFile.separator, "-")


	this in kJFold                                   -> relativeTo(
	  kJFold
	).path.replace(MFile.separator, "-")

	else                                             -> err("how to set name of ${this}?")
  }
}

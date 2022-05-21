package matt.mstruct

import matt.kbuild.USER_HOME
import matt.kjlib.file.get
import org.yaml.snakeyaml.Yaml
import java.io.File

class PythonProject(val file: File) {
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
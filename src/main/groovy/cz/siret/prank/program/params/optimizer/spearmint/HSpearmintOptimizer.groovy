package cz.siret.prank.program.params.optimizer.spearmint

import com.google.gson.Gson
import cz.siret.prank.program.params.optimizer.HObjectiveFunction
import cz.siret.prank.program.params.optimizer.HOptimizer
import cz.siret.prank.program.params.optimizer.HStep
import cz.siret.prank.program.params.optimizer.HVariable
import cz.siret.prank.utils.Futils
import cz.siret.prank.utils.ProcessRunner
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.file.Path

import static cz.siret.prank.utils.Futils.*

/**
 * optimizer based on https://github.com/HIPS/Spearmint
 */
@Slf4j
@CompileStatic
class HSpearmintOptimizer extends HOptimizer {

    enum Likelihood { NOISELESS, GAUSSIAN }

    String spearmintCommand = "python main.py"
    String mongodbCommand = "mongod"

    Likelihood likelihood = Likelihood.GAUSSIAN

    Path spearmintDir
    Path experimentDir

    /**
     * use absolute paths
     * @param spearmintDir
     * @param experimentDir
     */
    HSpearmintOptimizer(Path spearmintDir, Path experimentDir) {
        this.spearmintDir = spearmintDir
        this.experimentDir = experimentDir
    }

    @Override
    HStep optimize(HObjectiveFunction objective) {

        String dir = experimentDir.toString()
        delete(dir)
        mkdirs(dir)
        mkdirs("$dir/vars")
        mkdirs("$dir/eval")
        writeFile "$dir/config.json", genConfig()
        writeFile "$dir/eval.py", genEval()

        String mongoLogFile = Futils.absSafePath("$dir/mongo/mongo.log" )
        String mongoDataDir = Futils.absSafePath( "$dir/mongo/data" )
        mkdirs(mongoDataDir)

        // run mongo
        log.info("Starting mongodb")
        String mcmd = "$mongodbCommand --fork --logpath $mongoLogFile --dbpath $mongoDataDir"
        ProcessRunner mongoProc = new ProcessRunner(mcmd, dir).redirectErrorStream().redirectOutput(new File("$dir/mongo/mongo.out"))
        mongoProc.execute().waitFor()


        // run spearmint
        log.info("Starting spearmint")
        String scmd = spearmintCommand + " " + dir
        ProcessRunner spearmintProc = new ProcessRunner(scmd, spearmintDir.toString()).redirectErrorStream().redirectOutput(new File("$dir/spearmint.out"))
        spearmintProc.execute()

        int stepNumber = 0
        int jobId          // spearmint job id, may start at any number

        // find starting speramint job id
        String varsDir = "$dir/vars"
        log.debug "waiting for first vars file in '$varsDir'"
        while (isDirEmpty("$dir/vars")) {
            log.debug "waiting..."
            sleep(1000)
        }
        jobId = listFiles(varsDir).first().name.toInteger()

        String stepsf = "$dir/steps.csv"
        List<String> varNames = (variables*.name).toList()
        writeFile stepsf, "[num], [job_id], " + varNames.join(", ") + ", [value]"

        while (stepNumber < maxIterations) {
            log.info "job id: {}", jobId
            String varf = "$varsDir/$jobId"
            waitForFile(varf)

            // parse variable assignment
            Map<String, Object> vars = new Gson().fromJson(new File(varf).text, Map.class);
            log.info "vars: {}", vars

            // eval objective function
            double val = objective.eval(vars, stepNumber)
            log.info "value: {}", val

            HStep step = new HStep(stepNumber, vars, val)
            steps.add(step)
            append stepsf, "$stepNumber, $jobId, " + varNames.collect { vars.get(it) }.join(", ") + ", $val"

            stepNumber++
            jobId++
        }

        spearmintProc.kill()
        mongoProc.kill()

        return getMaxStep()
    }

    HStep getMaxStep() {
        assert !steps.isEmpty()

        steps.max { it.functionValue }
    }


    void waitForFile(String fname) {
        log.debug "waiting for file '$fname'"
        while (!exists(fname)) {
            log.debug "waiting..."
            sleep(1000)
        } 
    }

    private String genConfig() {

        """
        {
            "language"        : "PYTHON",
            "main-file"       : "eval.py",
            "experiment-name" : "spearmint_experiment",
            "likelihood"      : "$likelihood",
            "variables" : {
                ${genVariables()}
            }
        }
        """

    }

    private String genVariable(HVariable var) {
        """
                "$var.name" : {
                    "type" : "$var.type",
                    "size" : 1,
                    "min"  : $var.min,
                    "max"  : $var.max
                }
        """
    }

    private String genVariables() {
        variables.collect { genVariable(it) }.join(",\n")
    }

    private String genEval() {
        """
import numpy as np
import math
import os.path
import time
import json
import os

def eval(job_id, variables):

    valf = "eval/" + str(job_id)

    # wait until value file exists
    while not os.path.exists(valf):
        time.sleep(1)

    with open(valf) as file:
        value = file.read()
        
    return float(value)

def main(job_id, variables):
    print "variables: " + str(variables)

    # prepare vars for java
    vars = {}
    for key, value in variables.iteritems():
        vars[key] = value[0]
    print "vars: " + json.dumps(vars)
    if not os.path.exists("vars"):
        os.makedirs("vars")
    varf = "vars/" + str(job_id) + ".json"
    with open(varf, "w") as file:
        file.write(json.dumps(vars))

    return eval(job_id, variables)
    
        """
    }

    /*
$ tree
.
├── branin.py
├── config.json
├── eval
│   ├── 78
│   └── 79
├── output
│   ├── 00000078.out
│   ├── 00000079.out
│   └── 00000080.out
└── vars
    ├── 78.json
    ├── 79.json
    └── 80.json
     */

}

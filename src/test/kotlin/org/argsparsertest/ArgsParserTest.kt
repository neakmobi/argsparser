package org.argsparsertest

import org.junit.Test
import org.argsparser.*

private class TestConfiguration {

    var accepted = Array(4) { false }
    var keyPrior = 0
    var multiParamOptionPrior = 1
    var unfixedParamOptionPrior = 2
    var programParamPrior = 3

    var optionMultiParam = mutableListOf("param1", "param2", "param3", "param4")
    var optionUnfixParam = mutableListOf("paramA", "paramB", "paramC")
    var programParam = mutableListOf("pp1", "pp2", "pp3", "pp4", "pp5", "-s", "-p", "-d", "-e")

    private fun checkAccepted(n: Int): Boolean {
        for (idx in 0 until n)
            if (!accepted[idx]) return false
        return true
    }

    private fun compareParams(params0: MutableList<String>, params1: MutableList<String>): Boolean {
        if (params0.size != params1.size) return false
        for (idx in params0.indices)
            if (params0[idx] != params1[idx])
                return false
        return true
    }

    fun applyKey(): Boolean {
        assert(checkAccepted(keyPrior))
        accepted[keyPrior] = true
        return true
    }

    fun applyMultiParamOption(params: MutableList<String>): Boolean {
        assert(checkAccepted(multiParamOptionPrior))
        assert(compareParams(optionMultiParam, params))
        accepted[multiParamOptionPrior] = true
        return true
    }

    fun applyUnfixParamOption(params: MutableList<String>): Boolean {
        assert(checkAccepted(unfixedParamOptionPrior))
        assert(compareParams(optionUnfixParam, params))
        accepted[unfixedParamOptionPrior] = true
        return true
    }

    fun applyProgramParam(params: MutableList<String>): Boolean {
        assert(checkAccepted(programParamPrior))
        assert(compareParams(programParam, params))
        accepted[programParamPrior] = true
        return true
    }


}

class ArgsParserTest {
    @Test fun test() {
        val conf = TestConfiguration()
        val args = "pp1 pp2 -spdhek -o param1 param2 param3 param4 pp3 pp4 pp5 -u paramA paramB paramC".split(" ").toTypedArray()
        val parser = ArgsParser(
                "Test Program",
                "v.1.0",
                "Help preamble",
                "Help conclusion",
                conf::applyProgramParam,
                "Help usage",
                "Help options"
        )
        parser.addOption(Key(
                "-k",
                "--key",
                "Key option",
                conf.keyPrior,
                true,
                conf::applyKey
        ))
        parser.addOption(FixParamsOption(
                "-o",
                "--option",
                "Option with fix params",
                conf.multiParamOptionPrior,
                true,
                conf.optionMultiParam.size,
                conf::applyMultiParamOption
        ))
        parser.addOption(UnfixParamsOption(
                "-u",
                "--unfix",
                "Option with unfix params",
                conf.unfixedParamOptionPrior,
                true,
                conf::applyUnfixParamOption
        ))
        val parseResult = parser.parseArgs(args)
        assert(parseResult == ParseResult.OK || parseResult == ParseResult.HELP_REQUESTED)
    }

}
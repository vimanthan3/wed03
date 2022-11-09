package configurations

import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.annotation.JSONField
import common.functionalTestExtraParameters
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.parallelTests
import model.CIBuildModel
import model.Stage
import model.StageName
import model.TestCoverage

const val functionalTestTag = "FunctionalTest"

sealed class ParallelizationMethod {
    @get: JSONField(serialize = false)
    open val extraBuildParameters: String = ""
    val name: String = this::class.simpleName!!

    object None : ParallelizationMethod()
    object TestDistribution : ParallelizationMethod() {
        override val extraBuildParameters: String = "-DenableTestDistribution=%enableTestDistribution% -DtestDistributionPartitionSizeInSeconds=%testDistributionPartitionSizeInSeconds%"
    }

    class TeamCityParallelTests(val numberOfBuckets: Int) : ParallelizationMethod()

    companion object {
        fun fromJson(jsonObject: JSONObject): ParallelizationMethod {
            val nameJsonObject = jsonObject.getJSONObject("parallelizationMethod") ?: return None
            return when (nameJsonObject.getString("name")) {
                null -> None
                TestDistribution::class.simpleName -> TestDistribution
                TeamCityParallelTests::class.simpleName -> TeamCityParallelTests(jsonObject.getIntValue("numberOfBuckets"))
                else -> throw IllegalArgumentException("Unknown parallelization method")
            }
        }
    }
}

class FunctionalTest(
    model: CIBuildModel,
    id: String,
    name: String,
    description: String,
    val testCoverage: TestCoverage,
    stage: Stage,
    parallelizationMethod: ParallelizationMethod = ParallelizationMethod.None,
    subprojects: List<String> = listOf(),
    extraParameters: String = "",
    extraBuildSteps: BuildSteps.() -> Unit = {},
    preBuildSteps: BuildSteps.() -> Unit = {}
) : BaseGradleBuildType(stage = stage, init = {
    this.name = name
    this.description = description
    this.id(id)
    val testTasks = getTestTaskName(testCoverage, subprojects)

    val assembledExtraParameters = mutableListOf(
        functionalTestExtraParameters(functionalTestTag, testCoverage.os, testCoverage.arch, testCoverage.testJvmVersion.major.toString(), testCoverage.vendor.name),
        "-PflakyTests=${determineFlakyTestStrategy(stage)}",
        extraParameters,
        when (parallelizationMethod) {
            is ParallelizationMethod.TestDistribution -> "-DenableTestDistribution=%enableTestDistribution% -DtestDistributionPartitionSizeInSeconds=%testDistributionPartitionSizeInSeconds%"
            else -> ""
        }
    ).filter { it.isNotBlank() }.joinToString(separator = " ")

    if (parallelizationMethod is ParallelizationMethod.TeamCityParallelTests) {
        features {
            parallelTests {
                this.numberOfBatches = parallelizationMethod.numberOfBuckets
            }
        }
    }

    applyTestDefaults(
        model, this, testTasks,
        dependsOnQuickFeedbackLinux = !testCoverage.withoutDependencies && stage.stageName > StageName.PULL_REQUEST_FEEDBACK,
        os = testCoverage.os,
        buildJvm = testCoverage.buildJvm,
        arch = testCoverage.arch,
        extraParameters = assembledExtraParameters,
        timeout = testCoverage.testType.timeout,
        extraSteps = extraBuildSteps,
        preSteps = preBuildSteps
    )

    failureConditions {
        // JavaExecDebugIntegrationTest.debug session fails without debugger might cause JVM crash
        // Some soak tests produce OOM exceptions
        // There are also random worker crashes for some tests.
        // We have test-retry to handle the crash in tests
        javaCrash = false
    }
})

private fun determineFlakyTestStrategy(stage: Stage): String {
    val stageName = StageName.values().first { it.stageName == stage.stageName.stageName }
    // See gradlebuild.basics.FlakyTestStrategy
    return if (stageName < StageName.READY_FOR_RELEASE) "exclude" else "include"
}

fun getTestTaskName(testCoverage: TestCoverage, subprojects: List<String>): String {
    val testTaskName = "${testCoverage.testType.name}Test"
    return when {
        subprojects.isEmpty() -> {
            testTaskName
        }

        else -> {
            subprojects.joinToString(" ") { "$it:$testTaskName" }
        }
    }
}

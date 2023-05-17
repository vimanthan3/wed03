package projects

import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

class GradleBuildToolRootProject() : Project({
    buildType {
        id("reproducer")
        name = "reproducer"
        requirements {
            contains("teamcity.agent.jvm.os.name", "Linux")
        }
        steps {
            gradle {
                tasks = ":plugin-development:configCacheIntegTest --tests ValidatePluginsIntegrationTest"
                useGradleWrapper = true
            }
        }
    }
})

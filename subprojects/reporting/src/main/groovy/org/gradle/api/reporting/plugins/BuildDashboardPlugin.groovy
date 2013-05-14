/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.gradle.api.reporting.plugins

import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.reporting.DirectoryReport
import org.gradle.api.reporting.GenerateBuildDashboard
import org.gradle.api.reporting.Reporting
import org.gradle.api.reporting.ReportingExtension

/**
 * <p>A {@link Plugin} which allows to generate build dashboard report.</p>
 */
@Incubating
public class BuildDashboardPlugin implements Plugin<ProjectInternal> {
    public static final String BUILD_DASHBOARD_TASK_NAME = "buildDashboard"

    public void apply(ProjectInternal project) {
        project.plugins.apply(ReportingBasePlugin)

        GenerateBuildDashboard buildDashboardTask = project.tasks.create(BUILD_DASHBOARD_TASK_NAME, GenerateBuildDashboard)

        project.allprojects.each {
            aggregateReportings(it, buildDashboardTask)
            it.tasks.withType(Reporting).matching { task ->
                task != buildDashboardTask
            }.all { task ->
                task.finalizedBy(buildDashboardTask)
            }
        }
        addReportDestinationConventionMapping(project, buildDashboardTask.reports.html);
    }

    private void addReportDestinationConventionMapping(ProjectInternal project, DirectoryReport buildDashboardReport) {
        buildDashboardReport.conventionMapping.map('destination') {
            project.extensions.getByType(ReportingExtension).file('buildDashboard')
        }
    }

    private void aggregateReportings(Project project, GenerateBuildDashboard buildDashboardTask) {
        project.tasks.withType(Reporting).all {
            buildDashboardTask.aggregate(it)
        }
    }
}

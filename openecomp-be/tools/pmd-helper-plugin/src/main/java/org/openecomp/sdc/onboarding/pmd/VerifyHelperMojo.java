/*
 * Copyright © 2018 European Support Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on a "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openecomp.sdc.onboarding.pmd;

import static org.openecomp.sdc.onboarding.pmd.PMDHelperUtils.getStateFile;
import static org.openecomp.sdc.onboarding.pmd.PMDHelperUtils.readCurrentPMDState;
import static org.openecomp.sdc.onboarding.pmd.PMDHelperUtils.writeCurrentPMDState;
import static org.openecomp.sdc.onboarding.pmd.PMDHelperUtils.isReportEmpty;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "post-verify-helper", threadSafe = true, defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.NONE)
public class VerifyHelperMojo extends AbstractMojo {

    private static final String SKIP_PMD = "skipPMD";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    @Parameter(defaultValue = "${project.artifact.groupId}:${project.artifact.artifactId}")
    private String moduleCoordinates;
    @Parameter(defaultValue = "${session}")
    private MavenSession session;
    @Parameter
    private File pmdTargetLocation;
    @Parameter
    private File pmdReportFile;
    @Parameter
    private File pmdStateFile;
    @Parameter
    private String pmdCurrentStateFilePath;
    @Parameter
    private String excludePackaging;
    @Parameter
    private Boolean validatePMDReport = Boolean.FALSE;
    @Parameter
    private String persistingModuleCoordinates;
    @Parameter
    private File pmdFailureReportLocation;
    @Parameter
    private File compiledFilesList;
    @Parameter
    private File compiledTestFilesList;

    private static File pmdCurrentStateFile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.getPackaging().equals(excludePackaging)) {
            return;
        }
        if (moduleCoordinates.equals(persistingModuleCoordinates)) {
            if (pmdStateFile.exists()) {
                pmdStateFile.delete();
            }
        }
        if (pmdCurrentStateFile == null) {
            pmdCurrentStateFile =
                    getStateFile(pmdCurrentStateFilePath.substring(0, pmdCurrentStateFilePath.indexOf('/')), project,
                            pmdCurrentStateFilePath);
            pmdCurrentStateFile.getParentFile().mkdirs();
            pmdReportFile.getParentFile().mkdirs();
        }
        if (PMDState.getHistoricState() != null && PMDState.getHistoricState().isEmpty()) {
            getLog().error("PMD Check is skipped. problem while loading data.");
        }
        if (Boolean.FALSE.equals(Boolean.valueOf(project.getProperties().getProperty(SKIP_PMD))) && !isReportEmpty(pmdReportFile)) {
            Map<String, List<Violation>> data = readCurrentPMDState(pmdCurrentStateFile);
            Map<String, List<Violation>> cv = readCurrentModulePMDReport();
            data.putAll(cv);
            if (!PMDState.getHistoricState().isEmpty() && !PMDHelperUtils
                                                                   .evaluateCodeQuality(PMDState.getHistoricState(), cv,
                                                                           pmdFailureReportLocation, getLog())) {
                if (validatePMDReport) {
                    throw new MojoFailureException(
                            "PMD Failures encountered. Build halted. For details refer " + pmdFailureReportLocation
                                                                                                   .getAbsolutePath());
                } else {
                    getLog().error(
                            "\u001B[31m\u001B[1m Code Quality concerns raised by Quality Management System. For details refer "
                                    + pmdFailureReportLocation.getAbsolutePath()
                                    + " and address them before committing this code in Version Control System. \u001B[0m");
                }
            }
            Map<String, Object> checksumStore = HashMap.class.cast(data);
            checksumStore.put(moduleCoordinates,
                    project.getProperties().getProperty("mainChecksum") + ":" + project.getProperties()
                                                                                       .getProperty("testChecksum"));
            writeCurrentPMDState(pmdCurrentStateFile, data);
        }
        if (Boolean.FALSE.equals(Boolean.valueOf(project.getProperties().getProperty(SKIP_PMD)))) {
            if (isReportEmpty(pmdReportFile)){
                HashMap data = HashMap.class.cast(readCurrentPMDState(pmdCurrentStateFile));
                data.put(moduleCoordinates,
                        project.getProperties().getProperty("mainChecksum") + ":" + project.getProperties()
                                                                                           .getProperty("testChecksum"));
                writeCurrentPMDState(pmdCurrentStateFile, data);
            }
            pmdReportFile.delete();
        }
        if (pmdTargetLocation.exists()) {
            pmdTargetLocation.delete();
        }

    }

    private Map<String, List<Violation>> readCurrentModulePMDReport() {
        try {
            PMDState.reset(compiledFilesList, compiledTestFilesList, moduleCoordinates);
            if (pmdReportFile.exists()) {
                boolean isFirst = true;
                for (String line : Files.lines(pmdReportFile.toPath()).collect(Collectors.toList())) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        PMDState.addViolation(line, moduleCoordinates);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return PMDState.getState();
    }

}

package com.browserstack.automate.ci.jenkins;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logDebug;


import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;

import com.browserstack.automate.ci.common.AutomateTestCase;
import com.browserstack.automate.ci.common.analytics.Analytics;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;


public class AutomateTestDataPublisher extends TestDataPublisher {
    private static final String TAG = "[BrowserStack]";
    private static final String REPORT_FILE_PATTERN = "**/browserstack-reports/REPORT-*.xml";

    @Extension(ordinal = 1000) // JENKINS-12161
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public AutomateTestDataPublisher() {
        // This constructor is only called when the TestDataPublisher is created.
        // This is only when the user explicitly chooses to enable BrowserStack as an additional Test report.
        Analytics.trackReportingEvent(true);
    }

    @Override
    public TestResultAction.Data getTestData(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener, TestResult testResult) throws IOException, InterruptedException {
        return contributeTestData(abstractBuild, abstractBuild.getWorkspace(), launcher, buildListener, testResult);
    }

    @Override
    public TestResultAction.Data contributeTestData(Run<?, ?> run, @Nonnull FilePath workspace,
                                                    Launcher launcher, TaskListener listener,
                                                    TestResult testResult) throws IOException, InterruptedException {
        log(listener.getLogger(), "Publishing test results");
        Map<String, String> testSessionMap = workspace.act(new BrowserStackReportFileCallable(REPORT_FILE_PATTERN, run.getTimeInMillis()));
        AutomateActionData automateActionData = new AutomateActionData();
        Map<String, Long> testCaseIndices = new HashMap<String, Long>();

        int testCount = 0;
        int sessionCount = 0;

        if (testResult != null) {
            for (SuiteResult suiteResult : testResult.getSuites()) {
                List<CaseResult> cases = suiteResult.getCases();
                testCount += cases.size();
                logDebug(listener.getLogger(), suiteResult.getName() + ": " + cases.size() + " test cases found.");

                for (CaseResult caseResult : cases) {
                    String testCaseName = getTestCaseName(caseResult);

                    Long testIndex = testCaseIndices.containsKey(testCaseName) ? testCaseIndices.get(testCaseName) : -1L;
                    testCaseIndices.put(testCaseName, ++testIndex);
                    logDebug(listener.getLogger(), testCaseName + " / " + testCaseName + " <=> " + testIndex);

                    String testId = String.format("%s{%d}", testCaseName, testIndex);
                    if (testSessionMap.containsKey(testId)) {
                        AutomateTestAction automateTestAction = new AutomateTestAction(run, caseResult, testSessionMap.get(testId));
                        automateActionData.registerTestAction(caseResult.getId(), automateTestAction);
                        logDebug(listener.getLogger(), "registerTestAction: " + testId + " => " + automateTestAction);
                        sessionCount++;
                    }
                }
            }

            testCaseIndices.clear();
            log(listener.getLogger(), testCount + " tests recorded");
            log(listener.getLogger(), sessionCount + " sessions captured");
            log(listener.getLogger(), "Publishing test results: SUCCESS");
            return automateActionData;
        } else {

            List<String[]> parsedValues = new ArrayList<String[]>();
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(run.getLogInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    String [] parsedValue = findSessionAndJobId(line);

                    if(parsedValue[0] != null && parsedValue[1] != null){
                      parsedValues.add(parsedValue);
                    }
                }
            } catch (IOException e) {
                System.out.println("EXCEPTION  " + e.getMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            for (String[] parsedValue : parsedValues) {
                automateActionData.registerTestAction(parsedValue[1], new AutomateTestAction(run,null,parsedValue[0]));
                 System.out.println(" SessionId = " + parsedValue[0] + " JobId = " + parsedValue[1]);
                 AutomateBuildAction buildAction = new AutomateBuildAction(parsedValue[0], parsedValue[1]);
                 run.addAction(buildAction);
            }

            return automateActionData;
        }

    }


    private static String[] findSessionAndJobId(String line){
          String[] parsedValues = new String[2];
          String pattern = "browserstack:sessionId:(([a-z]|\\d){40}):buildId:(.*$)";
          Pattern r = Pattern.compile(pattern);
          Matcher m = r.matcher(line);
        
          if (m.find( )) {
              parsedValues[0] = m.group(1);
              parsedValues[1] = m.group(3);
          } else {
              parsedValues[0] = null;
              parsedValues[1] = null;
          }
        return parsedValues;
    }


    public static String getTestCaseName(CaseResult caseResult) {
        return caseResult.getClassName() + "." + AutomateTestCase.stripTestParams(caseResult.getDisplayName());
    }

    private static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Embed BrowserStack Report";
        }
    }
}

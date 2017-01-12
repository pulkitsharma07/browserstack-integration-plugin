package com.browserstack.automate.ci.jenkins;

import static com.browserstack.automate.ci.common.logger.PluginLogger.log;
import static com.browserstack.automate.ci.common.logger.PluginLogger.logDebug;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.util.DescribableList;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public class AutomateTestDataPublisherX extends Recorder implements SimpleBuildStep {

  @DataBoundConstructor
  public AutomateTestDataPublisherX(){
    super();
  }

  public BuildStepMonitor getRequiredMonitorService() {
      return BuildStepMonitor.NONE;
  }

  @Override
  public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

      TestResultAction report = run.getAction(TestResultAction.class);
      TestResult result = report.getResult();

      for (SuiteResult suiteResult : result.getSuites()) {
        System.out.println("first " + suiteResult.getStdout());
      }
  }


  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

      AutomateTestDataPublisher atdp = new AutomateTestDataPublisher();
      TestResultAction report = build.getAction(TestResultAction.class);
      if (report != null) {        
        List<TestResultAction.Data> data = new ArrayList<TestResultAction.Data>();

        TestResult result = report.getResult();

        TestResultAction.Data d = atdp.getTestData(build, launcher, listener, result);

        data.add(d);
        report.setData(data);
        build.save();
      } else {
        TestResultAction.Data d = atdp.getTestData(build, launcher, listener, null);
      }
      return true;
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

      @Override
      public String getDisplayName() {
          return "Run BrowserStack Test Publisher";
      }

      @Override
      public boolean isApplicable(Class<? extends AbstractProject> jobType) {
          return !TestDataPublisher.all().isEmpty();
      }
  }

}
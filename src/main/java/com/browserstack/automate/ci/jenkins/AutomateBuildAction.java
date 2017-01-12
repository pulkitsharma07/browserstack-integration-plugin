package com.browserstack.automate.ci.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.Action;

/**
 * Created by prit8976 on 8/27/15.
 */
public class AutomateBuildAction implements Action {

    private String sessionId;
    private String jobId;

    @Override
    public String getIconFileName() {
        return "/plugin/testExample/img/build-goals.png";
    }

    @Override
    public String getDisplayName() {
        return "Test Example Build Page";
    }

    @Override
    public String getUrlName() {
        return "testExampleBA";
    }

    public String getMessage() {
        return this.sessionId;
    }

    public String getBuildNumber() {
        return this.jobId;
    }


    AutomateBuildAction(final String SessionId, final String JobId)
    {
        this.sessionId = SessionId;
        this.jobId = JobId;
    }
}
package com.deskflow.ai;

public interface TriageClient {

    TriageResult triage(String title, String description);
}

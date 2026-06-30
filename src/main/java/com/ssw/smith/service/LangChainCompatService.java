package com.ssw.smith.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssw.smith.model.entity.RunEntity;
import jakarta.servlet.http.Part;

import java.util.Collection;
import java.util.Map;

public interface LangChainCompatService {

    Map<String, Object> submitMetadata(JsonNode body);

    RunEntity createRun(JsonNode body);

    RunEntity patchRun(String runId, JsonNode body);

    void ingestRunsMultipart(Collection<Part> parts);
}
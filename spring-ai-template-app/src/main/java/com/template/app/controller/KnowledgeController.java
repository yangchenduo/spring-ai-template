package com.template.app.controller;

import com.template.app.service.KnowledgeService;
import com.template.common.dto.KnowledgeDoc;
import com.template.common.dto.KnowledgeInfo;
import com.template.common.dto.KnowledgeUploadRequest;
import com.template.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Knowledge", description = "Knowledge Base Management API")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "Create a new knowledge base")
    @PostMapping
    public Result<KnowledgeInfo> create(@RequestParam String name,
                                        @RequestParam(required = false) String description) {
        return Result.ok(knowledgeService.createKnowledge(name, description));
    }

    @Operation(summary = "List all knowledge bases")
    @GetMapping
    public Result<List<KnowledgeInfo>> list() {
        return Result.ok(knowledgeService.listKnowledge());
    }

    @Operation(summary = "Get knowledge base details")
    @GetMapping("/{id}")
    public Result<KnowledgeInfo> get(@PathVariable String id) {
        return Result.ok(knowledgeService.getKnowledge(id));
    }

    @Operation(summary = "Delete a knowledge base")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeService.deleteKnowledge(id);
        return Result.ok();
    }

    @Operation(summary = "Upload a document to a knowledge base")
    @PostMapping("/{id}/documents")
    public Result<KnowledgeDoc> uploadDocument(@PathVariable String id,
                                               @RequestParam("file") MultipartFile file) {
        return Result.ok(knowledgeService.uploadDocument(id, file));
    }

    @Operation(summary = "List documents in a knowledge base")
    @GetMapping("/{id}/documents")
    public Result<List<KnowledgeDoc>> listDocuments(@PathVariable String id) {
        return Result.ok(knowledgeService.listDocuments(id));
    }
}

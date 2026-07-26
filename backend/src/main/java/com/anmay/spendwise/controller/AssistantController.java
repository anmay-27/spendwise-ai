package com.anmay.spendwise.controller;

import com.anmay.spendwise.dto.Requests.AssistantRequest;
import com.anmay.spendwise.dto.Responses.AssistantResponse;
import com.anmay.spendwise.security.CurrentUserService;
import com.anmay.spendwise.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService assistantService;
    private final CurrentUserService currentUserService;

    public AssistantController(AssistantService assistantService,
                               CurrentUserService currentUserService) {
        this.assistantService = assistantService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/ask")
    public AssistantResponse ask(@Valid @RequestBody AssistantRequest request) {
        return assistantService.answer(currentUserService.currentUserId(), request.question());
    }
}

package com.aalap.aalapbackend.controller;


import com.aalap.aalapbackend.dto.NoolRequest;
import com.aalap.aalapbackend.dto.NoolResponse;
import com.aalap.aalapbackend.dto.ThreadResponse;
import com.aalap.aalapbackend.dto.ThreadSummary;
import com.aalap.aalapbackend.service.NoolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.io.IOException;

import java.util.List;

@RestController
@RequestMapping("/api/threads")
public class NoolController {
    NoolService noolService;

    @Autowired
    public NoolController(NoolService noolService) {
        this.noolService = noolService;
    }
    @PostMapping
    public NoolResponse createNool(@Valid @RequestBody NoolRequest noolRequest) {
        return noolService.createNool(noolRequest);
    }

    @GetMapping("/{threadId}")
    public ThreadResponse getNool(@PathVariable long threadId) {
        return noolService.getNool(threadId);
    }

    @GetMapping
    public List<ThreadSummary> getAllNools() {
        return noolService.getAllNools();
    }

    // --- NEW: UPLOAD MASTER MIX ENDPOINT ---
    @PostMapping(value = "/{noolId}/master", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoolResponse> uploadMasterAudio(
            @PathVariable Long noolId,
            @RequestParam("file") MultipartFile file) {
        try {
            NoolResponse response = noolService.uploadMasterAudio(noolId, file);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

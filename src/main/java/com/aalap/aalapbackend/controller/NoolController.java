package com.aalap.aalapbackend.controller;


import com.aalap.aalapbackend.dto.NoolRequest;
import com.aalap.aalapbackend.dto.NoolResponse;
import com.aalap.aalapbackend.dto.ThreadResponse;
import com.aalap.aalapbackend.dto.ThreadSummary;
import com.aalap.aalapbackend.service.NoolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.io.IOException;


@RestController
@RequestMapping("/api/threads")
public class NoolController {
    NoolService noolService;

    @Autowired
    public NoolController(NoolService noolService) {
        this.noolService = noolService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoolResponse createNool(@Valid @RequestBody NoolRequest noolRequest) {
        return noolService.createNool(noolRequest);
    }

    @GetMapping("/{threadId}")
    public ThreadResponse getNool(@PathVariable long threadId) {
        return noolService.getNool(threadId);
    }

    @GetMapping
    public Page<ThreadSummary> getAllNools(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return noolService.getAllNools(page, size);
    }

    @PostMapping(value = "/{noolId}/master", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoolResponse> uploadMasterAudio(
            @PathVariable Long noolId,
            @RequestParam("file") MultipartFile file) {
        try {
            NoolResponse response = noolService.uploadMasterAudio(noolId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── DELETE THREAD ────────────────────────────────────────────────────────
    // Only the creator of the thread can call this.
    // Deletes all contributions + their Cloudinary files + the master mix,
    // then removes the thread from the DB.

    @DeleteMapping("/{threadId}")
    public ResponseEntity<Void> deleteNool(@PathVariable Long threadId) throws IOException {
        noolService.deleteNool(threadId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
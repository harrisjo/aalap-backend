package com.aalap.aalapbackend.controller;


import com.aalap.aalapbackend.dto.NoolRequest;
import com.aalap.aalapbackend.dto.NoolResponse;
import com.aalap.aalapbackend.dto.ThreadResponse;
import com.aalap.aalapbackend.dto.ThreadSummary;
import com.aalap.aalapbackend.service.NoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public NoolResponse createNool(@RequestBody NoolRequest noolRequest) {
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
}

package com.videotogether.controller;


import com.videotogether.commen.Result;
import com.videotogether.service.impl.VideoServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/video")
public class VideoController {

//    @Autowired
//    private VideoServiceImpl videoService;
//    @GetMapping("/searchVideo")
//    public Result<List<String>> searchVideo(String keyword){
//
//        return Result.success();
//    }
}

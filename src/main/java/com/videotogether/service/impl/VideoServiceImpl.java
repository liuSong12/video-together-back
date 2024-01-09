package com.videotogether.service.impl;

import com.videotogether.pojo.Video;
import com.videotogether.mapper.VideoMapper;
import com.videotogether.service.IVideoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 裹个小脑
 * @since 2024-01-09
 */
@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements IVideoService {

}

package com.videotogether.mapper;

import com.videotogether.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author 裹个小脑
 * @since 2024-01-04
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    User getUser(Integer id);
}

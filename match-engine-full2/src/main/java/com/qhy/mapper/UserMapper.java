package com.qhy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qhy.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

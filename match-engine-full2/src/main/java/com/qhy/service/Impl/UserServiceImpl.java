package com.qhy.service.Impl;

import com.qhy.mapper.UserMapper;
import com.qhy.pojo.User;
import com.qhy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;

    @Override
    public List<User> getUsers() {
        log.info("获取当前用户表");
        return userMapper.selectList(null);
    }
}

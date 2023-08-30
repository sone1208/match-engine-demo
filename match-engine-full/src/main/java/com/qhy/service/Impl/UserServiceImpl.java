package com.qhy.service.Impl;

import com.qhy.dao.UserDao;
import com.qhy.pojo.User;
import com.qhy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Override
    public List<User> getUsers() {
        return userDao.selectList(null);
    }
}

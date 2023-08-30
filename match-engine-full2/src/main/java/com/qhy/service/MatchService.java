package com.qhy.service;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MatchService {
    public void matchExecutor(String code);
}

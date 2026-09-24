package com.team3.user.dto;

import com.team3.user.User;

public record UserResponse(Long id, String nickname, String profileImageUrl) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.nickname(), user.profileImageUrl());
    }
}

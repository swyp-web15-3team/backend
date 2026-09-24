package com.team3.user;

import com.team3.user.dto.UserResponse;
import com.team3.user.exception.ActiveUserRequiredException;
import com.team3.user.exception.DeletedUserException;
import com.team3.user.exception.UserNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        checkActive(user);
        return UserResponse.from(user);
    }

    public UserResponse updateNickname(Long userId, String nickname) {
        User user = users.findLockedById(userId)
            .orElseThrow(UserNotFoundException::new);
        checkActive(user);
        user.updateNickname(nickname);
        return UserResponse.from(user);
    }

    private void checkActive(User user) {
        if (user.isDeleted()) {
            throw new DeletedUserException();
        }
        if (!user.isActive()) {
            throw new ActiveUserRequiredException();
        }
    }
}

package com.team3.user;

import com.team3.user.enums.Provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.team3.common.exception.CustomException;
import com.team3.user.enums.UserErrorCode;
import com.team3.user.exception.ActiveUserRequiredException;
import com.team3.user.exception.DeletedUserException;
import com.team3.user.exception.UserNotFoundException;

import org.junit.jupiter.api.Test;

class UserServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final UserService service = new UserService(users);

    @Test
    void rejectsMissingPendingAndDeletedUsersWithDomainErrors() {
        when(users.findLockedById(1L)).thenReturn(Optional.empty());
        assertError(UserNotFoundException.class, UserErrorCode.USER_NOT_FOUND);
        User user = new User(Provider.KAKAO, "123");
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        assertError(ActiveUserRequiredException.class, UserErrorCode.ACTIVE_USER_REQUIRED);
        assertThat(user.nickname()).isNull();
        user.activate();
        service.updateNickname(1L, "tester");
        assertThat(user.nickname()).isEqualTo("tester");
        user.delete(Instant.now());
        assertError(DeletedUserException.class, UserErrorCode.DELETED_USER);
        assertThat(user.nickname()).isNull();
    }

    private void assertError(Class<? extends CustomException> type, UserErrorCode code) {
        assertThatThrownBy(() -> service.updateNickname(1L, "tester"))
            .isInstanceOfSatisfying(type, ex -> assertThat(ex.getErrorCode()).isEqualTo(code));
    }
}

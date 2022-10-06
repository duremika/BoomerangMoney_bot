package ru.duremika.boomerangbot.service;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ru.duremika.boomerangbot.entities.Status;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.repository.UserRepository;

import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
class UserServiceTest {
    @MockBean
    UserRepository mockRepository;
    @Autowired
    UserService userService;


    @Test
    void enableNewUser() {
        Long id = -1L;
        Optional<User> emptyOptionalUser = Optional.empty();
        Mockito.when(mockRepository.findById(id)).thenReturn(emptyOptionalUser);

        UserService.EnabledStatus enabledStatus = userService.enableUser(id);
        Assert.assertEquals(UserService.EnabledStatus.NEW_USER, enabledStatus );
        Mockito.verify(mockRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void enableBannedUser() {
        Long id = 111L;
        Optional<User> existsBannedOptionalUser = Optional.of(new User(id));
        existsBannedOptionalUser.get().setStatus(Status.BANNED);
        Mockito.when(mockRepository.findById(id)).thenReturn(existsBannedOptionalUser);

        UserService.EnabledStatus enabledStatus = userService.enableUser(id);
        Assert.assertEquals(UserService.EnabledStatus.BANNED_USER, enabledStatus);
        Mockito.verify(mockRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void enableDisabledUser() {
        Long id = 112L;
        Optional<User> existsDisabledOptionalUser = Optional.of(new User(id));
        existsDisabledOptionalUser.get().setEnabled(false);
        Mockito.when(mockRepository.findById(id)).thenReturn(existsDisabledOptionalUser);

        UserService.EnabledStatus enabledStatus = userService.enableUser(id);
        Assert.assertEquals(UserService.EnabledStatus.DISABLED_USER, enabledStatus);
        Assert.assertTrue(existsDisabledOptionalUser.get().isEnabled());
        Mockito.verify(mockRepository, Mockito.times(1)).save(existsDisabledOptionalUser.get());
    }

    @Test
    void enableEnabledUser() {
        Long id = 111L;
        Optional<User> existsEnabledOptionalUser = Optional.of(new User(id));
        existsEnabledOptionalUser.get().setEnabled(true);
        Mockito.when(mockRepository.findById(id)).thenReturn(existsEnabledOptionalUser);

        UserService.EnabledStatus enabledStatus = userService.enableUser(id);
        Assert.assertEquals(UserService.EnabledStatus.ENABLED_USER, enabledStatus);
        Mockito.verify(mockRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void disableUser() {
        Long id = 113L;
        Optional<User> optionalUser = Optional.of(new User(id));
        Mockito.when(mockRepository.findById(id)).thenReturn(optionalUser);
        userService.disableUser(id);
        Assert.assertFalse(optionalUser.get().isEnabled());
        Mockito.verify(mockRepository, Mockito.times(1)).save(optionalUser.get());

        userService.disableUser(-1L);
        Mockito.verify(mockRepository, Mockito.times(1)).save(optionalUser.get());
    }
}
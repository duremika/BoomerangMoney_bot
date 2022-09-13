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
import ru.duremika.boomerangbot.exception.UserBannedException;
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
    void createUser() throws UserBannedException {
        Long notExistsId = -1L;
        Optional<User> emptyOptionalUser = Optional.empty();
        Mockito.when(mockRepository.findById(notExistsId)).thenReturn(emptyOptionalUser);

        boolean isNew = userService.createOrUpdateUser(notExistsId);
        Assert.assertTrue(isNew);
        Mockito.verify(mockRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void updateBannedUser() {
        Long existsId = 111L;
        Optional<User> existsBannedOptionalUser = Optional.of(userService.createNewUser(existsId));
        existsBannedOptionalUser.get().setStatus(Status.BANNED);
        Mockito.when(mockRepository.findById(existsId)).thenReturn(existsBannedOptionalUser);
        try {
            userService.createOrUpdateUser(existsId);
            Assert.fail();
        } catch (UserBannedException e){
            Assert.assertTrue(true);
        }
    }

    @Test
    void updateDisabledUser() throws UserBannedException {
        Long existsId = 112L;
        Optional<User> existsDisabledOptionalUser = Optional.of(userService.createNewUser(existsId));
        existsDisabledOptionalUser.get().setEnabled(false);
        Mockito.when(mockRepository.findById(existsId)).thenReturn(existsDisabledOptionalUser);

        boolean isNew = userService.createOrUpdateUser(existsId);
        Assert.assertFalse(isNew);
        Assert.assertTrue(existsDisabledOptionalUser.get().isEnabled());
        Mockito.verify(mockRepository, Mockito.times(1)).save(existsDisabledOptionalUser.get());
    }

    @Test
    void disableUser() {
        Long existsId = 113L;
        Optional<User> optionalUser = Optional.of(userService.createNewUser(existsId));
        Mockito.when(mockRepository.findById(existsId)).thenReturn(optionalUser);
        userService.disableUser(existsId);
        Assert.assertFalse(optionalUser.get().isEnabled());
        Mockito.verify(mockRepository, Mockito.times(1)).save(optionalUser.get());

        userService.disableUser(-1L);
        Mockito.verify(mockRepository, Mockito.times(1)).save(optionalUser.get());
    }
}
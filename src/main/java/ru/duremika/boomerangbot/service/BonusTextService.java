package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.BonusText;
import ru.duremika.boomerangbot.repository.BonusTextRepository;

@Service
@RequiredArgsConstructor
public class BonusTextService {
    private final BonusTextRepository repository;

    public void save(BonusText bonusText){
        repository.save(bonusText);
    }

    public BonusText get(String id){
        return repository.findById(id).orElse(null);
    }
}

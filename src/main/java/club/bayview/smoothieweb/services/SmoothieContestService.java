package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.repositories.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SmoothieContestService {

    @Autowired
    ContestRepository contestRepository;

    public Mono<Contest> findContestByName(String name) {
        return contestRepository.findByName(name);
    }

    public Mono<Contest> findContestById(String id) {
        return contestRepository.findById(id);
    }

    public Mono<Contest> saveContest(Contest contest) {
        return contestRepository.save(contest);
    }

    public Mono<Void> deleteContest(Contest contest) {
        return contestRepository.delete(contest);
    }

}

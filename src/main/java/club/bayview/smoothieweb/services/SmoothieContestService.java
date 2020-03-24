package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.repositories.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SmoothieContestService {

    @Autowired
    ContestRepository contestRepository;

    // TODO queue contest edits per contest in a webflux way

    public Mono<Contest> findContestByName(String name) {
        return contestRepository.findByName(name);
    }

    public Mono<Contest> findContestById(String id) {
        return contestRepository.findById(id);
    }

    public Mono<Contest> saveContest(Contest contest) {
        return contestRepository.save(contest);
    }

    public Mono<Void> deleteContestById(String id) {
        return contestRepository.deleteById(id);
    }

    public Flux<Contest> findAllContests(Pageable p) {
        return contestRepository.findAllBy(p);
    }

    public Mono<Long> countAllContests() {
        return contestRepository.countAllBy();
    }
}

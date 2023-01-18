package com.example.application.data.service;

import com.example.application.data.entity.SamplePerson;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

@Service
public class SamplePersonService {

    private final SamplePersonRepository repository;

    public SamplePersonService(SamplePersonRepository repository) {
        this.repository = repository;
    }

    public Optional<SamplePerson> get(Long id) {
        return repository.findById(id);
    }

    public SamplePerson update(SamplePerson entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
    
    public Stream<SamplePerson> stream(Pageable pageable) {
        return repository.findAllBy(pageable).stream();
    }

    public int count() {
        return (int) repository.count();
    }

}

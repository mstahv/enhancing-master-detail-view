package com.example.application.data.service;

import com.example.application.data.entity.SamplePerson;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SamplePersonRepository
        extends PagingAndSortingRepository<SamplePerson, Long> {

    List<SamplePerson> findAllBy(Pageable pageable);
}

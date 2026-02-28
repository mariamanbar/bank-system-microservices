package com.mariam.loggerservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.mariam.loggerservice.model.Log;

@Repository
public interface LogRepository extends MongoRepository<Log, String>{

}

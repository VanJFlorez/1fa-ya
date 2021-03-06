package com.jcflorezv.unfaya.houseServices.repositories;

import java.util.List;

import com.jcflorezv.unfaya.houseServices.models.Tag;

import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin("*")
public interface TagRepository extends Repository<Tag, Long> {
  Tag save(Tag t);
  Tag findById(Long id);
  Tag findByName(String name);
  List<Tag> findAll();
}
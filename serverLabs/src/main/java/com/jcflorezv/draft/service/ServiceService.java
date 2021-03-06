package com.jcflorezv.draft.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import com.google.common.collect.ImmutableList;
import com.jcflorezv.draft.entity.House;
import com.jcflorezv.draft.entity.Service;
import com.jcflorezv.draft.entity.Tag;
import com.jcflorezv.draft.repository.HouseRepository;
import com.jcflorezv.draft.repository.ServiceRepository;
import com.jcflorezv.draft.repository.TagRepository;

import org.springframework.beans.factory.annotation.Autowired;

 @Transactional
@org.springframework.stereotype.Service
public class ServiceService {

  @Autowired
  TagRepository tagRepository;

  @Autowired
  HouseRepository houseRepository;

  @Autowired
  ServiceRepository serviceRepository;

  @PersistenceContext
  EntityManager entityManager;

  
  public Service create(Service service) {
    // TODO
    // Enable this, taking the house from auth context
    Long houseId = 3L; // getCurrentContext().getAuthentication().getPersonal().getHouse().getHouseId();
    // Optional<House> result = houseRepository.findById(houseId);
    // House house = result.get();
    House house = houseRepository.findAll().iterator().next();
    house.addService(service);

    Service taggedService = applyTags(service);
    houseRepository.save(house);
    return serviceRepository.save(taggedService);
  }

  public Service replace(Long id, Service newService) {
    Service currService = serviceRepository.findById(id).get();
    Service updatedService = updateService(currService, newService);
    return serviceRepository.save(updatedService);
  }

  private Service applyTags(Service service) {
    Set<Tag> unassociatedTags = service.getTags();
    List<Tag> catalog = ImmutableList.copyOf(tagRepository.findAll());
    Set<Tag> bindedTags = new HashSet<>();
    
    unassociatedTags.forEach(untag -> {
      Tag tag = catalog.stream()
      .filter(ctag -> ctag.getId() == untag.getId())
      .findAny()
      .orElse(catalog.get(0));
      
      tag = entityManager.merge(tag);
      bindedTags.add(tag);
    });
    
    service.getTags().clear();
    bindedTags.forEach(service::addTag);

    return service;
  }

  /**
   * TODO:
   * Do this with reflection api
   * 
   * This method preserves relationships House.
   */
  private Service updateService(Service currService, Service newService) {
    currService.setName(newService.getName());
    currService.setPhonenumbers(newService.getPhonenumbers());
    currService.setTags(applyTags(newService).getTags());
    return currService;
  }

  public void delete(Long serviceId) {
    // update relationships
    House house = houseRepository.findAll().iterator().next();
    Service service = serviceRepository.findById(serviceId).get(); // proxy entity
    house.removeService(service);
    //persist
    houseRepository.save(house);
    serviceRepository.delete(service);
  }

  public void addViewCount(Long serviceId) {
    Service service = serviceRepository.findById(serviceId).get();
    Long currViewCount = service.getViewCount();
    service.setViewCount(++currViewCount);
    serviceRepository.save(service);
  }
}
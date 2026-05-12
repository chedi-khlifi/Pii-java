package com.example.service;

import com.example.entity.User;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class UserService extends GenericServiceImpl<User, Integer> {
    
    public UserService() {
        super(User.class);
    }
    
    public Optional<User> findByUsername(String username) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }
    
    public Optional<User> findByEmail(String email) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }
    
    public List<User> findByFirstName(String firstName) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.firstName = :firstName", User.class)
                    .setParameter("firstName", firstName)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<User> findByLastName(String lastName) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.lastName = :lastName", User.class)
                    .setParameter("lastName", lastName)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}

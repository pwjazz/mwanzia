package org.mwanzia.test;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPA {
    private static final Map<String, JPA> INSTANCES = new HashMap<String, JPA>();

    private EntityManagerFactory emf;
    private ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<EntityManager>() {
        protected EntityManager initialValue() {
            return emf.createEntityManager();
        }
    };

    private JPA(String persistenceUnitName) {
        this.emf = Persistence.createEntityManagerFactory(persistenceUnitName);
    }

    public static JPA initialize(String persistenceUnitName) {
        JPA jpa = new JPA(persistenceUnitName);
        INSTANCES.put(persistenceUnitName, jpa);
        return jpa;
    }

    public static JPA getInstance(String persistenceUnitName) {
        return INSTANCES.get(persistenceUnitName);
    }

    public static JPA getInstance() {
        return INSTANCES.values().iterator().next();
    }

    public EntityManagerFactory getEmf() {
        return emf;
    }

    public EntityManager getEntityManager() {
        return currentEntityManager.get();
    }

    public void clear() {
        currentEntityManager.remove();
    }
}

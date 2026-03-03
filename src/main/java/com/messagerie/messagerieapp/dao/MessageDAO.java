package com.messagerie.messagerieapp.dao;

import com.messagerie.messagerieapp.model.Message;
import com.messagerie.messagerieapp.model.MessageStatus;
import com.messagerie.messagerieapp.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class MessageDAO {

    public void save(Message message) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = em.getTransaction();
            tx.begin();
            em.persist(message);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // Historique complet entre 2 utilisateurs
    public List<Message> findConversation(String user1, String user2) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "FROM Message WHERE (sender = :u1 AND receiver = :u2) " +
                                    "OR (sender = :u2 AND receiver = :u1) ORDER BY dateEnvoi ASC",
                            Message.class)
                    .setParameter("u1", user1)
                    .setParameter("u2", user2)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Messages en attente hors-ligne
    public List<Message> findPendingMessages(String receiver) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "FROM Message WHERE receiver = :r AND statut = 'ENVOYE'",
                            Message.class)
                    .setParameter("r", receiver)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Marquer les messages comme RECU apres livraison
    public void markAsReceived(String sender, String receiver) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = em.getTransaction();
            tx.begin();
            em.createQuery(
                            "UPDATE Message SET statut = :s " +
                                    "WHERE sender = :sender AND receiver = :receiver AND statut = 'ENVOYE'")
                    .setParameter("s", MessageStatus.RECU)
                    .setParameter("sender", sender)
                    .setParameter("receiver", receiver)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
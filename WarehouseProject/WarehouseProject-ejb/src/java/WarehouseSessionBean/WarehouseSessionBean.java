/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package WarehouseSessionBean;

import WarehouseEntities.Inventory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Viesturs
 */
@Stateless
public class WarehouseSessionBean implements WarehouseSessionBeanLocal {
    @PersistenceContext(unitName = "WarehouseProject-ejbPU")
    private EntityManager em;

   
    @Override
    public List<Inventory> getByPriceRange(BigDecimal min, BigDecimal max){
        List<Inventory> inv = em.createNamedQuery("Inventory.findAll").getResultList();
        List<Inventory> result = new ArrayList<>();
        for (Inventory i : inv) {
            if (i.getPrice().compareTo(min) >= 0 && i.getPrice().compareTo(max) <= 0 ) result.add(i);
        }
        return result;
    }

    @Override
    public List<Inventory> getByStockReserve(){
        List<Inventory> inv = em.createNamedQuery("Inventory.findAll").getResultList();
        List<Inventory> result = new ArrayList<>();
        for (Inventory i : inv) {
            if (i.getReserved() - i.getInstock() < 0) result.add(i);
        }
        return result;
    }

    public void persist(Object object) {
        em.persist(object);
    }
}
